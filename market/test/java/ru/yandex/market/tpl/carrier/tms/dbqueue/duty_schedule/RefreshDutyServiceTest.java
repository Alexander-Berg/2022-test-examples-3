package ru.yandex.market.tpl.carrier.tms.dbqueue.duty_schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutySchedule;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutyScheduleGenerator;
import ru.yandex.market.tpl.carrier.core.domain.schedule.ScheduleCommandService;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@TmsIntTest
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RefreshDutyServiceTest {

    private final DbQueueTestUtil dbQueueTestUtil;
    private final DutyScheduleGenerator dutyScheduleGenerator;
    private final TestUserHelper testUserHelper;
    private final TestableClock clock;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TransactionTemplate transactionTemplate;
    private final DutyRepository dutyRepository;
    private final ScheduleCommandService scheduleCommandService;

    private OrderWarehouse orderWarehouse;

    @BeforeEach
    void setUp() {
        clock.setFixed(
                ZonedDateTime.of(
                        2022, 4, 18, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant(),
                DateTimeUtil.DEFAULT_ZONE_ID
        );

        testUserHelper.deliveryService(DutyScheduleGenerator.DEFAULT_DS_ID, Set.of(
                testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME)
        ));
        orderWarehouse = orderWarehouseGenerator.generateWarehouse();
    }

    @Test
    void shouldCreateNewDuties() {
        LocalDate now = LocalDate.now(clock);
        DutySchedule dutySchedule = dutyScheduleGenerator.generate(
                s -> s
                        .startDate(now),
                ds -> ds
                        .dutyWarehouseYandexId(Long.parseLong(orderWarehouse.getYandexId()))
        );

        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        transactionTemplate.execute(tc -> {
            List<Duty> duties = dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                    dutySchedule.getId(), Instant.now(clock)
            );
            Assertions.assertThat(duties).hasSize(2);
            Duty dutyForToday = StreamEx.of(duties)
                    .findAny(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).equals(now))
                    .orElseThrow();
            Assertions.assertThat(dutyForToday.getStatus()).isEqualTo(DutyStatus.CREATED);
            Assertions.assertThat(dutyForToday.getRun().getDeliveryServiceId()).isEqualTo(DutyScheduleGenerator.DEFAULT_DS_ID);
            Assertions.assertThat(dutyForToday.getDutyWarehouse()).isEqualTo(orderWarehouse);

            Duty dutyForTomorrow = StreamEx.of(duties)
                    .findAny(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).equals(now.plusDays(1)))
                    .orElseThrow();
            Assertions.assertThat(dutyForTomorrow.getStatus()).isEqualTo(DutyStatus.CREATED);
            Assertions.assertThat(dutyForTomorrow.getRun().getDeliveryServiceId()).isEqualTo(DutyScheduleGenerator.DEFAULT_DS_ID);
            Assertions.assertThat(dutyForTomorrow.getDutyWarehouse()).isEqualTo(orderWarehouse);
            return null;
        });
    }

    @Test
    void shouldCancelDutiesIfEndDateIsToday() {
        LocalDate now = LocalDate.now(clock);
        DutySchedule dutySchedule = dutyScheduleGenerator.generate(
                s -> s
                        .startDate(now),
                ds -> ds
                        .dutyWarehouseYandexId(Long.parseLong(orderWarehouse.getYandexId()))
        );

        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        transactionTemplate.execute(tc -> {
            List<Duty> duties = dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                    dutySchedule.getId(), Instant.now(clock)
            );
            Assertions.assertThat(duties).hasSize(2);
            return null;
        });

        scheduleCommandService.closeSchedule(dutySchedule.getSchedule().getId(), LocalDate.now(clock));
        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        transactionTemplate.execute(tc -> {
            List<Duty> duties = dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(
                    dutySchedule.getId(), Instant.now(clock)
            );
            Assertions.assertThat(duties).hasSize(2);

            Duty dutyForToday = StreamEx.of(duties)
                    .findAny(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).equals(now))
                    .orElseThrow();
            Assertions.assertThat(dutyForToday.getStatus()).isEqualTo(DutyStatus.CREATED);

            Duty dutyForTomorrow = StreamEx.of(duties)
                    .findAny(d -> DateTimeUtil.toLocalDate(d.getDutyStartTime()).equals(now.plusDays(1)))
                    .orElseThrow();
            Assertions.assertThat(dutyForTomorrow.getStatus()).isEqualTo(DutyStatus.CANCELLED);
            return null;
        });
    }
}
