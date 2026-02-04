package com.marcinpypec.banktransactions.importjob.exception;

public class ImportFileReadException extends RuntimeException {
    public ImportFileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
