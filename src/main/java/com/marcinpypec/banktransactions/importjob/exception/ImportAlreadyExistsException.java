package com.marcinpypec.banktransactions.importjob.exception;

public class ImportAlreadyExistsException extends RuntimeException {
    public ImportAlreadyExistsException(String yearMonth) {
        super("Import for yearMonth already exists: " + yearMonth);
    }}
