package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.delivery.dsmclient.payload.Task;

@RunWith(Parameterized.class)
public class TaskTransformerQueueTest {
    @Parameterized.Parameter
    public Task.Queue queue;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{Task.Queue.ORDER_CREATE});
        parameters.add(new Object[]{Task.Queue.ORDER_CANCEL});
        parameters.add(new Object[]{Task.Queue.ORDER_RECREATE});
        parameters.add(new Object[]{Task.Queue.DOC_GET_ATTACHED_DOC});
        parameters.add(new Object[]{Task.Queue.REGISTER_CREATE});

        return parameters;
    }

    @Test
    public void taskTransformerQueueTest() {
        TaskTransformer taskTransformer = new TaskTransformer();
        Task task = taskTransformer.generateTask(new Object(), queue);
        Assert.assertEquals("Unexpected queue after generateTask", task.getQueue(), queue);
    }
}
