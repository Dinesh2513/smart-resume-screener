package com.smartscreener.exception;

import com.smartscreener.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResumeProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeProcessing(
            ResumeProcessingException exception,
            HttpServletRequest request
    ) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaximumUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return createResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded files exceed the permitted size limit.",
                request,
                List.of()
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MultipartException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "The request is missing a valid resume file.",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> validationErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": "
                                + error.getDefaultMessage()
                )
                .toList();

        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request,
                validationErrors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error(
                "Unexpected request processing error",
                exception
        );

        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> createResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<String> details
    ) {
        ApiErrorResponse response = ApiErrorResponse.create(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(status).body(response);
    }
}