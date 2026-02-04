package com.marcinpypec.banktransactions.stats;

import java.math.BigDecimal;

public record MonthlyStatsRow(
        String yearMonth,
        String currency,
        long count,
        BigDecimal totalAmount
) {}
