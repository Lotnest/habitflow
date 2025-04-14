package dev.lotnest.habitflow.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
@Profile("prod")
@Slf4j
public class GlobalExceptionHandlerProd {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        log.debug("Authentication error: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        log.debug("Validation failed: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid request payload"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException exception) {
        log.debug("Malformed JSON: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Malformed JSON request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception exception) {
        log.error("Unexpected error: {}", exception.getMessage(), exception);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Something went wrong, please try again later."));
    }

    public record ErrorResponse(String message) {}
}
