package ru.yandex.market.tpl.carrier.core.domain.duty;

import java.time.Instant;
import java.util.function.Function;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.carrier.core.domain.duty.commands.DutyCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@Service
@RequiredArgsConstructor
public class DutyGenerator {

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DutyCommandService dutyCommandService;

    public Duty generate() {
        return generate(b -> b);
    }

    public Duty generate(Function<DutyGenerateParams.DutyGenerateParamsBuilder,
            DutyGenerateParams.DutyGenerateParamsBuilder> tuner) {
        OrderWarehouse orderWarehouse = orderWarehouseGenerator.generateWarehouse();
        DutyGenerateParams.DutyGenerateParamsBuilder builder = DutyGenerateParams.builder()
                .deliveryServiceId(123L)
                .dutyStartTime(Instant.parse("2021-01-01T10:00:00.00Z"))
                .dutyEndTime(Instant.parse("2021-01-01T20:00:00.00Z"))
                .pallets(33)
                .priceCents(600000L)
                .name("Дежурство на СЦ")
                .dutyWarehouseId(orderWarehouse.getYandexId());
        return generate(tuner.apply(builder).build());
    }

    public Duty generate(DutyGenerateParams param) {

        return dutyCommandService.create(DutyCommand.Create.builder()
                .dutyStartTime(param.getDutyStartTime())
                .dutyEndTime(param.getDutyEndTime())
                .deliveryServiceId(param.getDeliveryServiceId())
                .warehouseId(Long.parseLong(param.getDutyWarehouseId()))
                .pallets(param.getPallets())
                .priceCents(param.getPriceCents())
                .name(param.getName())
                .build());
    }

    @Value
    @Builder
    public static class DutyGenerateParams {
        String dutyWarehouseId;
        Long deliveryServiceId;
        Instant dutyStartTime;
        Instant dutyEndTime;
        DutyStatus dutyStatus;
        int pallets;
        Long priceCents;
        String name;
    }
}
