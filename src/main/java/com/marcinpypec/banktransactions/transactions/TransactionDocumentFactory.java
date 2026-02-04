package com.marcinpypec.banktransactions.transactions;

import com.marcinpypec.banktransactions.csv.TransactionDraft;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class TransactionDocumentFactory {

    public TransactionDocument create(TransactionDraft tx, String jobId, YearMonth yearMonth) {
        TransactionDocument doc = new TransactionDocument();
        doc.setImportJobId(jobId);
        doc.setIban(tx.iban());
        doc.setTransactionDate(tx.transactionDate());
        doc.setCurrency(tx.currency());
        doc.setCategory(tx.category());
        doc.setAmount(tx.amount());
        doc.setYearMonth(yearMonth.toString());
        return doc;
    }
}
