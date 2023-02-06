package ru.yandex.market.wms.api.utils;

import java.time.Instant;

import lombok.RequiredArgsConstructor;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.wms.common.model.dto.OrderCheckpointDto;
import ru.yandex.market.wms.common.model.enums.OrderCheckpoint;

@RequiredArgsConstructor
public class OrderCheckpointDtoGenerator {
    private final Instant baseDate = Instant.parse("2020-04-01T12:00:00Z");
    private final String originOrderKey;
    private final String externOrderKey;
    private long passedMillis = 0;

    public OrderCheckpointDtoGenerator(ResourceId orderId) {
        this(orderId.getPartnerId(), orderId.getYandexId());
    }

    public OrderCheckpointDto next(OrderCheckpoint checkpoint) {
        return next(originOrderKey, checkpoint, 10_000);
    }

    public OrderCheckpointDto next(OrderCheckpoint checkpoint, long offsetMillis) {
        return next(originOrderKey, checkpoint, offsetMillis);
    }

    public OrderCheckpointDto next(String orderKey, OrderCheckpoint checkpoint) {
        return next(orderKey, checkpoint, 10_000);
    }

    public OrderCheckpointDto next(String orderKey, OrderCheckpoint checkpoint, long offsetMillis) {
        this.passedMillis += offsetMillis;
        return OrderCheckpointDto.builder()
                .orderKey(orderKey)
                .originOrderKey(originOrderKey)
                .externOrderKey(externOrderKey)
                .checkpoint(checkpoint)
                .addDate(baseDate.plusMillis(this.passedMillis))
                .build();
    }
}
