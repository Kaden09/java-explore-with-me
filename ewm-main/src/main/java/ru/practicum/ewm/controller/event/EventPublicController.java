package ru.practicum.ewm.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDtoWithViews;
import ru.practicum.ewm.dto.event.EventShortDtoWithViews;
import ru.practicum.ewm.service.interfaces.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventPublicController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDtoWithViews>> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero
            Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive
            Integer size,
            HttpServletRequest request
    ) {
        log.info("GET запрос на получение событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                        sort, from, size, request));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDtoWithViews> getEventById(
            @PathVariable Long eventId, HttpServletRequest request
    ) {
        log.info("GET запрос на получение события по id: eventId={}", eventId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getEventById(eventId, request));
    }
}
