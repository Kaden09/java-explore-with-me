package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequestDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.request.ConfirmedRequestsDto;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.service.interfaces.CompilationService;

import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.ewm.model.enums.RequestStatus.CONFIRMED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        log.debug("Добавление подборки: pinned={}, title={}, eventsCount={}",
                newCompilationDto.getPinned(),
                newCompilationDto.getTitle(),
                newCompilationDto.getEvents() == null ? 0 : newCompilationDto.getEvents().size());

        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = eventRepository.findAllByIdIn(newCompilationDto.getEvents());
            compilation.setEvents(events);
        }

        Compilation saved = compilationRepository.save(compilation);
        return toCompilationDtoWithEvents(saved);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequestDto updateCompilation) {
        log.debug("Обновление подборки: compId={}, events={}, pinned={}, title={}",
                compId,
                updateCompilation.getEvents(),
                updateCompilation.getPinned(),
                updateCompilation.getTitle());

        Compilation compilation = getCompilation(compId);

        if (updateCompilation.getEvents() != null) {
            Set<Event> events = updateCompilation.getEvents().isEmpty()
                    ? Collections.emptySet()
                    : eventRepository.findAllByIdIn(updateCompilation.getEvents());
            compilation.setEvents(events);
        }

        if (updateCompilation.getPinned() != null) {
            compilation.setPinned(updateCompilation.getPinned());
        }

        if (updateCompilation.getTitle() != null && !updateCompilation.getTitle().isBlank()) {
            compilation.setTitle(updateCompilation.getTitle());
        }

        Compilation updated = compilationRepository.save(compilation);
        return toCompilationDtoWithEvents(updated);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.debug("Запрос подборок: pinned={}, from={}, size={}", pinned, from, size);

        validatePagination(from, size);
        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations = pinned != null
                ? compilationRepository.findAllByPinned(pinned, pageable)
                : compilationRepository.findAll(pageable).getContent();

        return compilations.stream()
                .map(this::toCompilationDtoWithEvents)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compilationId) {
        log.debug("Запрос подборки по id={}", compilationId);

        Compilation compilation = getCompilation(compilationId);
        return toCompilationDtoWithEvents(compilation);
    }

    @Transactional
    public void deleteCompilation(Long compilationId) {
        log.debug("Удаление подборки: id={}", compilationId);

        compilationRepository.delete(getCompilation(compilationId));
    }

    private Compilation getCompilation(Long compilationId) {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Compilation id=" + compilationId + " not found"));
    }

    private CompilationDto toCompilationDtoWithEvents(Compilation compilation) {
        CompilationDto dto = CompilationMapper.toCompilationDto(compilation);

        if (compilation.getEvents() == null || compilation.getEvents().isEmpty()) {
            dto.setEvents(Collections.emptyList());
            return dto;
        }

        List<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequests = requestRepository
                .countConfirmedRequestsByEventIds(eventIds, CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsDto::getEvent,
                        ConfirmedRequestsDto::getCount,
                        (a, b) -> a
                ));

        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());

        dto.setEvents(eventDtos);
        return dto;
    }

    private void validatePagination(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be >= 0");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be > 0");
        }
    }
}
