package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({ValidationException.class, NumberFormatException.class,
            MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class})
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
    public ResponseEntity<ErrorResponse> handleForbiddenException(final ForbiddenException e) {
        log.warn("Доступ запрещён: {}", e.getMessage());

        var error = new ErrorResponse(
                "Forbidden Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.error("Нарушение целостности данных: {}", e.getMessage());

        var error = new ErrorResponse(
                "Data Integrity Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
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
