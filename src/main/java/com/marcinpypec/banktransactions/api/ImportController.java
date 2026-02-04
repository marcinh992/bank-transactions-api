package com.marcinpypec.banktransactions.api;

import com.marcinpypec.banktransactions.importjob.dto.ImportJobResponse;
import com.marcinpypec.banktransactions.importjob.service.ImportService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImportJobResponse> createImport(
            @RequestParam("yearMonth")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must be yyyy-MM")
            String yearMonth,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.accepted().body(importService.createImport(yearMonth, file));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ImportJobResponse> getImport(@PathVariable String jobId) {
        return ResponseEntity.ok(importService.getImport(jobId));
    }
}
