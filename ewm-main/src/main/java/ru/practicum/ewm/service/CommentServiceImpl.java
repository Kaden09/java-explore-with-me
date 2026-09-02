package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.request.ConfirmedRequestsDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.RequestStatus;
import ru.practicum.ewm.model.enums.State;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.interfaces.CommentService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        log.debug("Добавление комментария: userId={}, eventId={}", userId, eventId);

        User author = checkAndGetUser(userId);
        Event event = checkAndGetEvent(eventId);

        if (event.getState() != State.PUBLISHED) {
            throw new ValidationException("Comments are available only for published events.");
        }

        Comment comment = commentRepository.save(CommentMapper.toComment(dto, author, event));
        return toCommentDto(comment);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto dto) {
        log.debug("Обновление комментария: userId={}, eventId={}, commentId={}", userId, eventId, commentId);

        User user = checkAndGetUser(userId);
        Event event = checkAndGetEvent(eventId);
        Comment comment = checkAndGetComment(commentId);

        if (!comment.getEvent().getId().equals(event.getId())) {
            throw new ValidationException("This comment is for other event.");
        }
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ValidationException("Only author can edit the comment.");
        }

        comment.setText(dto.getText());
        comment.setEdited(LocalDateTime.now());

        return toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByAuthor(Long userId, Integer from, Integer size) {
        log.debug("Запрос комментариев автора: userId={}, from={}, size={}", userId, from, size);

        checkAndGetUser(userId);
        Pageable pageable = makePageable(from, size);

        List<Comment> comments = commentRepository.findAllByAuthorId(userId, pageable);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(comments);

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(c, confirmedRequests))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long eventId, Integer from, Integer size) {
        log.debug("Запрос комментариев события: eventId={}, from={}, size={}", eventId, from, size);

        checkAndGetEvent(eventId);
        Pageable pageable = makePageable(from, size);

        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(c, confirmed))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        log.debug("Запрос комментария: id={}", commentId);

        return toCommentDto(checkAndGetComment(commentId));
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.debug("Удаление комментария пользователем: userId={}, commentId={}", userId, commentId);

        User user = checkAndGetUser(userId);
        Comment comment = checkAndGetComment(commentId);

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ValidationException("Only author can delete the comment.");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        log.debug("Удаление комментария администратором: id={}", commentId);

        checkAndGetComment(commentId);
        commentRepository.deleteById(commentId);
    }

    private CommentDto toCommentDto(Comment comment) {
        Long confirmed = requestRepository.countByEventIdAndStatus(
                comment.getEvent().getId(), RequestStatus.CONFIRMED);
        return CommentMapper.toCommentDto(comment, confirmed);
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Comment> comments) {
        List<Long> eventIds = comments.stream()
                .map(c -> c.getEvent().getId())
                .distinct()
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsDto::getEvent,
                        ConfirmedRequestsDto::getCount,
                        (a, b) -> a
                ));
    }

    private Pageable makePageable(Integer from, Integer size) {
        if (from == null || size == null || from < 0 || size <= 0) {
            throw new ValidationException("Invalid pagination parameters");
        }
        return PageRequest.of(from / size, size);
    }

    private User checkAndGetUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event checkAndGetEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Comment checkAndGetComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment with id=" + commentId + " was not found"));
    }
}