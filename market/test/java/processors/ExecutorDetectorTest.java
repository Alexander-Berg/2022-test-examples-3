package ru.yandex.market.starter.quartz.processors;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.model.PartitionExecutor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorDetectorTest {

    /*
        Copy of private tested method
     */
    private boolean isExecutorOld(Method method) {
        return method.getReturnType().isAssignableFrom(Executor.class)
            || method.getReturnType().isAssignableFrom(PartitionExecutor.class);
    }

    private boolean isExecutorNew(Method method) {
        return Executor.class.isAssignableFrom(method.getReturnType())
            || PartitionExecutor.class.isAssignableFrom(method.getReturnType());
    }

    @Test
    void testExecutorDetection() throws NoSuchMethodException {
        Method executorBean = Config.class.getMethod("abstractExecutorImplementorBeanMethod");
        Method partitionExecutorBean = Config.class.getMethod("abstractPartitionExecutorImplementorBeanMethod");

        assertFalse(isExecutorOld(executorBean));
        assertFalse(isExecutorOld(partitionExecutorBean));

        assertTrue(isExecutorNew(executorBean));
        assertTrue(isExecutorNew(partitionExecutorBean));
    }

    private static class Config {
        public AbstractExecutorImplementor abstractExecutorImplementorBeanMethod() {
            return null;
        }

        public AbstractPartitionExecutorImplementor abstractPartitionExecutorImplementorBeanMethod() {
            return null;
        }
    }

    private static abstract class AbstractExecutorImplementor implements Executor { }

    private static abstract class AbstractPartitionExecutorImplementor implements PartitionExecutor { }

}
