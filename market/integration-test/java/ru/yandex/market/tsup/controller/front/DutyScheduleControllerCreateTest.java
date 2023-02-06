package ru.yandex.market.tsup.controller.front;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.duty_schedule.dto.DutyScheduleCreateDTO;
import ru.yandex.market.tsup.service.data_provider.entity.duty_schedule.dto.ScheduleCreateDTO;
import ru.yandex.mj.generated.client.carrier.api.DutyScheduleApiClient;
import ru.yandex.mj.generated.client.carrier.model.DayOfWeekDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleDto;
import ru.yandex.mj.generated.client.carrier.model.DutyScheduleStatusDto;
import ru.yandex.mj.generated.client.carrier.model.ScheduleDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DutyScheduleControllerCreateTest extends AbstractContextualTest {

    public static final long WAREHOUSE_ID = 345L;
    public static final long DUTY_WAREHOUSE_PARTNER_ID = 172L;
    public static final long DUTY_MOVING_PARTNER_ID = 138585L;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private DutyScheduleApiClient dutyScheduleApiClient;

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

        ExecuteCall<DutyScheduleDto, RetryStrategy> createScheduleCall = Mockito.mock(ExecuteCall.class);
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
        Mockito.when(createScheduleCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(scheduleDto));

        Mockito.when(dutyScheduleApiClient.internalDutySchedulesPost(Mockito.any()))
                .thenReturn(createScheduleCall);

    }

    @SneakyThrows
    @Test
    void shouldCreateDutySchedule() {
       mockMvc.perform(post("/duty-schedules")
               .content(objectMapper.writeValueAsString(
                       DutyScheduleCreateDTO.builder()
                               .name("Duty schedule")
                               .dutyMovingPartnerId(DUTY_MOVING_PARTNER_ID)
                               .dutyPartnerId(DUTY_WAREHOUSE_PARTNER_ID)
                               .dutyPallets(33)
                               .dutyStartTime(LocalTime.of(9, 0))
                               .dutyEndTime(LocalTime.of(18, 0))
                               .schedule(ScheduleCreateDTO.builder()
                                       .startDate(LocalDate.of(2022, 4, 19))
                                       .endDate(null)
                                       .daysOfWeek(List.of(1, 2, 3))
                                       .holidays(List.of(LocalDate.of(2022, 5, 1)))
                                       .build())
                               .dutyPriceRuble(3000L)
                               .build()
               ))
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(IntegrationTestUtils.jsonContent("fixture/duty_schedule/duty_schedule_create_response.json"));
    }
}
