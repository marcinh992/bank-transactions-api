package com.marcinpypec.banktransactions.importjob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("import_jobs")
public class ImportJobDocument {

    @Id
    private String id;
    private String yearMonth;
    private String fileName;
    private ImportStatus status;

    private int totalRows;
    private int importedRows;
    private int invalidRows;

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    private String errorMessage;
}
