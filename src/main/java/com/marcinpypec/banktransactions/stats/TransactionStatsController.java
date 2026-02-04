package com.marcinpypec.banktransactions.stats;

import com.marcinpypec.banktransactions.api.StatsSort;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class TransactionStatsController {

    private final TransactionStatsService statsService;

    @GetMapping
    public List<TransactionStatsRow> getStats(
            @RequestParam("yearMonth")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must be yyyy-MM")
            String yearMonth,
            @RequestParam("groupBy") StatsGroupBy groupBy,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "sort", required = false, defaultValue = "TOTAL_DESC") StatsSort sort
    ) {
        validateLimit(limit);
        return statsService.getStats(yearMonth, groupBy, limit, sort);
    }

    @GetMapping("/monthly")
    public List<MonthlyStatsRow> getMonthlyStats(
            @RequestParam("from")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "from must be yyyy-MM")
            String from,
            @RequestParam("to")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "to must be yyyy-MM")
            String to
    ) {
        validateRange(from, to);
        return statsService.getMonthlyStats(from, to);
    }

    private static void validateLimit(int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
    }

    private static void validateRange(String from, String to) {
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException("from must be <= to");
        }
    }
}
