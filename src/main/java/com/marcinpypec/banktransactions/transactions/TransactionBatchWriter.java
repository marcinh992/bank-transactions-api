package com.marcinpypec.banktransactions.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionBatchWriter {

    private final TransactionRepository transactionRepository;

    public void saveBatch(List<TransactionDocument> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        transactionRepository.saveAll(batch);
    }
}
