package ru.practicum.ewm;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsClient extends BaseClient {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${server.application.name:ewm-main-service}")
    private String applicationName;

    public StatsClient(@Value("${server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> saveHit(HttpServletRequest request) {
        final EndpointHitDto hit = EndpointHitDto.builder()
                .app(applicationName)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(Timestamp.from(Instant.now()).toLocalDateTime())
                .build();
        return post(hit);
    }

    public ResponseEntity<Object> getHit(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String uriBuilder = "/stats?start={start}&end={end}";
        Map<String, Object> parameters = new HashMap<>(Map.of(
                "start", start.format(formatter),
                "end", end.format(formatter)
        ));

        if (uris != null) {
            parameters.put("uris", String.join(",", uris));
        }
        if (unique) {
            parameters.put("unique", true);
        }
        return get(uriBuilder, parameters);
    }
}