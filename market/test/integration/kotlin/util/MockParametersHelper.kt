package ru.yandex.market.logistics.calendaring.util

import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateCustomScheduleResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesCustomScheduleResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


object MockParametersHelper {

    fun mockGatesScheduleResponse(
        gates: Set<LogisticsPointGateResponse>,
        schedule: List<ScheduleDateTimeResponse>
    ): List<LogisticsPointGatesScheduleResponse> {
        return listOf(
            LogisticsPointGatesScheduleResponse.newBuilder().gates(gates).schedule(schedule).logisticsPointId(1L).build()
        )
    }

    fun mockGatesScheduleResponse(
        gates: List<LogisticsPointGateCustomScheduleResponse>
    ): List<LogisticsPointGatesCustomScheduleResponse> {
        return listOf(
            LogisticsPointGatesCustomScheduleResponse.newBuilder().gates(gates).logisticsPointId(1L).build()
        )
    }

    fun mockGatesSchedules(date: LocalDate, from: LocalTime, to: LocalTime): ScheduleDateTimeResponse {
        return ScheduleDateTimeResponse.newBuilder().date(date).from(from).to(to).build()
    }

    fun mockGatesSchedules(
        from: LocalTime, to: LocalTime, vararg workingDays: LocalDate,
    ): List<ScheduleDateTimeResponse> {
        return workingDays.map {
            mockGatesSchedules(
                it,
                from,
                to
            )
        }
    }

    fun mockSingleGateAvailableResponse(
        gate: Long,
        gateType: GateTypeResponse
    ): Set<LogisticsPointGateResponse> {
        return setOf(
            LogisticsPointGateResponse.newBuilder().types(EnumSet.of(gateType)).enabled(true)
                .gateNumber(gate.toString())
                .id(gate).build()
        )
    }

    fun mockAvailableGatesResponse(
        gates: Set<Long>,
        gateTypes: EnumSet<GateTypeResponse>,
        enabled: Boolean = true
    ): Set<LogisticsPointGateResponse> {
        return gates.map {
            LogisticsPointGateResponse.newBuilder()
                .enabled(enabled)
                .gateNumber("gateNumber$it")
                .types(gateTypes)
                .id(it).build()
        }.toSet()
    }

    fun mockAvailableGatesResponse(
        gates: Set<Long>,
        gateTypes: EnumSet<GateTypeResponse>,
        schedule: List<ScheduleDateTimeResponse>,
        enabled: Boolean = true
    ): List<LogisticsPointGateCustomScheduleResponse> {
        return gates.map {
            LogisticsPointGateCustomScheduleResponse.newBuilder()
                .enabled(enabled)
                .gateNumber("gateNumber$it")
                .types(gateTypes)
                .id(it)
                .schedule(schedule)
                .build()
        }.toList()
    }

    fun mockNotAvailableGatesResponse(
        gates: Set<Long>,
        gateType: GateTypeResponse
    ): Set<LogisticsPointGateResponse> {
        return gates.map {
            LogisticsPointGateResponse.newBuilder()
                .enabled(false)
                .gateNumber(it.toString())
                .types(EnumSet.of(gateType))
                .id(it).build()
        }.toSet()
    }

    fun mockNotAvailableGatesResponse(
        gates: Set<Long>,
        gateType: GateTypeResponse,
        schedule: List<ScheduleDateTimeResponse>
    ): List<LogisticsPointGateCustomScheduleResponse> {
        return gates.map {
            LogisticsPointGateCustomScheduleResponse.newBuilder()
                .enabled(false)
                .gateNumber(it.toString())
                .types(EnumSet.of(gateType))
                .id(it)
                .schedule(schedule)
                .build()
        }.toList()
    }

    fun mockBusinessWarehouseResponse(
        address: String
    ): BusinessWarehouseResponse? {
        return BusinessWarehouseResponse.newBuilder().address(
            Address.newBuilder()
                .addressString(address)
                .build()
            )
            .readableName("warehouse name")
            .build()
        }
}
