package ru.practicum.ewm.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.ViewStats;
import ru.practicum.ewm.model.EndpointHitModel;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHitModel, Long> {
    @Query("SELECT new ru.practicum.ewm.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHitModel AS h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getUniqueStats(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.ewm.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHitModel AS h " +
            "WHERE h.uri IN (:uris) AND h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getUniqueStatsForUris(@Param("uris") List<String> uris,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.ewm.ViewStats(h.app, h.uri, COUNT(h.uri)) " +
            "FROM EndpointHitModel AS h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT (h.uri) DESC")
    List<ViewStats> getTotalStats(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.ewm.ViewStats(h.app, h.uri, COUNT(h.uri)) " +
            "FROM EndpointHitModel AS h " +
            "WHERE h.uri IN (:uris) AND h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT (h.uri) DESC")
    List<ViewStats> getTotalStatsForUris(@Param("uris") List<String> uris,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}





