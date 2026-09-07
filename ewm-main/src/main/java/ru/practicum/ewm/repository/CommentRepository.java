package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.author JOIN FETCH c.event WHERE c.author.id = :userId")
    List<Comment> findAllByAuthorId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author JOIN FETCH c.event WHERE c.event.id = :eventId")
    List<Comment> findAllByEventId(@Param("eventId") Long eventId, Pageable pageable);
}