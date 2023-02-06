package ru.yandex.market.tsup.controller.front;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

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
import ru.yandex.mj.generated.client.carrier.model.PageOfDutyScheduleDto;
import ru.yandex.mj.generated.client.carrier.model.ScheduleDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class DutyScheduleControllerTest extends AbstractContextualTest {
    public static final long DELIVERY_SERVICE_ID = 138585L;
    public static final long DUTY_WAREHOUSE_YANDEX_ID = 456L;
    public static final long PARTNER_ID = 567L;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private DutyScheduleApiClient dutyScheduleApiClient;

    @BeforeEach
    void setUp() {
        ExecuteCall<PageOfDutyScheduleDto, RetryStrategy> getSchedulesCall = Mockito.mock(ExecuteCall.class);
        List<DutyScheduleDto> schedules = List.of(
                new DutyScheduleDto()
                        .id(1L)
                        .name("Name")
                        .status(DutyScheduleStatusDto.ACTIVE)
                        .dutyStartTime("09:00")
                        .dutyEndTime("18:00")
                        .dutyPallets(33)
                        .dutyPriceCents(3000_00L)
                        .dutyDeliveryServiceId(DELIVERY_SERVICE_ID)
                        .dutyWarehouseYandexId(DUTY_WAREHOUSE_YANDEX_ID)
                        .schedule(new ScheduleDto()
                                .startDate(LocalDate.of(2022, 1, 10))
                                .endDate(null)
                                .daysOfWeek(List.of(DayOfWeekDto.MONDAY, DayOfWeekDto.TUESDAY, DayOfWeekDto.WEDNESDAY))
                                .holidays(List.of(
                                        LocalDate.of(2022, 5, 1),
                                        LocalDate.of(2022, 5, 2)
                                ))
                        )
        );
        Mockito.when(getSchedulesCall.schedule())
                .thenReturn(CompletableFuture.completedFuture(
                        new PageOfDutyScheduleDto()
                                .content(schedules)
                                .number(0)
                                .size(20)
                                .totalElements(1L)
                                .totalPages(1)
                ));
        Mockito.when(dutyScheduleApiClient.internalDutySchedulesGet(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(getSchedulesCall);

        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(DUTY_WAREHOUSE_YANDEX_ID)
                                .partnerId(PARTNER_ID)
                                .name("PartnerName")
                                .build()
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetDutySchedule() {
        mockMvc.perform(get("/duty-schedules"))
                .andExpect(IntegrationTestUtils.jsonContent("fixture/duty_schedule/duty_schedule_response.json"));
    }
}
