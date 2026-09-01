package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.RequestStatus;
import ru.practicum.ewm.model.enums.State;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.interfaces.RequestService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.debug("Добавление запроса на участие: userId={}, eventId={}", userId, eventId);

        Event event = getEvent(eventId);
        User user = getUser(userId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new DataIntegrityViolationException("Request already exists");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Initiator cannot request own event");
        }
        if (event.getState() != State.PUBLISHED) {
            throw new ForbiddenException("Participation is only possible in published events");
        }
        if (isLimitReached(event)) {
            throw new ForbiddenException("Participant limit has been reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .status(resolveStatus(event))
                .build();

        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Transactional
    public EventRequestStatusUpdateResultDto updateRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequestDto updateRequest) {
        log.debug("Обновление статусов запросов: userId={}, eventId={}, requestsCount={}, targetStatus={}",
                userId,
                eventId,
                updateRequest.getRequestIds() == null ? 0 : updateRequest.getRequestIds().size(),
                updateRequest.getStatus());

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId + " not found or user is not initiator"));

        if (isLimitReached(event)) {
            throw new ForbiddenException("Participant limit has been reached");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventIdAndIdInAndStatus(
                eventId, updateRequest.getRequestIds(), RequestStatus.PENDING);

        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResultDto(List.of(), List.of());
        }

        RequestStatus targetStatus = updateRequest.getStatus();
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (targetStatus == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(request));
            }
        } else {
            long availableSlots = event.getParticipantLimit() -
                    requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            for (ParticipationRequest request : requests) {
                if (event.getParticipantLimit() == 0 || availableSlots > 0) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toParticipationRequestDto(request));
                    availableSlots--;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toParticipationRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);
        return new EventRequestStatusUpdateResultDto(confirmed, rejected);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Отмена запроса на участие: userId={}, requestId={}", userId, requestId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByEventOwner(Long userId, Long eventId) {
        log.debug("Запрос заявок владельцем события: userId={}, eventId={}", userId, eventId);

        checkUserExists(userId);
        eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
        return requestRepository.findAllByEventIdOrderByCreatedAsc(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.debug("Запрос заявок пользователя: userId={}", userId);

        checkUserExists(userId);
        return requestRepository.findAllByRequesterIdOrderByCreatedAsc(userId).stream()
                .map(RequestMapper::toParticipationRequestDto).collect(Collectors.toList());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private boolean isLimitReached(Event event) {
        if (event.getParticipantLimit() == 0) {
            return false;
        }
        long confirmed = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);
        return confirmed >= event.getParticipantLimit();
    }

    private RequestStatus resolveStatus(Event event) {
        if (event.getRequestModeration() && event.getParticipantLimit() > 0) {
            return RequestStatus.PENDING;
        }
        return RequestStatus.CONFIRMED;
    }
}
