package com.marcinpypec.banktransactions.importjob.service;

import com.marcinpypec.banktransactions.csv.CsvTransactionReader;
import com.marcinpypec.banktransactions.csv.RowValidationException;
import com.marcinpypec.banktransactions.importjob.exception.ImportFileReadException;
import com.marcinpypec.banktransactions.importjob.exception.ImportNotFoundException;
import com.marcinpypec.banktransactions.importjob.model.ImportJobDocument;
import com.marcinpypec.banktransactions.importjob.model.ImportReport;
import com.marcinpypec.banktransactions.importjob.model.ImportStatus;
import com.marcinpypec.banktransactions.importjob.repository.ImportJobRepository;
import com.marcinpypec.banktransactions.stats.TransactionStatsMaterializer;
import com.marcinpypec.banktransactions.transactions.TransactionBatchWriter;
import com.marcinpypec.banktransactions.transactions.TransactionDocument;
import com.marcinpypec.banktransactions.transactions.TransactionDocumentFactory;
import com.marcinpypec.banktransactions.csv.TransactionDraft;
import com.marcinpypec.banktransactions.csv.TransactionRowMapper;
import com.marcinpypec.banktransactions.csv.TransactionRowValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportProcessor {

    private static final int BATCH_SIZE = 1000;

    private final CsvTransactionReader csvReader;
    private final TransactionRowMapper rowMapper;
    private final TransactionBatchWriter batchWriter;
    private final TransactionRowValidator rowValidator;
    private final ImportJobRepository importJobRepository;
    private final TransactionDocumentFactory documentFactory;
    private final TransactionStatsMaterializer statsMaterializer;

    @Async
    public void process(String jobId, byte[] fileBytes) {
        ImportJobDocument job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportNotFoundException(jobId));

        start(job);

        try {
            YearMonth expectedMonth = YearMonth.parse(job.getYearMonth());
            ImportReport report = importTransactions(jobId, expectedMonth, fileBytes);
            complete(job, report);
        } catch (Exception e) {
            fail(job, e);
        }
    }

    private ImportReport importTransactions(String jobId, YearMonth expectedMonth, byte[] fileBytes) {
        ImportReport report = ImportReport.empty();
        List<TransactionDocument> batch = new ArrayList<>(BATCH_SIZE);

        try (CSVParser parser = csvReader.openParser(fileBytes)) {
            for (CSVRecord record : parser) {
                report = report.incTotal();

                try {
                    TransactionDraft draft = rowMapper.map(record);
                    rowValidator.validate(draft, expectedMonth);

                    TransactionDocument doc = documentFactory.create(draft, jobId, expectedMonth);
                    batch.add(doc);
                    report = report.incImported();

                    if (batch.size() >= BATCH_SIZE) {
                        batchWriter.saveBatch(batch);
                        batch.clear();
                    }
                } catch (RowValidationException | IllegalArgumentException ex) {
                    report = report.incInvalid();
                }
            }

            batchWriter.saveBatch(batch);
            return report;

        } catch (java.io.IOException e) {
            throw new ImportFileReadException("Cannot close CSV parser", e);
        }
    }

    private void start(ImportJobDocument job) {
        job.setStatus(ImportStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        importJobRepository.save(job);
    }

    private void complete(ImportJobDocument job, ImportReport report) {
        job.setTotalRows(report.totalRows());
        job.setImportedRows(report.importedRows());
        job.setInvalidRows(report.invalidRows());

        statsMaterializer.materializeForMonth(job.getYearMonth());

        job.setStatus(ImportStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        importJobRepository.save(job);
    }

    private void fail(ImportJobDocument job, Exception e) {
        log.error("Import job failed: jobId={}, yearMonth={}", job.getId(), job.getYearMonth(), e);
        job.setStatus(ImportStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(toSafeMessage(e));
        importJobRepository.save(job);
    }

    private String toSafeMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        int max = 300;
        return msg.length() <= max ? msg : msg.substring(0, max);
    }
}