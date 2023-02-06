package ru.yandex.market.tpl.carrier.planner.controller.api.duty_schedule;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutySchedule;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutyScheduleGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.service.duty_schedule.RefreshDutyProducer;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class DutyScheduleEndDateTest extends BasePlannerWebTest {

    private final DutyScheduleGenerator dutyScheduleGenerator;
    private final RefreshDutyProducer refreshDutyProducer;
    private final DutyRepository dutyRepository;
    private final TestUserHelper testUserHelper;

    private final DbQueueTestUtil dbQueueTestUtil;

    private final TestableClock clock;

    @Test
    void shouldNotCreateDutyWithStartTimeAfterEndTime() {
        testUserHelper.deliveryService(123L, Set.of(testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME)));
        DutySchedule dutySchedule = dutyScheduleGenerator.generate(s -> {
        }, d -> {
            d
                    .dutyStartTime(LocalTime.of(23, 0))
                    .dutyEndTime(LocalTime.of(2, 0));
        });

        clock.setFixed(
                ZonedDateTime.of(2022, 6, 20, 15, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant(),
                DateTimeUtil.DEFAULT_ZONE_ID
        );

        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        fetchDutiesAndAssertStartEndTime(dutySchedule);

        refreshDutyProducer.produceSingle(dutySchedule.getId());
        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.REFRESH_DUTY);

        fetchDutiesAndAssertStartEndTime(dutySchedule);
    }

    private void fetchDutiesAndAssertStartEndTime(DutySchedule dutySchedule) {
        List<Duty> duties =
                dutyRepository.findAllByDutyScheduleIdAndDutyStartTimeGreaterThan(dutySchedule.getId(),
                        clock.instant());

        Assertions.assertThat(duties).hasSize(2);

        Duty duty = duties.get(0);
        Assertions.assertThat(duty.getDutyStartTime()).isBefore(duty.getDutyEndTime());
    }
}
