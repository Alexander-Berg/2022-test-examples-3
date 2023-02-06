package ru.yandex.market.ff.controller.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

public class MockParametersHelper {
    private MockParametersHelper() {
    }

    public static List<LogisticsPointGatesScheduleResponse> mockGatesScheduleResponse(
            Set<LogisticsPointGateResponse> gates,
            List<ScheduleDateTimeResponse> schedule) {
        return ImmutableList.of(
                LogisticsPointGatesScheduleResponse.newBuilder().gates(gates).schedule(schedule).build()
        );
    }

    public static ScheduleDateTimeResponse mockGatesSchedules(LocalDate date, LocalTime from, LocalTime to) {
        return ScheduleDateTimeResponse.newBuilder().date(date).from(from).to(to).build();
    }

    public static Set<LogisticsPointGateResponse> mockSingleGateAvailableResponse(long gate,
                                                                                  GateTypeResponse gateType) {
        return ImmutableSet
                .of(LogisticsPointGateResponse.newBuilder().types(EnumSet.of(gateType)).enabled(true)
                        .gateNumber(String.valueOf(gate))
                        .id(gate).build());
    }

    public static Set<LogisticsPointGateResponse> mockAvailableGatesResponse(Set<Long> gates,
                                                                             GateTypeResponse gateType) {
        return gates.stream().map(gateId -> LogisticsPointGateResponse.newBuilder()
                .enabled(true)
                .gateNumber(String.valueOf(gateId))
                .types(EnumSet.of(gateType))
                .id(gateId).build()).collect(Collectors.toSet());
    }
}
