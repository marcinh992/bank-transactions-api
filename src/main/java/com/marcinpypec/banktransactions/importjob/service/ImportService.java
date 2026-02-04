package com.marcinpypec.banktransactions.importjob.service;

import com.marcinpypec.banktransactions.importjob.exception.ImportAlreadyExistsException;
import com.marcinpypec.banktransactions.importjob.exception.ImportFileReadException;
import com.marcinpypec.banktransactions.importjob.mapper.ImportJobMapper;
import com.marcinpypec.banktransactions.importjob.dto.ImportJobResponse;
import com.marcinpypec.banktransactions.importjob.exception.ImportNotFoundException;
import com.marcinpypec.banktransactions.importjob.model.ImportJobDocument;
import com.marcinpypec.banktransactions.importjob.model.ImportStatus;
import com.marcinpypec.banktransactions.importjob.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobMapper importJobMapper;
    private final ImportProcessor importProcessor;
    private final ImportJobRepository importJobRepository;

    public ImportJobResponse createImport(String yearMonth, MultipartFile file) {
        validateImportDoesNotExist(yearMonth);

        ImportJobDocument saved = importJobRepository.save(createImportJob(yearMonth, file));

        byte[] bytes = readBytes(file);

        importProcessor.process(saved.getId(), bytes);

        return importJobMapper.toResponse(saved);
    }

    public ImportJobResponse getImport(String jobId) {
        ImportJobDocument job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportNotFoundException(jobId));
        return importJobMapper.toResponse(job);
    }

    private void validateImportDoesNotExist(String yearMonth) {
        if (importJobRepository.existsByYearMonth(yearMonth)) {
            throw new ImportAlreadyExistsException(yearMonth);
        }
    }

    private ImportJobDocument createImportJob(String yearMonth, MultipartFile file) {
        return ImportJobDocument.builder()
                .yearMonth(yearMonth)
                .fileName(file.getOriginalFilename())
                .status(ImportStatus.RECEIVED)
                .createdAt(Instant.now())
                .totalRows(0)
                .importedRows(0)
                .invalidRows(0)
                .build();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImportFileReadException("Cannot read uploaded file", e);
        }
    }
}
