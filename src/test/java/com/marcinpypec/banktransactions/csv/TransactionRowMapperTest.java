package com.marcinpypec.banktransactions.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRowMapperTest {

    private TransactionRowMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionRowMapper();
    }

    private CSVRecord parseRecord(String header, String row) throws Exception {
        String csv = header + "\n" + row;
        CSVParser parser = CSVParser.parse(new StringReader(csv),
                CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build());
        return parser.getRecords().get(0);
    }

    @Test
    void shouldMapValidRecord() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,Groceries,-186.47"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.iban()).isEqualTo("PL61109010140000071219812874");
        assertThat(draft.transactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(draft.currency()).isEqualTo("PLN");
        assertThat(draft.category()).isEqualTo("Groceries");
        assertThat(draft.amount()).isEqualByComparingTo(new BigDecimal("-186.47"));
    }

    @Test
    void shouldMapPositiveAmount() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-02,PLN,Salary,12500.00"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.amount()).isEqualByComparingTo(new BigDecimal("12500.00"));
    }

    @Test
    void shouldHandleLowercaseHeaders() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "iban,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,Food,-50.00"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.iban()).isEqualTo("PL61109010140000071219812874");
        assertThat(draft.category()).isEqualTo("Food");
    }

    @Test
    void shouldHandleUppercaseHeaders() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,DATE,CURRENCY,CATEGORY,AMOUNT",
                "PL61109010140000071219812874,2026-01-15,EUR,Rent,-1500.00"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.currency()).isEqualTo("EUR");
        assertThat(draft.category()).isEqualTo("Rent");
    }

    @Test
    void shouldThrowForMissingColumn() throws Exception {
        // given - missing 'category' column
        CSVRecord record = parseRecord(
                "IBAN,date,currency,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,-50.00"
        );

        // when/then
        assertThatThrownBy(() -> mapper.map(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing column");
    }

    @Test
    void shouldThrowForInvalidDateFormat() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,15-01-2026,PLN,Food,-50.00"
        );

        // when/then
        assertThatThrownBy(() -> mapper.map(record))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void shouldThrowForInvalidAmountFormat() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,Food,not-a-number"
        );

        // when/then
        assertThatThrownBy(() -> mapper.map(record))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void shouldHandleAmountWithManyDecimalPlaces() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,Food,-123.456789"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.amount()).isEqualByComparingTo(new BigDecimal("-123.456789"));
    }

    @Test
    void shouldHandleZeroAmount() throws Exception {
        // given
        CSVRecord record = parseRecord(
                "IBAN,date,currency,category,amount",
                "PL61109010140000071219812874,2026-01-15,PLN,Refund,0.00"
        );

        // when
        TransactionDraft draft = mapper.map(record);

        // then
        assertThat(draft.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
