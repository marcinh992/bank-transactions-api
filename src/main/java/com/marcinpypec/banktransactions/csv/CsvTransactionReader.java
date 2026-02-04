package com.marcinpypec.banktransactions.csv;

import com.marcinpypec.banktransactions.importjob.exception.ImportFileReadException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class CsvTransactionReader {

    public CSVParser openParser(byte[] fileBytes) {
        try {
            return CSVParser.parse(
                    new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8),
                    CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .setTrim(true)
                            .build()
            );
        } catch (Exception e) {
            throw new ImportFileReadException("Cannot read CSV file", e);
        }
    }
}

