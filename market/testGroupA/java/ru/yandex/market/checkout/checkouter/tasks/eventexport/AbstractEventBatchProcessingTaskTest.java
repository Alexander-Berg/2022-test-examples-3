package ru.yandex.market.checkout.checkouter.tasks.eventexport;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.tasks.AbstractSequentialProcessingTask;
import ru.yandex.market.checkout.common.tasks.ZooTask;

/**
 * @author mmetlov
 */
@ExtendWith(MockitoExtension.class)
public class AbstractEventBatchProcessingTaskTest {

    @Mock
    private Clock clock;

    @Mock
    private ZooTask zooTask;

    private AbstractSequentialProcessingTask alwaysWorkTask = new AbstractSequentialProcessingTask() {
        @Override
        protected int process(ZooTask task) {
            return 1;
        }
    };

    @Test
    public void shouldWork80PercentOfRuntime() {
        Mockito.when(zooTask.getRepeatPeriodMsActual())
                .thenReturn(100L);
        Mockito.when(zooTask.getClock()).thenReturn(clock);

        Mockito.when(clock.millis())
                .thenReturn(0L)
                .thenReturn(50L)
                .thenReturn(80L);

        alwaysWorkTask.run(zooTask, () -> false);

        Mockito.verify(clock, Mockito.times(3)).millis();
    }
}
