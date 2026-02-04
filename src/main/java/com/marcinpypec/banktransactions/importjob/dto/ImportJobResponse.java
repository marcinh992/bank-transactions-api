package com.marcinpypec.banktransactions.importjob.dto;

import com.marcinpypec.banktransactions.importjob.model.ImportStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ImportJobResponse(
        String id,
        String yearMonth,
        String fileName,
        ImportStatus status,
        int totalRows,
        int importedRows,
        int invalidRows,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage

) {}
