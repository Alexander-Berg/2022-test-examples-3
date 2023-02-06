package ru.yandex.market.tpl.carrier.core.domain.duty_schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.carrier.core.domain.schedule.Schedule;
import ru.yandex.market.tpl.carrier.core.domain.schedule.ScheduleCommandService;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@Service
@RequiredArgsConstructor
public class DutyScheduleGenerator {

    public static final long DEFAULT_DS_ID = 123L;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final ScheduleCommandService scheduleCommandService;
    private final DutyScheduleCommandService dutyScheduleCommandService;
    private final DutyScheduleGeneratorMapper dutyScheduleGeneratorMapper;

    public DutySchedule generate() {
        return generate(s -> {}, d -> {});
    }

    public DutySchedule generate(
            Consumer<ScheduleGenerateParams.ScheduleGenerateParamsBuilder> scheduleBuilder,
            Consumer<DutyScheduleGenerateParams.DutyScheduleGenerateParamsBuilder> dutyBuilder) {
        OrderWarehouse warehouse = orderWarehouseGenerator.generateWarehouse();

        var schedule =  ScheduleGenerateParams
                .builder()
                .startDate(LocalDate.of(2022, 1, 1))
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
                .holidays(List.of());

        scheduleBuilder.accept(schedule);
        Schedule createdSchedule = scheduleCommandService.create(dutyScheduleGeneratorMapper.map(schedule.build()));

        var defaultBuilder = DutyScheduleGenerateParams.builder()
                .status(DutyScheduleStatus.ACTIVE)
                .dutyDeliveryServiceId(DEFAULT_DS_ID)
                .dutyWarehouseYandexId(Long.parseLong(warehouse.getYandexId()))
                .dutyPallets(33)
                .dutyPriceCents(3000_00L)
                .name("scheduleName")
                .dutyStartTime(LocalTime.of(9, 0))
                .dutyEndTime(LocalTime.of(18, 0));

        dutyBuilder.accept(defaultBuilder);

        return dutyScheduleCommandService.create(dutyScheduleGeneratorMapper.map(defaultBuilder.build(), createdSchedule.getId()));
    }

    @Value
    @Builder
    public static class DutyScheduleGenerateParams {
        Long dutyWarehouseYandexId;
        Long dutyDeliveryServiceId;
        LocalTime dutyStartTime;
        LocalTime dutyEndTime;
        DutyScheduleStatus status;
        int dutyPallets;
        Long dutyPriceCents;
        String name;
        Long dutyId;
    }

    @Value
    @Builder
    public static class ScheduleGenerateParams {
        LocalDate startDate;
        LocalDate endDate;
        @Builder.Default
        Set<DayOfWeek> daysOfWeek = Set.of();
        @Builder.Default
        List<LocalDate> holidays = List.of();
    }
}
