package com.marcinpypec.banktransactions.csv;

import com.marcinpypec.banktransactions.importjob.exception.ImportFileReadException;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvTransactionReaderTest {

    private CsvTransactionReader reader;

    @BeforeEach
    void setUp() {
        reader = new CsvTransactionReader();
    }

    @Test
    void shouldParseValidCsv() throws Exception {
        // given
        String csv = """
                IBAN,date,currency,category,amount
                PL61109010140000071219812874,2026-01-15,PLN,Food,-50.00
                DE89370400440532013000,2026-01-16,EUR,Salary,3000.00
                """;

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records).hasSize(2);
            assertThat(records.get(0).get("IBAN")).isEqualTo("PL61109010140000071219812874");
            assertThat(records.get(0).get("amount")).isEqualTo("-50.00");
            assertThat(records.get(1).get("currency")).isEqualTo("EUR");
        }
    }

    @Test
    void shouldSkipHeaderRow() throws Exception {
        // given
        String csv = """
                IBAN,date,currency,category,amount
                PL12345678901234567890123456,2026-01-01,PLN,Test,100.00
                """;

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records).hasSize(1);
            assertThat(records.get(0).get("IBAN")).isEqualTo("PL12345678901234567890123456");
        }
    }

    @Test
    void shouldTrimWhitespace() throws Exception {
        // given
        String csv = """
                IBAN,date,currency,category,amount
                  PL12345678901234567890123456  ,  2026-01-01  ,  PLN  ,  Food  ,  100.00
                """;

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records.get(0).get("IBAN")).isEqualTo("PL12345678901234567890123456");
            assertThat(records.get(0).get("currency")).isEqualTo("PLN");
            assertThat(records.get(0).get("category")).isEqualTo("Food");
        }
    }

    @Test
    void shouldHandleEmptyFile() throws Exception {
        // given
        String csv = "";

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records).isEmpty();
        }
    }

    @Test
    void shouldHandleHeaderOnlyFile() throws Exception {
        // given
        String csv = "IBAN,date,currency,category,amount\n";

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records).isEmpty();
        }
    }

    @Test
    void shouldHandleUtf8Characters() throws Exception {
        // given
        String csv = """
                IBAN,date,currency,category,amount
                PL12345678901234567890123456,2026-01-01,PLN,Żywność,100.00
                """;

        // when
        try (CSVParser parser = reader.openParser(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CSVRecord> records = parser.getRecords();

            // then
            assertThat(records.get(0).get("category")).isEqualTo("Żywność");
        }
    }

    @Test
    void shouldThrowExceptionForNullInput() {
        assertThatThrownBy(() -> reader.openParser(null))
                .isInstanceOf(ImportFileReadException.class);
    }
}
