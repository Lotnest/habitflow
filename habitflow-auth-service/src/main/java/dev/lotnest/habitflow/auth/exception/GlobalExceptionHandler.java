package dev.lotnest.habitflow.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        log.warn("Validation failed: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid request payload"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException exception) {
        log.warn("Malformed JSON request: {}", exception.getMessage(), exception);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Malformed JSON request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception exception) {
        log.error("An unexpected error occurred: {}", exception.getMessage(), exception);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Something went wrong, please try again later."));
    }

    public record ErrorResponse(String message) {
    }
}
