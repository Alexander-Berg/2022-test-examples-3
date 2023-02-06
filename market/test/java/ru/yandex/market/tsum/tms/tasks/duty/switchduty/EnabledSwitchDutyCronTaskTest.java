package ru.yandex.market.tsum.tms.tasks.duty.switchduty;

import java.time.Instant;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.tms.service.CronTaskDutySwitcher;
import ru.yandex.market.tsum.tms.service.StaffGroupNotificationContext;
import ru.yandex.market.tsum.tms.service.StaffGroupNotifier;
import ru.yandex.market.tsum.tms.service.SwitchDutyResult;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = SwitchDutyTaskTestConfig.class)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "tsum.tms.duty-switch.department.checkouter.enabled=true",
    "tsum.tms.duty-switch.department.sre.enabled=true",
    "tsum.tms.duty-switch.department.idx.enabled=true",
    "tsum.tms.duty-switch.department.ir.enabled=true",
    "tsum.tms.duty-switch.department.logistics-delivery.enabled=true",
    "tsum.tms.duty-switch.department.logistics-wms.enabled=true",
    "tsum.tms.duty-switch.department.mstat-dwh-e2e.enabled=true",
    "tsum.tms.duty-switch.department.mstat-dwh-b2c.enabled=true",
    "tsum.tms.duty-switch.department.mstat-dwh-ba.enabled=true",
    "tsum.tms.duty-switch.department.logistics-ff.enabled=true",
    "tsum.tms.duty-switch.department.market-infra.enabled=true",
    "tsum.tms.duty-switch.department.marketstat-bsi.enabled=true",
    "tsum.tms.duty-switch.department.marketstat-dwh.enabled=true",
    "tsum.tms.duty-switch.department.mbo.enabled=true",
    "tsum.tms.duty-switch.department.vendors.enabled=true",
    "tsum.tms.duty-switch.department.mbi-shops.enabled=true",
    "tsum.tms.duty-switch.department.market-reliability.enabled=true"
})
public class EnabledSwitchDutyCronTaskTest {

    @Autowired
    StaffGroupNotifier staffGroupNotifier;
    @Autowired
    CronTaskDutySwitcher cronTaskDutySwitcher;
    @Autowired
    List<SwitchDutyCronTask> switchDutyCronTasks;
    @Mock
    ExecutionContext executionContext;

    @Test
    public void switchDutyCronTask_DoNotificationIfSwitchDutyFailed() {
        ensureTasksEnabled();
        setupFailedSwitchDutyResult();

        executeTasks(switchDutyCronTasks);

        Mockito.verify(staffGroupNotifier, Mockito.times(switchDutyCronTasks.size()))
            .notifyStaffGroup(any());
    }

    private void ensureTasksEnabled() {
        assertTrue(switchDutyCronTasks.stream().allMatch(SwitchDutyCronTask::enabled));
    }

    void setupFailedSwitchDutyResult() {
        StaffGroupNotificationContext staffGroupNotificationContext = Mockito.mock(StaffGroupNotificationContext.class);
        Mockito.when(staffGroupNotificationContext.getStaffPerson()).thenReturn(new StaffPerson(
            "login",
            1,
            null,
            null,
            null,
            null
        ));
        Mockito.when(staffGroupNotificationContext.getDutyStartInstant()).thenReturn(Instant.now());
        Mockito.when(staffGroupNotificationContext.getDutyEndInstant()).thenReturn(Instant.now());

        SwitchDutyResult result = Mockito.mock(SwitchDutyResult.class);
        Mockito.when(result.isSuccessful()).thenReturn(false);
        Mockito.when(result.getStaffGroupNotificationContext()).thenReturn(staffGroupNotificationContext);
        Mockito.when(cronTaskDutySwitcher.switchDuty(any(), any()))
            .thenReturn(result);
    }

    void executeTasks(List<SwitchDutyCronTask> tasks) {
        tasks
            .forEach(switchDutyCronTask -> switchDutyCronTask.execute(executionContext));
    }
}
