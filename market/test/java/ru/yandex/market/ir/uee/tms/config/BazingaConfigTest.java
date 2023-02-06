package ru.yandex.market.ir.uee.tms.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.commune.bazinga.scheduler.TaskQueue;
import ru.yandex.commune.bazinga.scheduler.TaskQueueName;
import ru.yandex.market.ir.uee.tms.pojos.BusinessTaskType;

import static org.junit.Assert.assertTrue;

public class BazingaConfigTest {

    @Test
    public void testInitializeAllQueues() {
        final List<String> initQueues = new BazingaConfig().getTaskQueues()
                .stream()
                .map(TaskQueue::getName)
                .map(TaskQueueName::getName)
                .collect(Collectors.toList());

        final List<String> notInitQueue = Arrays.stream(BusinessTaskType.values())
                .map(BusinessTaskType::getTaskQueueName)
                .map(TaskQueueName::getName)
                .distinct()
                .filter(name -> !initQueues.contains(name))
                .collect(Collectors.toList());

            assertTrue("Not initialize queues:" + String.join(",", notInitQueue), notInitQueue.isEmpty());

    }
}
