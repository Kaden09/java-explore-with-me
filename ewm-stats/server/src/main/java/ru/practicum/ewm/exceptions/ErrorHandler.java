package ru.practicum.ewm.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            DateTimeException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(final Exception e) {
        log.warn("Ошибка валидации: {}", e.getMessage());

        var error = new ErrorResponse(
                "Bad Request",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNotFoundException(final NotFoundException e) {
        log.warn("Ресурс не найден: {}", e.getMessage());

        var error = new ErrorResponse(
                "Not Found Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleThrowable(final Throwable e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);

        var error = new ErrorResponse(
                "Internal Server Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
