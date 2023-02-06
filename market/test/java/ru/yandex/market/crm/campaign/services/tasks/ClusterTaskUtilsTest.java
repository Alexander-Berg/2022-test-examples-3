package ru.yandex.market.crm.campaign.services.tasks;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.utils.ClusterTaskUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class ClusterTaskUtilsTest {

    private static class Task1 implements Task<Void, Integer> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, Integer status, Control<Integer> control) {
            return ExecutionResult.completed();
        }
    }

    private static class Task2<T> implements Task<Void, T> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, T status, Control<T> control) {
            return ExecutionResult.completed();
        }
    }

    private interface IntermediateInterface<T> extends Task<Void, T> {

        @Nonnull
        @Override
        default ExecutionResult run(Void context, T status, Control<T> control) {
            return ExecutionResult.completed();
        }
    }

    private static class Task3<T> implements IntermediateInterface<T> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, T status, Control<T> control) {
            return ExecutionResult.completed();
        }
    }

    private interface InterfaceWithoutParams extends Task<Void, String> {

        @Nonnull
        @Override
        default ExecutionResult run(Void context, String status, Control<String> control) {
            return ExecutionResult.completed();
        }
    }

    private static class Task4 implements Task<Void, String> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, String status, Control<String> control) {
            return ExecutionResult.completed();
        }
    }

    @Test
    public void testResolveForDirectRealization() {
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(Task1.class);
        assertEquals(Integer.class, dataClass);
    }

    @Test
    public void testResolveForExtensionOfParameterizedClass() {
        Task<Void, Integer> task = new Task2<>() {};
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(task.getClass());
        assertEquals(Integer.class, dataClass);
    }

    @Test
    public void testResolveThroughIntermediateInterface() {
        Task<Void, Integer> task = new IntermediateInterface<>() {};
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(task.getClass());
        assertEquals(Integer.class, dataClass);
    }

    @Test
    public void testResolveThroughIntermediateClass() {
        Task<Void, Integer> task = new Task3<>() {};
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(task.getClass());
        assertEquals(Integer.class, dataClass);
    }

    @Test
    public void testResolveThroughInterfaceWithoutParams() {
        Task<Void, ?> task = new InterfaceWithoutParams() {};
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(task.getClass());
        assertEquals(String.class, dataClass);
    }

    @Test
    public void testResolveThroughClassWithoutParams() {
        Task<Void, ?> task = new Task4() {};
        Class<?> dataClass = ClusterTaskUtils.getTaskDataClass(task.getClass());
        assertEquals(String.class, dataClass);
    }
}
