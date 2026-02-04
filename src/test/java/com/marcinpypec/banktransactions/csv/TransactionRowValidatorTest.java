package com.marcinpypec.banktransactions.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRowValidatorTest {

    private TransactionRowValidator validator;
    private YearMonth expectedMonth;

    @BeforeEach
    void setUp() {
        validator = new TransactionRowValidator();
        expectedMonth = YearMonth.of(2026, 1);
    }

    private TransactionDraft validDraft() {
        return new TransactionDraft(
                "PL61109010140000071219812874",
                LocalDate.of(2026, 1, 15),
                "PLN",
                "Groceries",
                new BigDecimal("123.45")
        );
    }

    @Test
    void shouldPassForValidTransaction() {
        assertThatCode(() -> validator.validate(validDraft(), expectedMonth))
                .doesNotThrowAnyException();
    }

    @Nested
    class IbanValidation {

        @Test
        void shouldPassForValidPolishIban() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldPassForValidGermanIban() {
            var draft = new TransactionDraft(
                    "DE89370400440532013000",
                    LocalDate.of(2026, 1, 15),
                    "EUR",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void shouldRejectBlankIban(String iban) {
            var draft = new TransactionDraft(
                    iban,
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("IBAN");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "pl61109010140000071219812874",  // lowercase country code
                "PL123",                          // too short
                "12345678901234567890",           // no country code
                "PL6110901014000007121981287!",   // special character
                "PL 61109010140000071219812874"   // space
        })
        void shouldRejectInvalidIban(String iban) {
            var draft = new TransactionDraft(
                    iban,
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("IBAN");
        }
    }

    @Nested
    class DateValidation {

        @Test
        void shouldRejectNullDate() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    null,
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("date");
        }

        @Test
        void shouldRejectDateFromDifferentMonth() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 2, 15),  // February, but expected January
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("date");
        }

        @Test
        void shouldRejectDateFromDifferentYear() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2025, 1, 15),  // 2025, but expected 2026
                    "PLN",
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("date");
        }

        @Test
        void shouldAcceptAnyDayWithinExpectedMonth() {
            for (int day = 1; day <= 31; day++) {
                var draft = new TransactionDraft(
                        "PL61109010140000071219812874",
                        LocalDate.of(2026, 1, day),
                        "PLN",
                        "Food",
                        BigDecimal.TEN
                );
                assertThatCode(() -> validator.validate(draft, expectedMonth))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    class CurrencyValidation {

        @ParameterizedTest
        @ValueSource(strings = {"PLN", "EUR", "USD", "GBP", "CHF"})
        void shouldAcceptValidCurrencyCodes(String currency) {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    currency,
                    "Food",
                    BigDecimal.TEN
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"pln", "Pln", "PL", "PLNN", "123", "PL1"})
        void shouldRejectInvalidCurrency(String currency) {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    currency,
                    "Food",
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("currency");
        }
    }

    @Nested
    class CategoryValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void shouldRejectBlankCategory(String category) {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    category,
                    BigDecimal.TEN
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("category");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Food", "Groceries", "Salary", "Rent", "Entertainment"})
        void shouldAcceptValidCategories(String category) {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    category,
                    BigDecimal.TEN
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class AmountValidation {

        @Test
        void shouldRejectNullAmount() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Food",
                    null
            );
            assertThatThrownBy(() -> validator.validate(draft, expectedMonth))
                    .isInstanceOf(RowValidationException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        void shouldAcceptPositiveAmount() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Salary",
                    new BigDecimal("5000.00")
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptNegativeAmount() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Groceries",
                    new BigDecimal("-150.00")
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptZeroAmount() {
            var draft = new TransactionDraft(
                    "PL61109010140000071219812874",
                    LocalDate.of(2026, 1, 15),
                    "PLN",
                    "Refund",
                    BigDecimal.ZERO
            );
            assertThatCode(() -> validator.validate(draft, expectedMonth))
                    .doesNotThrowAnyException();
        }
    }
}
