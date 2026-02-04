package com.marcinpypec.banktransactions.api;

public record ApiError(
        String code,
        String message
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message);
    }
}
