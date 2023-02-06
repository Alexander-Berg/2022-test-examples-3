package ru.yandex.market.tpl.carrier.planner.controller.api.duty_schedule;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.DayOfWeekDto;
import ru.yandex.mj.generated.server.model.DutyScheduleCreateDto;
import ru.yandex.mj.generated.server.model.ScheduleCreateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class DutyScheduleCreatorTest extends BasePlannerWebTest {

    private final ObjectMapper objectMapper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;

    @BeforeEach
    void setUp() {
        testUserHelper.deliveryService(DeliveryService.DEFAULT_DS_ID);
        orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("123"));
    }

    @SneakyThrows
    @Test
    void shouldCreateDutySchedule() {
        mockMvc.perform(post("/internal/duty-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new DutyScheduleCreateDto()
                                .name("Расписание")
                                .dutyStartTime("09:00")
                                .dutyEndTime("18:00")
                                .dutyDeliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                                .dutyWarehouseYandexId(123L)
                                .dutyPallets(33)
                                .dutyPriceCents(3000_00L)
                                .schedule(new ScheduleCreateDto()
                                    .startDate(LocalDate.of(2022, 4, 12))
                                    .holidays(List.of(LocalDate.of(2022, 5, 9)))
                                    .daysOfWeek(List.of(DayOfWeekDto.MONDAY, DayOfWeekDto.TUESDAY, DayOfWeekDto.WEDNESDAY))
                                )
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.schedule.holidays[0]").value("2022-05-09"))
        ;
    }
}
