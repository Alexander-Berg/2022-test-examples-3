package ru.yandex.market.tsup.controller.front;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.DutyScheduleApiClient;
import ru.yandex.mj.generated.client.carrier.model.DayOfWeekDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleStatusDto;
import ru.yandex.mj.generated.client.carrier.model.ScheduleDto;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

public class DutyScheduleControllerUpdateTest extends AbstractContextualTest {

    @Autowired
    private DutyScheduleApiClient dutyScheduleApiClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(345L)
                                .partnerId(172L)
                                .name("partner name")
                                .build()
                ));

        ExecuteCall<DutyScheduleDto, RetryStrategy> updateScheduleCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(updateScheduleCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new DutyScheduleDto()
                                        .id(2L)
                                        .status(DutyScheduleStatusDto.ACTIVE)
                                        .name("Duty schedule")
                                        .schedule(new ScheduleDto()
                                                .startDate(LocalDate.of(2022, 4, 18))
                                                .daysOfWeek(List.of(DayOfWeekDto.MONDAY))
                                                .holidays(List.of())
                                        )
                                        .dutyStartTime("09:00")
                                        .dutyEndTime("18:00")
                                        .dutyPriceCents(3000_00L)
                                        .dutyPallets(33)
                                        .dutyDeliveryServiceId(138585L)
                                        .dutyWarehouseYandexId(345L)
                        ));

        Mockito.when(dutyScheduleApiClient.internalDutySchedulesIdPut(
                Mockito.any(),
                Mockito.any()
        ))
                .thenReturn(updateScheduleCall);
    }

    @SneakyThrows
    @Test
    void shouldUpdateDutySchedule() {
        mockMvc.perform(MockMvcRequestBuilders.put("/duty-schedules/{id}", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("fixture/duty_schedule/duty_schedule_update.json"))
        )
                .andExpect(jsonContent("fixture/duty_schedule/duty_schedule_update_response.json"));


    }
}
