package ru.yandex.market.checkout.checkouter.tasks.v2.tms;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.tasks.v2.FillEmptyPicUrlInOrderItemTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.FillEmptyPicUrlInOrderItemPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentBnplTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.executor.PartitionExecutors;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.wrapper.TaskInfo;

public class TmsTaskServiceTest extends AbstractServicesTestBase {

    @Autowired
    private TmsTaskService tmsTaskService;

    @Value("${market.checkouter.oms.service.tms.fillEmptyPicUrlInOrderItem.partitions:3}")
    int fillEmptyPicUrlInOrderItemTasksPartitions;

    @Autowired
    private PartitionExecutors partitionExecutors;
    @Autowired
    private FillEmptyPicUrlInOrderItemPartitionTaskV2Factory fillEmptyPicUrlInOrderItemPartitionTaskV2Factory;

    @Test
    public void changeEnabled() {
        var taskInfo = getTaskInfoByDescriptionVerbose(InspectExpiredPaymentBnplTaskV2.DESCRIPTION_VERBOSE);
        var taskName = taskInfo.getJobKey().getName();
        Assertions.assertFalse(tmsTaskService.getEnabled(taskName));
        tmsTaskService.setEnabled(taskName, true);
        var enabledTask = getTaskInfoByDescriptionVerbose(InspectExpiredPaymentBnplTaskV2.DESCRIPTION_VERBOSE);
        Assertions.assertTrue(tmsTaskService.getEnabled(enabledTask.getJobKey().getName()));
    }

    @Test
    public void autoRegisterPartitionTask() throws SchedulerException {
        var executorGroupName = fillEmptyPicUrlInOrderItemPartitionTaskV2Factory.getGeneralTaskName() + "Executor";
        tmsTaskService.setEnabled(executorGroupName, true);

        var fillEmptyPicUrlInOrderItemTasks = tmsTaskService.getAllTaskInfos()
                .stream()
                .filter(task -> task.getTaskName().contains(FillEmptyPicUrlInOrderItemTaskV2.class.getSimpleName()))
                .filter(TaskInfo::isEnabled)
                .map(TaskInfo::getTaskName)
                .collect(Collectors.toList());

        Assertions.assertEquals(fillEmptyPicUrlInOrderItemTasksPartitions, fillEmptyPicUrlInOrderItemTasks.size());
        var fillEmptyPicUrlInOrderItemNumber = fillEmptyPicUrlInOrderItemTasks
                .stream()
                .filter(taskName -> taskName.matches(FillEmptyPicUrlInOrderItemTaskV2.class.getSimpleName()
                        + "_([0-9]+)"))
                .collect(Collectors.toList());
        Assertions.assertEquals(fillEmptyPicUrlInOrderItemTasksPartitions - 1,
                fillEmptyPicUrlInOrderItemNumber.size(), fillEmptyPicUrlInOrderItemTasks.toString());
    }

    @Test
    public void partitionExecutorsRegistered() {
        partitionExecutors.getExecutors().forEach((key, value) ->
                Assertions.assertNotNull(tmsTaskService.getExecutors().get(key)));
    }

    private TaskInfo getTaskInfoByDescriptionVerbose(String descriptionVerbose) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = tmsTaskService.getAllTaskFullInfos().stream()
                    .filter(job -> descriptionVerbose.equals(job.getDescriptionVerbose()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(taskInfo);
        } catch (Exception e) {
            Assertions.fail("Не должно быть ошибки", e);
        }

        return taskInfo;
    }
}
