package ru.yandex.market.jmf.queue.retry.internal;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.background.BackgroundService;

@ExtendWith(MockitoExtension.class)
public class RetryTaskInterruptionServiceImplTest {

    private RetryTasksInterruptionServiceImpl interruptionService;

    @Mock
    private BackgroundService backgroundService;

    @Mock
    private RetryTasksQueue retryTasksQueue;

    @Mock
    private RetryTasksPropertiesConfiguration configuration;

    @BeforeEach
    void setUp() {
        Mockito.when(configuration.isRetryTasksInterruptionEnabled())
                .thenReturn(true);

        interruptionService = new RetryTasksInterruptionServiceImpl(10000,
                backgroundService,
                List.of(configuration));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(backgroundService);
        Mockito.clearInvocations(backgroundService);
    }

    @Test
    public void threadShouldBeInterruptedWhenRunningTooLong() throws Exception {
        var threadOne = Mockito.mock(Thread.class);

        var interruptConfig = new RetryTaskInterruptConfiguration(Duration.ofMillis(1));
        interruptionService.markThreadStartsProcessing(threadOne, interruptConfig, retryTasksQueue);
        Thread.sleep(100);
        interruptionService.interruptFreezingProcesses();

        Mockito.verify(backgroundService, Mockito.times(1))
                .stopByName(Mockito.any());
    }

    @Test
    public void threadShouldNotBeInterruptedWhenHasZeroTimeInterruptionConfig() throws Exception {
        var threadOne = Mockito.mock(Thread.class);

        var interruptConfig = RetryTaskInterruptConfiguration.DO_NOT_INTERRUPT_CONFIGURATION;
        interruptionService.markThreadStartsProcessing(threadOne, interruptConfig, retryTasksQueue);
        Thread.sleep(100);
        interruptionService.interruptFreezingProcesses();

        Mockito.verify(backgroundService, Mockito.never())
                .stopByName(Mockito.any());
    }

    @Test
    public void serviceShouldInterruptThreadWithRightName() throws Exception {
        Thread threadToInterrupt = new Thread(() -> {
        });
        Thread threadTwo = new Thread(() -> {
        });
        threadToInterrupt.setName(Randoms.string());
        threadTwo.setName(Randoms.string());

        var interruptOneConfig = new RetryTaskInterruptConfiguration(Duration.ofMillis(1));
        var interruptTwoConfig = RetryTaskInterruptConfiguration.DO_NOT_INTERRUPT_CONFIGURATION;

        interruptionService.markThreadStartsProcessing(threadToInterrupt, interruptOneConfig, retryTasksQueue);
        interruptionService.markThreadStartsProcessing(threadTwo, interruptTwoConfig, retryTasksQueue);
        Thread.sleep(100);
        interruptionService.interruptFreezingProcesses();


        var interruptedThreadNameCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(backgroundService).stopByName(interruptedThreadNameCaptor.capture());
        var captured = interruptedThreadNameCaptor.getAllValues();
        Assertions.assertEquals(1, captured.size());
        Assertions.assertEquals(threadToInterrupt.getName(), captured.get(0));
    }


    @Test
    public void theadShouldNotBeInterruptedWhenTaskCompleted() throws Exception {
        var threadOne = Mockito.mock(Thread.class);

        var interruptConfig = RetryTaskInterruptConfiguration.DO_NOT_INTERRUPT_CONFIGURATION;
        interruptionService.markThreadStartsProcessing(threadOne, interruptConfig, retryTasksQueue);
        Thread.sleep(100);
        interruptionService.markThreadFinishedProcessing(threadOne);
        interruptionService.interruptFreezingProcesses();

        Mockito.verify(backgroundService, Mockito.never())
                .stopByName(Mockito.any());
    }


}
