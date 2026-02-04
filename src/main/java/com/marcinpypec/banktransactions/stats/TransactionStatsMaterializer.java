package com.marcinpypec.banktransactions.stats;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Fields.fields;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@RequiredArgsConstructor
public class TransactionStatsMaterializer {

    private final MongoTemplate mongoTemplate;
    private final TransactionStatsRepository statsRepository;

    public void materializeForMonth(String yearMonth) {
        statsRepository.deleteByYearMonth(yearMonth);

        saveGroup(yearMonth, StatsGroupBy.CATEGORY, "category");
        saveGroup(yearMonth, StatsGroupBy.IBAN, "iban");

        saveMonthlyTotals(yearMonth);
    }

    private void saveGroup(String yearMonth, StatsGroupBy groupBy, String groupField) {
        MatchOperation match = match(where("yearMonth").is(yearMonth));

        GroupOperation group = group(fields().and(groupField, "$" + groupField).and("currency", "$currency"))
                .count().as("count")
                .sum("amount").as("totalAmount");

        ProjectionOperation project = project()
                .andExclude("_id")
                .and(context -> new Document("$literal", yearMonth)).as("yearMonth")
                .and(context -> new Document("$literal", groupBy.name())).as("groupBy")
                .and("_id." + groupField).as("key")
                .and("_id.currency").as("currency")
                .and("count").as("count")
                .and("totalAmount").as("totalAmount");

        SortOperation sort = sort(Sort.by(Sort.Direction.DESC, "totalAmount"));

        Aggregation agg = newAggregation(match, group, project, sort);

        List<TransactionStatsDocument> docs = mongoTemplate
                .aggregate(agg, "transactions", TransactionStatsDocument.class)
                .getMappedResults();

        if (!docs.isEmpty()) {
            statsRepository.saveAll(docs);
        }
    }

    private void saveMonthlyTotals(String yearMonth) {
        MatchOperation match = match(where("yearMonth").is(yearMonth));

        GroupOperation group = group(fields().and("currency", "$currency"))
                .count().as("count")
                .sum("amount").as("totalAmount");

        ProjectionOperation project = project()
                .andExclude("_id")
                .and(context -> new Document("$literal", yearMonth)).as("yearMonth")
                .and(context -> new Document("$literal", StatsGroupBy.MONTH.name())).as("groupBy")
                .and(context -> new Document("$literal", "TOTAL")).as("key")
                .and("_id").as("currency")
                .and("count").as("count")
                .and("totalAmount").as("totalAmount");

        Aggregation agg = newAggregation(match, group, project);

        List<TransactionStatsDocument> docs =
                mongoTemplate.aggregate(agg, "transactions",
                                TransactionStatsDocument.class)
                        .getMappedResults();

        if (!docs.isEmpty()) {
            statsRepository.saveAll(docs);
        }
    }
}