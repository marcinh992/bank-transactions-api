package com.marcinpypec.banktransactions.transactions;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Document("transactions")
@CompoundIndex(name = "idx_ym_cat", def = "{'yearMonth': 1, 'category': 1}")
@CompoundIndex(name = "idx_ym_iban", def = "{'yearMonth': 1, 'iban': 1}")
public class TransactionDocument {

    @Id
    private String id;
    private String importJobId;
    private String iban;
    private LocalDate transactionDate;
    private String currency;
    private String category;
    private BigDecimal amount;
    private String yearMonth;
}
