package ru.yandex.market.tpl.carrier.tms.executor.duty_schedule;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutyScheduleGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
class DutyScheduleExecutorTest {

    private final DutyScheduleGenerator dutyScheduleGenerator;
    private final DutyScheduleExecutor dutyScheduleExecutor;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @SneakyThrows
    @Test
    @Disabled
    void shouldEnqueueDutyScheduleRefresh() {
        testUserHelper.deliveryService(DutyScheduleGenerator.DEFAULT_DS_ID);
        dutyScheduleGenerator.generate();

        dbQueueTestUtil.clear(QueueType.REFRESH_DUTY);

        dutyScheduleExecutor.doRealJob(jobExecutionContext);

        dbQueueTestUtil.assertTasksHasSize(QueueType.REFRESH_DUTY, 1);
    }

}
