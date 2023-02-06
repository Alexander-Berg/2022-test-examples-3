package ru.yandex.market.tsum.pipelines.fps;

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;

@RunWith(MockitoJUnitRunner.class)
public class SleepJobTest {
    @Mock
    private TsumJobContext context;
    @Mock
    private SleepJobConfig config;
    @Mock
    private JobProgressContext progressContext;
    @Mock
    private JobProgressContextImpl.ProgressBuilder progressBuilder;

    @InjectMocks
    private SleepJob sleepJob;

    @Test
    public void testThatSleepJobReallyWork() throws Exception {
        // When

        // Настройка конфигурации
        Mockito.when(config.getSleepDurationSeconds()).thenReturn(10L);
        Mockito.when(config.getInformingDurationSeconds()).thenReturn(2L);

        // Настройка контекста
        Mockito.when(context.progress()).thenReturn(progressContext);
        Mockito.doAnswer(invocation ->
                        ((Function<
                                JobProgressContextImpl.ProgressBuilder,
                                JobProgressContextImpl.ProgressBuilder
                                >) invocation.getArgument(0))
                                .apply(progressBuilder)
                )
                .when(progressContext).update(Mockito.any());

        Mockito.when(progressBuilder.setText(Mockito.anyString())).thenReturn(progressBuilder);
        Mockito.when(progressBuilder.setCanInterrupt(Mockito.anyBoolean())).thenReturn(progressBuilder);

        // Given
        sleepJob.execute(context);

        // Then
        Mockito.verify(progressBuilder, Mockito.times(5)).setText(Mockito.startsWith("Осталось"));
    }
}
