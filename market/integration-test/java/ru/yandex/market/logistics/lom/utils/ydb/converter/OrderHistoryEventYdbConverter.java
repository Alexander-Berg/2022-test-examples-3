package ru.yandex.market.logistics.lom.utils.ydb.converter;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.OrderHistoryEvent;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderHistoryEventTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public final class OrderHistoryEventYdbConverter {

    public Map<String, Object> mapToItem(YdbTableDescription tableDescription, Object o) {
        OrderHistoryEventTableDescription table = (OrderHistoryEventTableDescription) tableDescription;
        OrderHistoryEvent event = (OrderHistoryEvent) o;
        return Map.of(
            table.getEventId().name(), event.getId(),
            table.getEventIdHash().name(), HashUtils.hashLong(event.getId()),
            table.getOrderId().name(), event.getOrderId(),
            table.getOrderIdHash().name(), HashUtils.hashLong(event.getOrderId()),
            table.getCreated().name(), event.getCreated(),
            table.getDiff().name(), event.getDiff()
        );
    }
}
