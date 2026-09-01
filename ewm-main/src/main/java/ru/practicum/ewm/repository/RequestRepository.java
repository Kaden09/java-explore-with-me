package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.dto.request.ConfirmedRequestsDto;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.enums.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    List<ParticipationRequest> findAllByEventIdOrderByCreatedAsc(Long eventId);

    List<ParticipationRequest> findAllByRequesterIdOrderByCreatedAsc(Long userId);

    List<ParticipationRequest> findAllByEventIdAndIdInAndStatus(Long eventId, Collection<Long> requestId, RequestStatus status);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
            SELECT new ru.practicum.ewm.dto.request.ConfirmedRequestsDto(
                COUNT(r.id),
                r.event.id
            )
            FROM ParticipationRequest r
            WHERE r.event.id IN :ids
              AND r.status = :status
            GROUP BY r.event.id
            """)
    List<ConfirmedRequestsDto> countConfirmedRequestsByEventIds(
            @Param("ids") Collection<Long> ids,
            @Param("status") RequestStatus status);
}
