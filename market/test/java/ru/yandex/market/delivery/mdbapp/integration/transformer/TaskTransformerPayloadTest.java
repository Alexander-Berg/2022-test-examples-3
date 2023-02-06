package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.delivery.dsmclient.payload.Task;

@RunWith(Parameterized.class)
public class TaskTransformerPayloadTest {
    @Parameterized.Parameter(0)
    public Object payload;

    @Parameterized.Parameter(1)
    public String caseName;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{null, "null"});
        parameters.add(new Object[]{new Object(), "empty"});
        parameters.add(new Object[]{"qwerty", "String"});
        parameters.add(new Object[]{123, "int"});

        return parameters;
    }

    @Test
    public void taskTransformerPayloadTest() {
        TaskTransformer taskTransformer = new TaskTransformer();
        Task task = taskTransformer.generateTask(payload, Task.Queue.ORDER_CREATE);
        Assert.assertEquals("Unexpected taskData after generateTask", task.getData(), payload);
    }
}
