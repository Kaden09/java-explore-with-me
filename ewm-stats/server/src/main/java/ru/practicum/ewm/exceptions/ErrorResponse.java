package ru.practicum.ewm.exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String detailedMessage,
        LocalDateTime errorTime
) {
}
