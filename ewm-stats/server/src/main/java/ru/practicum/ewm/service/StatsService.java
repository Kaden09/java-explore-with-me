package ru.practicum.ewm.service;

import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    EndpointHitDto saveHit(EndpointHitDto hit);

    List<ViewStats> getViewStatsList(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
