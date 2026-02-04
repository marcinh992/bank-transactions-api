package com.marcinpypec.banktransactions.csv;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDraft(
        String iban,
        LocalDate transactionDate,
        String currency,
        String category,
        BigDecimal amount
) {}
