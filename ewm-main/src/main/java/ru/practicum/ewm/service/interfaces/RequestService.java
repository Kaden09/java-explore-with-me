package ru.practicum.ewm.service.interfaces;

import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequestDto;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResultDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    EventRequestStatusUpdateResultDto updateRequestsStatus(Long userId, Long eventId,
                                                           EventRequestStatusUpdateRequestDto statusUpdateRequest);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByEventOwner(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);
}
