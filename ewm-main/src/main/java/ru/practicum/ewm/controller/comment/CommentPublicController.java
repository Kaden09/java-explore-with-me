package ru.practicum.ewm.controller.comment;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.service.interfaces.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentPublicController {
    private final CommentService commentService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long eventId,
                                                        @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
                                                        @RequestParam(value = "size", defaultValue = "10") @Positive Integer size) {
        log.info("GET запрос на получение комментариев события: eventId={}, from={}, size={}", eventId, from, size);

        return ResponseEntity.ok(commentService.getComments(eventId, from, size));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long commentId) {
        log.info("GET запрос на получение комментария: commentId={}", commentId);

        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }
}