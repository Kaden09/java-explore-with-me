package ru.practicum.ewm.controller.comment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.service.interfaces.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping("/{eventId}")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long userId,
                                                 @PathVariable Long eventId,
                                                 @RequestBody @Valid NewCommentDto newCommentDto) {
        log.info("POST запрос на добавление комментария: userId={}, eventId={}", userId, eventId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(userId, eventId, newCommentDto));
    }

    @PatchMapping("/{eventId}/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long userId,
                                                    @PathVariable Long eventId,
                                                    @PathVariable Long commentId,
                                                    @RequestBody @Valid NewCommentDto newCommentDto) {
        log.info("PATCH запрос на обновление комментария: userId={}, eventId={}, commentId={}",
                userId, eventId, commentId);

        return ResponseEntity.ok(commentService.updateComment(userId, eventId, commentId, newCommentDto));
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getCommentsByAuthor(@PathVariable Long userId,
                                                                @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
                                                                @RequestParam(value = "size", defaultValue = "10") @Positive Integer size) {
        log.info("GET запрос на получение комментариев автора: userId={}, from={}, size={}", userId, from, size);

        return ResponseEntity.ok(commentService.getCommentsByAuthor(userId, from, size));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long userId,
                                              @PathVariable Long commentId) {
        log.info("DELETE запрос на удаление комментария: userId={}, commentId={}", userId, commentId);

        commentService.deleteComment(userId, commentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}