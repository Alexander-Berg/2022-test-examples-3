package ru.yandex.market.mboc.tms.service.orchestrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.market.mbo.category.orchestrator.model.MoveRequestDto;
import ru.yandex.market.mbo.category.orchestrator.model.MoveResponseDto;
import ru.yandex.market.openapi.api.CategoryOrchestratorApi;

public class CategoryOrchestratorApiMock extends CategoryOrchestratorApi {
    private Map<Long, MoveRequestDto> requestDtoStorage = new HashMap<>();
    private AtomicLong nextRequestId = new AtomicLong();

    @Override
    public MoveResponseDto move(MoveRequestDto moveRequestDto) {
        var requestId = nextRequestId.getAndIncrement();
        requestDtoStorage.put(requestId, moveRequestDto);
        return new MoveResponseDto()
            .requestId(requestId)
            .status(MoveResponseDto.StatusEnum.OK);
    }

    public List<MoveRequestDto> getRequestDtos() {
        return List.copyOf(requestDtoStorage.values());
    }

    public void invalidateAll() {
        requestDtoStorage = new HashMap<>();
        nextRequestId = new AtomicLong();
    }
}
