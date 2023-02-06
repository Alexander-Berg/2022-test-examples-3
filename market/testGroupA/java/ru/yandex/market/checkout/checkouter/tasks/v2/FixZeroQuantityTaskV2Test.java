package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.tasks.v2.orderitem.FixZeroQuantityTaskPayload;
import ru.yandex.market.checkout.checkouter.tasks.v2.orderitem.FixZeroQuantityTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TaskPropertiesDao;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TaskPropertiesService;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TmsTaskService;

/**
 * @author zagidullinri
 * @date 17.05.2022
 */
public class FixZeroQuantityTaskV2Test extends AbstractServicesTestBase {

    private static final String STRING_PAYLOAD = "{\"endDate\": \"2022-02-01T10:11:12\", " +
            "\"batchSize\": \"99\", \"startDate\": \"2022-01-01T10:11:12\"}";

    @Autowired
    TaskPropertiesService taskPropertiesService;
    @Autowired
    TaskPropertiesDao taskPropertiesDao;
    @Autowired
    private TmsTaskService tmsTaskService;
    @Autowired
    private FixZeroQuantityTaskV2 fixZeroQuantityTaskV2;

    @Test
    public void shouldGetPayload() {
        taskPropertiesDao.save(fixZeroQuantityTaskV2.getTaskName());
        taskPropertiesDao.setPayload(fixZeroQuantityTaskV2.getTaskName(), STRING_PAYLOAD);

        FixZeroQuantityTaskPayload loadedPayload = taskPropertiesService.getPayload(
                fixZeroQuantityTaskV2.getTaskName(), new TypeReference<>() {
                }, null);

        Assertions.assertEquals(99, loadedPayload.getBatchSize());
        LocalDateTime start = LocalDateTime.of(2022, 1, 1, 10, 11, 12);
        Assertions.assertEquals(start, loadedPayload.getStartDate());
        LocalDateTime end = LocalDateTime.of(2022, 2, 1, 10, 11, 12);
        Assertions.assertEquals(end, loadedPayload.getEndDate());
    }

    @Test
    public void shouldGetPayloadFromTaskInfo() throws SchedulerException {
        taskPropertiesDao.save(fixZeroQuantityTaskV2.getTaskName());
        taskPropertiesDao.setPayload(fixZeroQuantityTaskV2.getTaskName(), STRING_PAYLOAD);

        fixZeroQuantityTaskV2.run(TaskRunType.ONCE);
        var taskInfo = tmsTaskService.getAllTaskFullInfos()
                .stream()
                .filter(job -> fixZeroQuantityTaskV2.getTaskName().equals(job.getTaskName()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(taskInfo);
        FixZeroQuantityTaskPayload payload = (FixZeroQuantityTaskPayload) taskInfo.getParameters().get("payload");
        Assertions.assertNotNull(payload);
        Assertions.assertEquals(99, payload.getBatchSize());
        LocalDateTime start = LocalDateTime.of(2022, 1, 1, 10, 11, 12);
        Assertions.assertEquals(start, payload.getStartDate());
        LocalDateTime end = LocalDateTime.of(2022, 2, 1, 10, 11, 12);
        Assertions.assertEquals(end, payload.getEndDate());
    }
}
