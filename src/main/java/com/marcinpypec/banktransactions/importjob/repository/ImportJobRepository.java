package com.marcinpypec.banktransactions.importjob.repository;

import com.marcinpypec.banktransactions.importjob.model.ImportJobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImportJobRepository extends MongoRepository<ImportJobDocument, String> {
    boolean existsByYearMonth(String yearMonth);
}
