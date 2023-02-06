package ru.yandex.market.tsup.controller.front;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.DutyScheduleApiClient;
import ru.yandex.mj.generated.client.carrier.model.DayOfWeekDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleStatusDto;
import ru.yandex.mj.generated.client.carrier.model.ScheduleDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DutyScheduleControllerGetByIdTest extends AbstractContextualTest {

    private static final long WAREHOUSE_ID = 12345L;
    private static final long DUTY_WAREHOUSE_PARTNER_ID = 172L;
    private static final long DUTY_MOVING_PARTNER_ID = 138585L;

    @Autowired
    private DutyScheduleApiClient dutyScheduleApiClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(WAREHOUSE_ID)
                                .partnerId(DUTY_WAREHOUSE_PARTNER_ID)
                                .name("partner name")
                                .build()
                ));

        ExecuteCall<DutyScheduleDto, RetryStrategy> getScheduleByIdCall = Mockito.mock(ExecuteCall.class);
        DutyScheduleDto scheduleDto = new DutyScheduleDto()
                .id(111L)
                .name("Duty schedule")
                .status(DutyScheduleStatusDto.ACTIVE)
                .dutyWarehouseYandexId(WAREHOUSE_ID)
                .dutyDeliveryServiceId(DUTY_MOVING_PARTNER_ID)
                .dutyStartTime("09:00")
                .dutyEndTime("18:00")
                .dutyPallets(33)
                .dutyPriceCents(3000_00L)
                .schedule(new ScheduleDto()
                        .startDate(LocalDate.of(2022, 4, 19))
                        .endDate(null)
                        .daysOfWeek(List.of(DayOfWeekDto.MONDAY, DayOfWeekDto.TUESDAY, DayOfWeekDto.WEDNESDAY))
                        .holidays(List.of(LocalDate.of(2022, 5, 1)))
                );
        Mockito.when(getScheduleByIdCall.schedule())
                .thenReturn(CompletableFuture.completedFuture(scheduleDto));

        Mockito.when(dutyScheduleApiClient.internalDutySchedulesIdGet(Mockito.any()))
                .thenReturn(getScheduleByIdCall);
    }

    @SneakyThrows
    @Test
    void shouldGetDutySchedule() {
        mockMvc.perform(MockMvcRequestBuilders.get("/duty-schedules/1"))
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/duty_schedule/duty_schedule_by_id_response.json"));
    }
}
