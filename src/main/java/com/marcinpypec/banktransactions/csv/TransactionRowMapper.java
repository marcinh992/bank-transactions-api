package com.marcinpypec.banktransactions.csv;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class TransactionRowMapper {

    public TransactionDraft map(CSVRecord r) {
        String iban     = get(r, "IBAN");
        String dateStr  = get(r, "date");
        String currency = get(r, "currency");
        String category = get(r, "category");
        String amount   = get(r, "amount");

        return new TransactionDraft(
                iban,
                LocalDate.parse(dateStr),
                currency,
                category,
                new BigDecimal(amount)
        );
    }

    private String get(CSVRecord r, String key) {
        if (r.isMapped(key)) return r.get(key);
        String lower = key.toLowerCase();
        if (r.isMapped(lower)) return r.get(lower);
        String upper = key.toUpperCase();
        if (r.isMapped(upper)) return r.get(upper);
        throw new IllegalArgumentException("Missing column: " + key);
    }
}
