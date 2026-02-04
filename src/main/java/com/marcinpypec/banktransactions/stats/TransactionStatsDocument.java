package com.marcinpypec.banktransactions.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document("transaction_stats")
@CompoundIndex(name = "ux_stats", def = "{'yearMonth': 1, 'groupBy': 1, 'key': 1, 'currency': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatsDocument {

    @Id
    private String id;

    private String yearMonth;
    private String groupBy;
    private String key;
    private String currency;

    private long count;
    private BigDecimal totalAmount;
}
