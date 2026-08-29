package ru.practicum.ewm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.StatsClient;
import ru.practicum.ewm.ViewStats;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.ConfirmedRequestsDto;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.RequestStatus;
import ru.practicum.ewm.model.enums.State;
import ru.practicum.ewm.model.enums.StateActionAdmin;
import ru.practicum.ewm.model.enums.StateActionPrivate;
import ru.practicum.ewm.repository.*;
import ru.practicum.ewm.service.interfaces.EventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventServiceImpl implements EventService {
    final EventRepository eventRepository;
    final UserRepository userRepository;
    final CategoryRepository categoryRepository;
    final LocationRepository locationRepository;
    final RequestRepository requestRepository;
    final StatsClient statsClient;

    @Value("${app}")
    String app;

    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        log.debug("Добавление события: userId={}, title={}", userId, dto.getTitle());

        checkActualTime(dto.getEventDate());

        User user = getUser(userId);
        Category category = getCategory(dto.getCategory());
        Location location = resolveLocation(LocationMapper.toLocation(dto.getLocation()));

        Event event = EventMapper.toEvent(dto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(location);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);

        return EventMapper.toEventFullDto(eventRepository.save(event), 0L);
    }

    public EventFullDto updateEventByOwner(Long userId, Long eventId, UpdateEventUserRequestDto dto) {
        log.debug("Обновление события владельцем: userId={}, eventId={}", userId, eventId);

        Event event = getEventByOwnerId(eventId, userId);

        if (event.getState() == State.PUBLISHED) {
            throw new ForbiddenException("Published events can't be updated");
        }

        applyUpdate(event, dto);

        if (dto.getStateAction() != null) {
            StateActionPrivate action = StateActionPrivate.valueOf(dto.getStateAction());
            event.setState( switch (action) {
                case SEND_TO_REVIEW -> State.PENDING;
                case CANCEL_REVIEW -> State.CANCELED;
            });
        }

        return toEventFullDto(eventRepository.save(event));
    }

    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequestDto dto) {
        log.debug("Обновление события админом: eventId={}", eventId);

        Event event = getEvent(eventId);

        if (dto.getStateAction() != null) {
            StateActionAdmin action = StateActionAdmin.valueOf(dto.getStateAction());
            validateStateTransition(event, action);
            event.setState( switch (action) {
                case PUBLISH_EVENT -> {
                    event.setPublishedOn(LocalDateTime.now());
                    yield State.PUBLISHED;
                }
                case REJECT_EVENT -> State.CANCELED;
            });
        }

        applyUpdate(event, dto);

        return toEventFullDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size) {
        log.debug("Запрос событий владельца: userId={}, from={}, size={}", userId, from, size);

        validatePagination(from, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size));
        Map<Long, Long> confirmed = getConfirmedRequests(events);

        return events.stream()
                .map(e -> EventMapper.toEventShortDto(e, confirmed.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getEventByOwner(Long userId, Long eventId) {
        log.debug("Запрос события владельца: userId={}, eventId={}", userId, eventId);

        return toEventFullDto(getEventByOwnerId(eventId, userId));
    }

    @Transactional(readOnly = true)
    public List<EventFullDtoWithViews> getEventsByAdminParams(
            List<Long> users, List<String> states, List<Long> categories,
            LocalDateTime rangeStart, LocalDateTime rangeEnd,
            Integer from, Integer size) {
        log.debug("Запрос событий админом: usersCount={}, statesCount={}, categoriesCount={}, from={}, size={}",
                users == null ? 0 : users.size(),
                states == null ? 0 : states.size(),
                categories == null ? 0 : categories.size(),
                from, size);

        validateDateRange(rangeStart, rangeEnd);
        validatePagination(from, size);

        Specification<Event> spec = buildAdminSpec(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size)).getContent();

        return toEventFullDtoWithViews(events);
    }

    @Transactional(readOnly = true)
    public List<EventShortDtoWithViews> getEvents(
            String text, List<Long> categories, Boolean paid,
            LocalDateTime rangeStart, LocalDateTime rangeEnd,
            Boolean onlyAvailable, String sort, Integer from, Integer size,
            HttpServletRequest request) {
        log.debug("Публичный запрос событий: text={}, categoriesCount={}, paid={}, sort={}, from={}, size={}",
                text,
                categories == null ? 0 : categories.size(),
                paid,
                sort,
                from,
                size);

        validateDateRange(rangeStart, rangeEnd);
        validatePagination(from, size);

        Specification<Event> spec = buildPublicSpec(text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        PageRequest pageRequest = buildPageRequest(from, size, sort);

        List<Event> events = eventRepository.findAll(spec, pageRequest).getContent();
        List<EventShortDtoWithViews> result = toEventShortDtoWithViews(events);

        saveHit(request);
        return result;
    }

    @Transactional(readOnly = true)
    public EventFullDtoWithViews getEventById(Long eventId, HttpServletRequest request) {
        log.debug("Запрос события по id={}", eventId);

        Event event = getEvent(eventId);
        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Event must be published.");
        }

        EventFullDtoWithViews result = toEventFullDtoWithViews(List.of(event)).getFirst();
        saveHit(request);
        return result;
    }

    private void checkActualTime(LocalDateTime eventTime) {
        if (eventTime == null || eventTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event must be scheduled at least 2 hours from now.");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Start date must not be after end date.");
        }
    }

    private void validatePagination(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be >= 0");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be > 0");
        }
    }

    private void validateStateTransition(Event event, StateActionAdmin action) {
        switch (action) {
            case PUBLISH_EVENT -> {
                if (event.getState() != State.PENDING) {
                    throw new ForbiddenException("Event can't be published because it's not pending");
                }
            }
            case REJECT_EVENT -> {
                if (event.getState() == State.PUBLISHED) {
                    throw new ForbiddenException("Event can't be rejected because it's already published");
                }
            }
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category getCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Event getEventByOwnerId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Location resolveLocation(Location location) {
        return locationRepository
                .findByLatAndLon(location.getLat(), location.getLon())
                .orElseGet(() -> locationRepository.save(location));
    }

    private void applyUpdate(Event event, UpdateEventUserRequestDto dto) {
        ifPresent(dto.getAnnotation(), event::setAnnotation);
        ifPresent(dto.getDescription(), event::setDescription);
        ifPresent(dto.getTitle(), event::setTitle);

        if (dto.getCategory() != null) {
            event.setCategory(getCategory(dto.getCategory()));
        }
        if (dto.getEventDate() != null) {
            checkActualTime(dto.getEventDate());
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(resolveLocation(LocationMapper.toLocation(dto.getLocation())));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
    }

    private void applyUpdate(Event event, UpdateEventAdminRequestDto dto) {
        ifPresent(dto.getAnnotation(), event::setAnnotation);
        ifPresent(dto.getDescription(), event::setDescription);
        ifPresent(dto.getTitle(), event::setTitle);

        if (dto.getCategory() != null) {
            event.setCategory(getCategory(dto.getCategory()));
        }
        if (dto.getEventDate() != null) {
            checkActualTime(dto.getEventDate());
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(resolveLocation(LocationMapper.toLocation(dto.getLocation())));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
    }

    private void ifPresent(String value, Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }


    private Specification<Event> buildAdminSpec(List<Long> users, List<String> states,
                                                List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Specification<Event> spec = Specification.where(null);
        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("initiator").get("id").in(users));
        }
        if (states != null && !states.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("state").as(String.class).in(states));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("category").get("id").in(categories));
        }
        if (rangeStart != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }
        return spec;
    }

    private Specification<Event> buildPublicSpec(String text, List<Long> categories, Boolean paid,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 Boolean onlyAvailable) {
        Specification<Event> spec = Specification.where(null);

        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("category").get("id").in(categories));
        }
        if (paid != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("paid"), paid));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = rangeStart != null ? rangeStart : now;
        spec = spec.and((root, q, cb) -> cb.greaterThan(root.get("eventDate"), startDateTime));

        if (rangeEnd != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("eventDate"), rangeEnd));
        }

        spec = spec.and((root, q, cb) -> cb.equal(root.get("state"), State.PUBLISHED));

        return spec;
    }

    private PageRequest buildPageRequest(Integer from, Integer size, String sort) {
        if (sort == null) {
            return PageRequest.of(from / size, size);
        }
        return switch (sort) {
            case "EVENT_DATE" -> PageRequest.of(from / size, size, Sort.by("eventDate"));
            case "VIEWS" -> PageRequest.of(from / size, size, Sort.by("views").descending());
            default -> throw new ValidationException("Unknown sort: " + sort);
        };
    }

    private EventFullDto toEventFullDto(Event event) {
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        return EventMapper.toEventFullDto(event, confirmed);
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getId).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return requestRepository.countConfirmedRequestsByEventIds(ids, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsDto::getEvent,
                        ConfirmedRequestsDto::getCount,
                        (a, b) -> a
                ));
    }

    private Map<String, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(e -> String.format("/events/%s", e.getId()))
                .toList();

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        ResponseEntity<Object> response = statsClient.getStats(start, LocalDateTime.now(), uris, true);

        if (response.getBody() == null) {
            return Collections.emptyMap();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<ViewStats> stats;
        try {
            stats = mapper.convertValue(response.getBody(), new TypeReference<>() {
            });
        } catch (IllegalArgumentException e) {
            return Collections.emptyMap();
        }

        return stats.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits, (a, b) -> a));
    }

    private List<EventFullDtoWithViews> toEventFullDtoWithViews(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<String, Long> views = getViews(events);

        return events.stream()
                .map(event -> {
                    String uri = String.format("/events/%s", event.getId());
                    long hits = views.getOrDefault(uri, 0L);
                    long conf = confirmed.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventFullDtoWithViews(event, hits, conf);
                })
                .toList();
    }

    private List<EventShortDtoWithViews> toEventShortDtoWithViews(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<String, Long> views = getViews(events);

        return events.stream()
                .map(event -> {
                    String uri = String.format("/events/%s", event.getId());
                    long hits = views.getOrDefault(uri, 0L);
                    long conf = confirmed.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDtoWithViews(event, hits, conf);
                })
                .toList();
    }

    private void saveHit(HttpServletRequest request) {
        EndpointHitDto hit = new EndpointHitDto(
                app,
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );
        statsClient.saveHit(hit);
    }
}
