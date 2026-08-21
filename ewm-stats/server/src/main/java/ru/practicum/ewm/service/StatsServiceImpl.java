package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStats;
import ru.practicum.ewm.mapper.EndpointHitMapper;
import ru.practicum.ewm.model.EndpointHitModel;
import ru.practicum.ewm.repository.StatsRepository;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hit) {
        EndpointHitModel endpointHit = statsRepository.save(EndpointHitMapper.toEndpointHitModel(hit));
        return EndpointHitMapper.toEndpointHitDto(endpointHit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStats> getViewStatsList(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new DateTimeException("Wrong timestamp.");
        }
        if (unique) {
            if (uris != null) {
                return statsRepository.getUniqueStatsForUris(uris, start, end);
            }
            return statsRepository.getUniqueStats(start, end);
        } else {
            if (uris != null) {
                return statsRepository.getTotalStatsForUris(uris, start, end);
            }
            return statsRepository.getTotalStats(start, end);
        }
    }

}
//    @Override
//    public List<ViewStats> getViewStatsList(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
//        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
//
//        if(start.isAfter(end)) {
//            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
//        }
//
//        if (unique) {
//            return statRepository.getUniqueStats(start, end, uris);
//        }
//
//        return statRepository.getStats(start, end, uris);
//    }
//}
