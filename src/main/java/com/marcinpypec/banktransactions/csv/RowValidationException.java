package com.marcinpypec.banktransactions.csv;

public class RowValidationException extends RuntimeException {
    public RowValidationException(String message) {
        super(message);
    }
}
