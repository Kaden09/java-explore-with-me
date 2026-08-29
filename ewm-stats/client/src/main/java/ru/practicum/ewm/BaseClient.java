package ru.practicum.ewm;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BaseClient {

    protected final RestClient restClient;

    public BaseClient(RestClient restClient) {
        this.restClient = restClient;
    }

    protected <T> ResponseEntity<Object> post(String path, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, null, body);
    }

    protected ResponseEntity<Object> get(String path, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, parameters, null);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path,
                                                          @Nullable Map<String, Object> parameters,
                                                          @Nullable T body) {
        try {
            var uriSpec = restClient.method(method)
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        if (parameters != null) {
                            parameters.forEach((key, value) -> {
                                if (value instanceof Collection<?> collection) {
                                    collection.forEach(item -> uriBuilder.queryParam(key, item));
                                } else {
                                    uriBuilder.queryParam(key, value);
                                }
                            });
                        }
                        return uriBuilder.build();
                    })
                    .headers(headers -> {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    });

            ResponseEntity<Object> response;
            if (body != null) {
                response = ((RestClient.RequestBodyUriSpec) uriSpec)
                        .body(body)
                        .retrieve()
                        .toEntity(Object.class);
            } else {
                response = uriSpec.retrieve().toEntity(Object.class);
            }

            return prepareResponse(response);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }
    }

    private static ResponseEntity<Object> prepareResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());
        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }
        return responseBuilder.build();
    }
}