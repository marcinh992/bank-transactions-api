package com.marcinpypec.banktransactions.importjob.exception;

public class ImportNotFoundException extends RuntimeException {
    public ImportNotFoundException(String jobId) {
        super("Import job not found: " + jobId);
    }
}