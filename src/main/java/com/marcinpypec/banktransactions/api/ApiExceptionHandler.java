package com.marcinpypec.banktransactions.api;

import com.marcinpypec.banktransactions.importjob.exception.ImportAlreadyExistsException;
import com.marcinpypec.banktransactions.importjob.exception.ImportFileReadException;
import com.marcinpypec.banktransactions.importjob.exception.ImportNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ImportAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleConflict(ImportAlreadyExistsException ex) {
        return ResponseEntity.status(409)
                .body(ApiError.of("IMPORT_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(ImportNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ImportNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiError.of("IMPORT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ImportFileReadException.class)
    public ResponseEntity<ApiError> handleBadRequest(ImportFileReadException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("IMPORT_FILE_INVALID", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500)
                .body(ApiError.of("UNEXPECTED_ERROR", "Unexpected error"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413)
                .body(ApiError.of("FILE_TOO_LARGE", "Uploaded file is too large"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

}
