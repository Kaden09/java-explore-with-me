package ru.practicum.ewm.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventUserRequestDto;
import ru.practicum.ewm.service.interfaces.EventService;
import ru.practicum.ewm.service.interfaces.RequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventPrivateController {
    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<EventFullDto> addEvent(
            @PathVariable Long userId,
            @RequestBody @Valid NewEventDto newEventDto
    ) {
        log.info("POST запрос на создание события: userId={}, title={}", userId, newEventDto.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.addEvent(userId, newEventDto));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventByOwner(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventUserRequestDto updateEvent
    ) {
        log.info("PATCH запрос на обновление события владельцем: userId={}, eventId={}, stateAction={}",
                userId, eventId, updateEvent.getStateAction());

        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.updateEventByOwner(userId, eventId, updateEvent));
    }

    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResultDto> updateRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequestDto request
    ) {
        log.info("PATCH запрос на обновление статусов заявок: userId={}, eventId={}, status={}",
                userId, eventId, request.getStatus());

        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.updateRequestsStatus(userId, eventId, request));
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEventsByOwner(
            @PathVariable Long userId,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size
    ) {
        log.info("GET запрос на получение событий владельцем: userId={}, from={}, size={}", userId, from, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getEventsByOwner(userId, from, size));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventByOwner(
            @PathVariable Long userId, @PathVariable Long eventId
    ) {
        log.info("GET запрос на получение события владельцем: userId={}, eventId={}", userId, eventId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(eventService.getEventByOwner(userId, eventId));
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsByEventOwner(
            @PathVariable Long userId, @PathVariable Long eventId
    ) {
        log.info("GET запрос на получение заявок по событию владельцем: userId={}, eventId={}", userId, eventId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.getRequestsByEventOwner(userId, eventId));
    }
}