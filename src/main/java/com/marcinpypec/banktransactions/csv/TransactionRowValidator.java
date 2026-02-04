package com.marcinpypec.banktransactions.csv;

import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class TransactionRowValidator {

    public void validate(TransactionDraft tx, YearMonth expectedMonth) {
        requireNotBlank(tx.iban(), "IBAN blank");
        if (!tx.iban().matches("^[A-Z]{2}[0-9A-Z]{13,32}$")) {
            throw new RowValidationException("IBAN invalid");
        }

        if (tx.transactionDate() == null) {
            throw new RowValidationException("date missing");
        }
        if (!YearMonth.from(tx.transactionDate()).equals(expectedMonth)) {
            throw new RowValidationException("date not in yearMonth");
        }

        requireMatches(tx.currency(), "^[A-Z]{3}$", "currency invalid");
        requireNotBlank(tx.category(), "category blank");

        if (tx.amount() == null) {
            throw new RowValidationException("amount missing");
        }
    }

    private void requireNotBlank(String v, String msg) {
        if (v == null || v.isBlank()) throw new RowValidationException(msg);
    }

    private void requireMatches(String v, String regex, String msg) {
        if (v == null || !v.matches(regex)) throw new RowValidationException(msg);
    }
}
