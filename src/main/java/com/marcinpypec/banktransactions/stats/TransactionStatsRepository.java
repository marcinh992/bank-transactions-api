package com.marcinpypec.banktransactions.stats;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface TransactionStatsRepository extends MongoRepository<TransactionStatsDocument, String> {

    List<TransactionStatsDocument> findByYearMonthAndGroupByOrderByTotalAmountDesc(String yearMonth, String groupBy);

    void deleteByYearMonth(String yearMonth);

    List<TransactionStatsDocument> findByGroupByAndYearMonthBetweenOrderByYearMonthAsc(String groupBy, String from, String to);

    List<TransactionStatsDocument> findByYearMonthAndGroupBy(String yearMonth, String groupBy, Sort sort);
}