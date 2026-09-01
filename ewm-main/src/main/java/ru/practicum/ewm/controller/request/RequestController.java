package ru.practicum.ewm.controller.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.interfaces.RequestService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users/{userId}/requests")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> addRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId
    ) {
        log.info("POST запрос на создание заявки: userId={}, eventId={}", userId, eventId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.addRequest(userId, eventId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId
    ) {
        log.info("PATCH запрос на отмену заявки: userId={}, requestId={}", userId, requestId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.cancelRequest(userId, requestId));
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsByUser(@PathVariable Long userId) {
        log.info("GET запрос на получение заявок пользователя: userId={}", userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(requestService.getRequestsByUser(userId));
    }
}
