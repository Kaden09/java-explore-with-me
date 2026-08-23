package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.model.EndpointHitModel;

@UtilityClass
public class EndpointHitMapper {
    public EndpointHitModel toEndpointHitModel(EndpointHitDto hit) {
        return new EndpointHitModel(
                hit.getApp(),
                hit.getUri(),
                hit.getIp(),
                hit.getTimestamp()
        );
    }

    public EndpointHitDto toEndpointHitDto(EndpointHitModel hit) {
        return new EndpointHitDto(
                hit.getApp(),
                hit.getUri(),
                hit.getIp(),
                hit.getTimestamp()
        );
    }
}