package ru.yandex.market.tpl.carrier.planner.controller.api.duty_schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutySchedule;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutyScheduleGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.mj.generated.server.model.DayOfWeekDto;
import ru.yandex.mj.generated.server.model.DutyScheduleDto;
import ru.yandex.mj.generated.server.model.DutyScheduleUpdateDto;
import ru.yandex.mj.generated.server.model.ScheduleUpdateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class DutyScheduleUpdaterTest extends BasePlannerWebTest {

    public static final long ANOTHER_DUTY_DS_ID = 345L;

    private final DutyRepository dutyRepository;

    private final DutyScheduleGenerator dutyScheduleGenerator;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper objectMapper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final Clock testableClock;
    private final TransactionTemplate transactionTemplate;

    private DutySchedule dutySchedule;
    private OrderWarehouse anotherOrderWarehouse;

    @BeforeEach
    void setUp() {
        Mockito.when(testableClock.instant()).thenReturn(
                ZonedDateTime.of(
                        2022, 4, 18, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant()
        );

        testUserHelper.deliveryService(DutyScheduleGenerator.DEFAULT_DS_ID,
                Set.of(testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME))
        );
        testUserHelper.deliveryService(ANOTHER_DUTY_DS_ID, Set.of(
                testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                        .companyName("Company 2")
                        .campaignId(234L)
                        .login("company2@yandex.ru")
                        .build()
                )
        ));

        dutySchedule = dutyScheduleGenerator.generate();
        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        List<Duty> duties =
                dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                        dutySchedule.getId(),
                        testableClock.instant()
                );
        Assertions.assertThat(duties)
                .hasSize(2);

        anotherOrderWarehouse = orderWarehouseGenerator.generateWarehouse();

    }

    @SneakyThrows
    @Test
    void shouldUpdateDutyScheduleAndDuties() {
        var updatedDutyScheduleString = mockMvc.perform(put("/internal/duty-schedules/{id}", dutySchedule.getId())
                        .content(objectMapper.writeValueAsString(
                                new DutyScheduleUpdateDto()
                                        .name("Дежурство в обед")
                                        .schedule(new ScheduleUpdateDto()
                                                .daysOfWeek(List.of(
                                                        DayOfWeekDto.MONDAY, DayOfWeekDto.TUESDAY, DayOfWeekDto.WEDNESDAY
                                                ))
                                                .startDate(LocalDate.of(2022, 1, 2))
                                        )
                                        .dutyStartTime(LocalTime.of(13, 0).toString())
                                        .dutyEndTime(LocalTime.of(16, 0).toString())
                                        .dutyPallets(11)
                                        .dutyPriceCents(9000L)
                                        .dutyDeliveryServiceId(ANOTHER_DUTY_DS_ID)
                                        .dutyWarehouseYandexId(Long.parseLong(anotherOrderWarehouse.getYandexId()))
                        ))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        DutyScheduleDto dutyScheduleDto = objectMapper.readValue(updatedDutyScheduleString, DutyScheduleDto.class);
        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        transactionTemplate.execute(tc -> {
            List<Duty> duties =
                    dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                            dutyScheduleDto.getId(),
                            testableClock.instant()
                    );

            Assertions.assertThat(duties).hasSize(4);

            List<Duty> dutiesForApril18 = StreamEx.of(duties)
                    .filter(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).isEqual(LocalDate.of(2022, 4, 18)))
                    .collect(Collectors.toList());

            Assertions.assertThat(dutiesForApril18).hasSize(2);
            Assertions.assertThat(dutiesForApril18).extracting(Duty::getStatus).containsExactlyInAnyOrder(DutyStatus.CANCELLED, DutyStatus.CREATED);

            Duty duty = StreamEx.of(dutiesForApril18)
                            .filterBy(Duty::getStatus, DutyStatus.CREATED)
                            .findFirst().orElseThrow();

            Assertions.assertThat(DateTimeUtil.toLocalTime(duty.getDutyStartTime(), DateTimeUtil.DEFAULT_ZONE_ID))
                    .isEqualTo("13:00");
            Assertions.assertThat(DateTimeUtil.toLocalTime(duty.getDutyEndTime(), DateTimeUtil.DEFAULT_ZONE_ID))
                    .isEqualTo("16:00");
            Assertions.assertThat(duty.getPallets())
                    .isEqualTo(11);
            Assertions.assertThat(duty.getDutyWarehouse().getId())
                    .isEqualTo(anotherOrderWarehouse.getId());
            Assertions.assertThat(duty.getRun().getPriceCents())
                    .isEqualTo(9000L);
            Assertions.assertThat(duty.getRun().getDeliveryServiceId())
                    .isEqualTo(ANOTHER_DUTY_DS_ID);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldCancelDuties() {
        var updatedDutyScheduleString = mockMvc.perform(put("/internal/duty-schedules/{id}", dutySchedule.getId())
                        .content(objectMapper.writeValueAsString(
                                new DutyScheduleUpdateDto()
                                        .name("Дежурство в обед")
                                        .schedule(new ScheduleUpdateDto()
                                                .daysOfWeek(List.of(
                                                        DayOfWeekDto.TUESDAY, DayOfWeekDto.WEDNESDAY
                                                ))
                                                .startDate(LocalDate.of(2022, 1, 2))
                                        )
                                        .dutyStartTime(LocalTime.of(9, 0).toString())
                                        .dutyEndTime(LocalTime.of(18, 0).toString())
                                        .dutyPallets(33)
                                        .dutyPriceCents(3000_00L)
                                        .dutyDeliveryServiceId(DutyScheduleGenerator.DEFAULT_DS_ID)
                                        .dutyWarehouseYandexId(Long.parseLong(anotherOrderWarehouse.getYandexId()))
                        ))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);
        DutyScheduleDto dutyScheduleDto = objectMapper.readValue(updatedDutyScheduleString, DutyScheduleDto.class);

        transactionTemplate.execute(tc -> {
            List<Duty> duties =
                    dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                            dutyScheduleDto.getId(),
                            testableClock.instant()
                    );
            Assertions.assertThat(duties)
                    .hasSize(2);
            Duty duty = StreamEx.of(duties)
                    .findFirst(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).isEqual(LocalDate.of(2022, 4, 18)))
                    .orElseThrow();
            Assertions.assertThat(duty.getStatus())
                    .isEqualTo(DutyStatus.CANCELLED);
            Assertions.assertThat(duty.getRun().getStatus())
                    .isEqualTo(RunStatus.CANCELLED_INCORRECT);
            return null;
        });

    }
}
