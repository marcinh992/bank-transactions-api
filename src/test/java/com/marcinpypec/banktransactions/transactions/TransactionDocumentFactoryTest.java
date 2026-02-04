package com.marcinpypec.banktransactions.transactions;

import com.marcinpypec.banktransactions.csv.TransactionDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionDocumentFactoryTest {

    private TransactionDocumentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TransactionDocumentFactory();
    }

    @Test
    void shouldCreateDocumentFromDraft() {
        // given
        TransactionDraft draft = new TransactionDraft(
                "PL61109010140000071219812874",
                LocalDate.of(2026, 1, 15),
                "PLN",
                "Groceries",
                new BigDecimal("-186.47")
        );
        String jobId = "job-123";
        YearMonth yearMonth = YearMonth.of(2026, 1);

        // when
        TransactionDocument doc = factory.create(draft, jobId, yearMonth);

        // then
        assertThat(doc.getImportJobId()).isEqualTo("job-123");
        assertThat(doc.getIban()).isEqualTo("PL61109010140000071219812874");
        assertThat(doc.getTransactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(doc.getCurrency()).isEqualTo("PLN");
        assertThat(doc.getCategory()).isEqualTo("Groceries");
        assertThat(doc.getAmount()).isEqualByComparingTo(new BigDecimal("-186.47"));
        assertThat(doc.getYearMonth()).isEqualTo("2026-01");
    }

    @Test
    void shouldFormatYearMonthCorrectly() {
        // given
        TransactionDraft draft = new TransactionDraft(
                "DE89370400440532013000",
                LocalDate.of(2026, 12, 25),
                "EUR",
                "Gifts",
                new BigDecimal("-500.00")
        );

        // when
        TransactionDocument doc = factory.create(draft, "job-456", YearMonth.of(2026, 12));

        // then
        assertThat(doc.getYearMonth()).isEqualTo("2026-12");
    }

    @Test
    void shouldHandlePositiveAmount() {
        // given
        TransactionDraft draft = new TransactionDraft(
                "PL61109010140000071219812874",
                LocalDate.of(2026, 1, 2),
                "PLN",
                "Salary",
                new BigDecimal("12500.00")
        );

        // when
        TransactionDocument doc = factory.create(draft, "job-789", YearMonth.of(2026, 1));

        // then
        assertThat(doc.getAmount()).isPositive();
        assertThat(doc.getCategory()).isEqualTo("Salary");
    }

    @Test
    void shouldNotSetId() {
        // given
        TransactionDraft draft = new TransactionDraft(
                "PL61109010140000071219812874",
                LocalDate.of(2026, 1, 15),
                "PLN",
                "Food",
                BigDecimal.TEN
        );

        // when
        TransactionDocument doc = factory.create(draft, "job-123", YearMonth.of(2026, 1));

        // then - ID should be null (MongoDB will generate it)
        assertThat(doc.getId()).isNull();
    }
}
