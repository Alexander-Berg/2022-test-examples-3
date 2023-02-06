package ru.yandex.market.delivery.mdbapp.components.queue.producer;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.delivery.mdbapp.components.logging.OrderLogContext;
import ru.yandex.market.delivery.mdbapp.components.queue.TaskActorTransformer;
import ru.yandex.market.delivery.mdbapp.configuration.queue.QueueInitializationSynchronizer;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.settings.QueueId;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ActorEnrichingQueueProducerDecoratorTest {

    private static final QueueId QUEUE_ID = new QueueId("queue");

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    private PayloadTransactionalProducer<String> payloadTransactionalProducer;

    @Captor
    private ArgumentCaptor<EnqueueParams<String>> enqueueParamsArgumentCaptor;

    private ActorEnrichingQueueProducerDecorator<String> actorEnrichingQueueProducerDecorator;

    @Parameterized.Parameter
    public String logEventId;

    @Parameterized.Parameter(1)
    public String logOrderId;

    @Parameterized.Parameter(2)
    public String rawActor;

    @Parameterized.Parameter(3)
    public String expectedRawActor;

    @Before
    public void setUp() {
        when(payloadTransactionalProducer.getQueueId())
            .thenReturn(QUEUE_ID);
        when(payloadTransactionalProducer.getPayloadClass())
            .thenReturn(String.class);

        QueueInitializationSynchronizer synchronizer = new QueueInitializationSynchronizer(1000, true);
        synchronizer.initialized();
        actorEnrichingQueueProducerDecorator = new ActorEnrichingQueueProducerDecorator<>(
            payloadTransactionalProducer,
            new TaskActorTransformer(new ObjectMapper()),
            synchronizer
        );
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {null, null, null, null},
            {"1", "2", null, "{\"eventId\":\"1\",\"orderId\":\"2\"}"},
            {null, null, "TEXT", "{\"payload\":\"TEXT\"}"},
            {"1", "2", "TEXT", "{\"eventId\":\"1\",\"orderId\":\"2\",\"payload\":\"TEXT\"}"},
            {"100", null, "{\"orderId\":\"2\"}", "{\"orderId\":\"2\"}"},
            {"1", "2", "{\"eventId\":\"1\",\"orderId\":\"2\"}", "{\"eventId\":\"1\",\"orderId\":\"2\"}"},
        });
    }

    @Test
    public void enqueue() {
        OrderLogContext.logEventId(logEventId);
        OrderLogContext.logOrderId(logOrderId);

        actorEnrichingQueueProducerDecorator.enqueue(
            EnqueueParams.create("")
                .withActor(rawActor)
        );

        verify(payloadTransactionalProducer).enqueue(enqueueParamsArgumentCaptor.capture());

        softly.assertThat(enqueueParamsArgumentCaptor.getValue().getActor())
            .isEqualTo(expectedRawActor);

        OrderLogContext.clear();
    }
}
