package com.smartscreener.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {

    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ApiErrorResponse create(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse create(
            int status,
            String error,
            String message,
            String path,
            List<String> details
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                details
        );
    }
}