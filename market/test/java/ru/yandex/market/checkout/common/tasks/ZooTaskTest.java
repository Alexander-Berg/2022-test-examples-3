package ru.yandex.market.checkout.common.tasks;

import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
public class ZooTaskTest {

    @Test
    public void enableAwareQueueConsumerEnabled() throws Exception {
        ZooTask task = new ZooTaskStub(ZooTaskStubState.ENABLE);

        final boolean[] worked = new boolean[1];
        QueueConsumer<Long> queueConsumer = task.buildEnableAwareQueueConsumer(message ->
                worked[0] = true);

        queueConsumer.consumeMessage(123L);

        Assertions.assertTrue(worked[0]);
    }

    @Test
    public void enableAwareQueueConsumerDisabled() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            ZooTask task = new ZooTaskStub(ZooTaskStubState.DISABLE);

            QueueConsumer<Long> queueConsumer = task.buildEnableAwareQueueConsumer(message -> {
            }, 1, 1L, 1L);

            queueConsumer.consumeMessage(123L);
        });
    }

    @Test
    public void enableAwareQueueConsumerException() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            ZooTask task = new ZooTaskStub(ZooTaskStubState.EXCEPTION);

            QueueConsumer<Long> queueConsumer = task.buildEnableAwareQueueConsumer(message -> {
            }, 1, 1L, 1L);

            queueConsumer.consumeMessage(123L);
        });
    }

    private enum ZooTaskStubState {
        ENABLE,
        DISABLE,
        EXCEPTION
    }

    private static final class ZooTaskStub extends ZooTask {

        private final ZooTaskStubState state;

        ZooTaskStub(ZooTaskStubState state) {
            this.state = state;
        }

        @Override
        protected boolean canRun() {
            switch (state) {
                case ENABLE:
                    return true;
                case DISABLE:
                    return false;
                case EXCEPTION:
                default:
                    throw new RuntimeException("Test Error",
                            new KeeperException.SystemErrorException());
            }
        }
    }
}
