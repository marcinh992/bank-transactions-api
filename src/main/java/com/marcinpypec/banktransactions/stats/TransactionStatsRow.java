package com.marcinpypec.banktransactions.stats;

import java.math.BigDecimal;

public record TransactionStatsRow(
        String key,
        String currency,
        long count,
        BigDecimal totalAmount
) {}
