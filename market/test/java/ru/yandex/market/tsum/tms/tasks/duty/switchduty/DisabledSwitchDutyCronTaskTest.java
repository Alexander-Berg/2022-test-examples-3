package ru.yandex.market.tsum.tms.tasks.duty.switchduty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.tsum.tms.service.CronTaskDutySwitcher;
import ru.yandex.market.tsum.tms.service.StaffGroupNotifier;

import java.util.List;

import static org.easymock.EasyMock.anyString;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = SwitchDutyTaskTestConfig.class)
@RunWith(SpringRunner.class)
public class DisabledSwitchDutyCronTaskTest {

    @Autowired
    StaffGroupNotifier staffGroupNotifier;
    @Autowired
    CronTaskDutySwitcher cronTaskDutySwitcher;
    @Autowired
    List<SwitchDutyCronTask> switchDutyCronTasks;
    @Mock
    ExecutionContext executionContext;


    @Test
    public void switchDutyCronTask_NoInteractionsWithSwitcherAndNotifierWhenDisabled() {
        executeTasks(switchDutyCronTasks);

        Mockito.verify(cronTaskDutySwitcher, Mockito.never()).switchDuty(anyString(), anyString());
        Mockito.verify(staffGroupNotifier, Mockito.never()).notifyStaffGroup(any());
    }

    void executeTasks(List<SwitchDutyCronTask> tasks) {
        tasks
            .forEach(switchDutyCronTask -> switchDutyCronTask.execute(executionContext));
    }

}