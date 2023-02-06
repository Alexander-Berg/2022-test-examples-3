package ru.yandex.market.logshatter.reader.logbroker2.topic;

import org.junit.Test;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.meta.LogshatterMetaDao;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.PartitionDao;
import ru.yandex.market.logshatter.reader.logbroker.monitoring.LogBrokerSourcesWithoutMetadataMonitoring;
import ru.yandex.market.logshatter.reader.logbroker2.TestScheduledExecutorService;
import ru.yandex.market.logshatter.reader.logbroker2.TestSingleThreadExecutorServiceFactory;
import ru.yandex.market.logshatter.reader.logbroker2.common.TopicId;

import java.util.Collections;

import static com.google.common.util.concurrent.Service.State.FAILED;
import static com.google.common.util.concurrent.Service.State.NEW;
import static com.google.common.util.concurrent.Service.State.RUNNING;
import static com.google.common.util.concurrent.Service.State.STARTING;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 18.02.2019
 */
public class LbTopicReaderServiceTest {
    private static final TopicId TOPIC_ID = TopicId.fromString("market-health-stable--other");

    private final TestScheduledExecutorService executorService = new TestScheduledExecutorService();
    private final TestSingleThreadExecutorServiceFactory executorServiceFactory =
        new TestSingleThreadExecutorServiceFactory(Collections.singletonList(executorService));

    private final LbApiStreamConsumerFactory streamConsumerFactory = mock(LbApiStreamConsumerFactory.class);

    private final StreamConsumer streamConsumer = mock(StreamConsumer.class);

    private StreamListener streamListener;

    private final LbTopicReaderService sut = new LbTopicReaderServiceFactory(
        executorServiceFactory,
        streamConsumerFactory,
        mock(PartitionDao.class),
        mock(ReadSemaphore.class),
        mock(LogShatterService.class),
        mock(LogBrokerConfigurationService.class),
        mock(LogshatterMetaDao.class),
        mock(LogBrokerSourcesWithoutMetadataMonitoring.class),
        mock(BatchErrorLoggerFactory.class)
    ).create(TOPIC_ID);

    @Test
    public void successfulStartAndStop() {
        mockStreamConsumerFactoryToReturnStreamConsumer();
        mockStreamConsumerStartToRememberStreamListener();

        assertEquals(NEW, sut.state());

        sut.startAsync();
        assertEquals(STARTING, sut.state());
        verify(streamConsumerFactory).createStreamConsumer(same(TOPIC_ID), same(executorService));
        verify(streamConsumer).startConsume(any(StreamListener.class));
        verifyNoMoreInteractions(streamConsumerFactory, streamConsumer);

        logBrokerSendsInit();
        assertEquals(RUNNING, sut.state());

        sut.stopAsync();
        assertEquals(TERMINATED, sut.state());
        verify(streamConsumer).stopConsume();
        verifyNoMoreInteractions(streamConsumerFactory, streamConsumer);
        assertTrue(executorService.isShutdown());
    }

    @Test
    public void createStreamConsumerFails() {
        when(streamConsumerFactory.createStreamConsumer(any(), any())).thenThrow(new RuntimeException());

        sut.startAsync();
        assertEquals(FAILED, sut.state());
        assertTrue(executorService.isShutdown());
    }

    @Test
    public void startConsumeFails() {
        mockStreamConsumerFactoryToReturnStreamConsumer();
        doThrow(new RuntimeException()).when(streamConsumer).startConsume(any());

        sut.startAsync();
        assertEquals(FAILED, sut.state());
        verify(streamConsumer).startConsume(any(StreamListener.class));
        verify(streamConsumer).stopConsume();
        verifyNoMoreInteractions(streamConsumer);
        assertTrue(executorService.isShutdown());
    }

    @Test
    public void stopConsumeFails() {
        mockStreamConsumerFactoryToReturnStreamConsumer();
        mockStreamConsumerStartToRememberStreamListener();
        doThrow(new RuntimeException()).when(streamConsumer).stopConsume();

        assertEquals(NEW, sut.state());

        sut.startAsync();
        assertEquals(STARTING, sut.state());
        verify(streamConsumer).startConsume(any(StreamListener.class));
        verifyNoMoreInteractions(streamConsumer);

        logBrokerSendsInit();
        assertEquals(RUNNING, sut.state());

        sut.stopAsync();
        assertEquals(FAILED, sut.state());
        verify(streamConsumer).stopConsume();
        verifyNoMoreInteractions(streamConsumer);
        assertTrue(executorService.isShutdown());
    }

    @Test
    public void startConsumeAndStopConsumeFail() {
        mockStreamConsumerFactoryToReturnStreamConsumer();
        doThrow(new RuntimeException()).when(streamConsumer).startConsume(any());
        doThrow(new RuntimeException()).when(streamConsumer).stopConsume();

        sut.startAsync();
        assertEquals(FAILED, sut.state());
        verify(streamConsumer).startConsume(any(StreamListener.class));
        verify(streamConsumer).stopConsume();
        verifyNoMoreInteractions(streamConsumer);
        assertTrue(executorService.isShutdown());
    }


    private void mockStreamConsumerFactoryToReturnStreamConsumer() {
        when(streamConsumerFactory.createStreamConsumer(any(), any())).thenReturn(streamConsumer);
    }

    private void mockStreamConsumerStartToRememberStreamListener() {
        doAnswer(invocation -> {
            streamListener = invocation.getArgument(0);
            return null;
        })
            .when(streamConsumer).startConsume(any());
    }

    private void logBrokerSendsInit() {
        streamListener.onInit(new ConsumerInitResponse(""));
    }
}
