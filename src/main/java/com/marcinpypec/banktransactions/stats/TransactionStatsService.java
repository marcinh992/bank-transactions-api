package com.marcinpypec.banktransactions.stats;

import com.marcinpypec.banktransactions.api.StatsSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionStatsService {

    private final TransactionStatsRepository statsRepository;

    public List<TransactionStatsRow> getStats(String yearMonth, StatsGroupBy groupBy, int limit, StatsSort sort) {
        var results = statsRepository.findByYearMonthAndGroupBy(yearMonth, groupBy.name(), toSort(sort));
        return results.stream()
                .limit(limit)
                .map(d -> new TransactionStatsRow(d.getKey(), d.getCurrency(), d.getCount(), d.getTotalAmount()))
                .toList();
    }

    private Sort toSort(StatsSort sort) {
        return switch (sort) {
            case TOTAL_DESC -> Sort.by(Sort.Direction.DESC, "totalAmount");
            case TOTAL_ASC -> Sort.by(Sort.Direction.ASC, "totalAmount");
        };
    }

    public List<MonthlyStatsRow> getMonthlyStats(String from, String to) {
        return statsRepository
                .findByGroupByAndYearMonthBetweenOrderByYearMonthAsc(StatsGroupBy.MONTH.name(), from, to)
                .stream()
                .map(d -> new MonthlyStatsRow(d.getYearMonth(), d.getCurrency(), d.getCount(), d.getTotalAmount()))
                .toList();
    }
}
