package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.logistics4go.client.model.UpdateWarehouseRequest;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseResponse;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

@ParametersAreNonnullByDefault
public class UpdateWarehouseFactory {
    private final String warehouseExternalId;
    private final long partnerId;
    private final String suffix;
    private final String newSuffix;
    private final boolean onlyRequired;

    public UpdateWarehouseFactory(String warehouseExternalId, long partnerId, String suffix, boolean onlyRequired) {
        this.warehouseExternalId = warehouseExternalId;
        this.partnerId = partnerId;
        this.suffix = suffix;
        this.newSuffix = suffix + "-new";
        this.onlyRequired = onlyRequired;
    }

    @Nonnull
    public UpdateWarehouseRequest updateWarehouseRequest() {
        UpdateWarehouseRequest result = new UpdateWarehouseRequest()
            .name(WarehouseFactory.addSuffix("warehouse-name", newSuffix))
            .schedule(List.of(WarehouseFactory.scheduleDay(2)));

        if (!onlyRequired) {
            result.contact(WarehouseFactory.warehouseContact(newSuffix));
        }

        return result;
    }

    @Nonnull
    public WarehouseResponse warehouseResponse(long id) {
        WarehouseResponse result = new WarehouseResponse()
            .id(id)
            .externalId(warehouseExternalId)
            .name(WarehouseFactory.addSuffix("warehouse-name", newSuffix))
            .address(WarehouseFactory.warehouseAddress(suffix, onlyRequired))
            .schedule(List.of(WarehouseFactory.scheduleDay(2)));

        if (!onlyRequired) {
            result.contact(WarehouseFactory.warehouseContact(newSuffix));
        }

        return result;
    }

    @Nonnull
    public LogisticsPointUpdateRequest logisticsPointUpdateRequest() {
        LogisticsPointUpdateRequest.Builder builder = LogisticsPointUpdateRequest.newBuilder()
            .externalId(String.valueOf(warehouseExternalId))
            .name(WarehouseFactory.addSuffix("warehouse-name", newSuffix))
            .address(WarehouseFactory.address(suffix, onlyRequired))
            .active(true)
            .isFrozen(false)
            .schedule(Set.of(WarehouseFactory.scheduleDayResponse(2)))
            .marketBranded(false);

        if (!onlyRequired) {
            builder
                .contact(WarehouseFactory.contact(newSuffix))
                .phones(Set.of(WarehouseFactory.phone(newSuffix)));
        }

        return builder.build();
    }

    @Nonnull
    public LogisticsPointResponse logisticsPointResponse(long id) {
        LogisticsPointResponse.LogisticsPointResponseBuilder builder = LogisticsPointResponse.newBuilder()
            .id(id)
            .externalId(String.valueOf(warehouseExternalId))
            .partnerId(partnerId)
            .type(PointType.WAREHOUSE)
            .name(WarehouseFactory.addSuffix("warehouse-name", newSuffix))
            .address(WarehouseFactory.address(suffix, onlyRequired))
            .active(true)
            .isFrozen(false)
            .schedule(Set.of(WarehouseFactory.scheduleDayResponse(2)));

        if (!onlyRequired) {
            builder
                .contact(WarehouseFactory.contact(newSuffix))
                .phones(Set.of(WarehouseFactory.phone(newSuffix)));
        }

        return builder.build();
    }
}
