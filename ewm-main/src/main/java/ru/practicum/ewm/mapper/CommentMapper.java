package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.util.Map;

@UtilityClass
public class CommentMapper {

    public Comment toComment(NewCommentDto dto, User author, Event event) {
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setText(dto.getText());
        comment.setCreated(LocalDateTime.now());
        return comment;
    }

    public CommentDto toCommentDto(Comment comment, Long confirmedRequests) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                UserMapper.toUserShortDto(comment.getAuthor()),
                EventMapper.toEventShortDto(comment.getEvent(), confirmedRequests),
                comment.getCreated(),
                comment.getEdited()
        );
    }

    public CommentDto toCommentDto(Comment comment, Map<Long, Long> confirmedRequests) {
        return toCommentDto(
                comment,
                confirmedRequests.getOrDefault(comment.getEvent().getId(), 0L)
        );
    }
}