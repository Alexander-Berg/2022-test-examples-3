package ru.yandex.chemodan.app.queller.test;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.queller.rabbit.RabbitConnectionPojo;
import ru.yandex.chemodan.queller.rabbit.patchedSpringAmqp.BatchMessageListenerContainer;
import ru.yandex.misc.ip.Host;
import ru.yandex.misc.ip.IpPort;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TestsBaseContextConfiguration.class,
})
@TestExecutionListeners(value = {
        DependencyInjectionTestExecutionListener.class,
})
public class RabbitPoolTest {
    private static final Logger logger = LoggerFactory.getLogger(RabbitPoolTest.class);

    private static final Duration SEND_TIMEOUT = Duration.standardSeconds(5);

    private static final String Q1_NAME = "performance_test_q1";
    private static final String Q2_NAME = "performance_test_q2";

    private int prefetchCount;

    private final RabbitConnectionPojo connectionData;
    private CachingConnectionFactory factory;
    private RabbitAdmin admin;

    private BatchMessageListenerContainer listener;

    private volatile String source;
    private volatile String destination;

    private ListF<Boolean> batchSendConfirmed(String exchange, String queue, ListF<Message> messages,
            Duration timeout) throws
            RuntimeException
    {
        Instant start = Instant.now();

        RabbitTemplate template = new RabbitTemplate(factory);
        MapF<String, Boolean> confirmations = Cf.concurrentHashMap();

        template.setConfirmCallback(
                (correlationData, ack, cause) -> confirmations.put(correlationData.getId(), ack)
        );

        ListF<String> sent = messages.map(message -> {
            String correlationId = UUID.randomUUID().toString();
            template.convertAndSend(exchange, queue, message, new CorrelationData(correlationId));
            return correlationId;
        });

        logger.info("Sending messages in {}", new Duration(start, Instant.now()));

        sent.forEach(correlationId -> {
            while (!confirmations.containsKeyTs(correlationId)) {
                if (Instant.now().isAfter(start.plus(timeout))) {
                    logger.warn("confirmation failed due to timeout");
                    throw new IllegalStateException("message not confirmed: timeout");
                }
            }
            if (!confirmations.removeTs(correlationId)) {
                throw new IllegalStateException("message nacked");
            }
        });
        logger.info("Confirmed sending messages in {}", new Duration(start, Instant.now()));

        return messages.map(m -> true);
    }

    public RabbitPoolTest()
    {
        connectionData = new RabbitConnectionPojo(
                Host.parse("bazinga04e.media.dev.yandex.net"),
                IpPort.cons(5672),
                "disk_test_queller",
                "disk_test_queller",
                "eeTh5ohtho",
                Duration.standardSeconds(5),
                Duration.standardSeconds(5),
                Duration.standardSeconds(5),
                Duration.standardSeconds(5),
                Duration.millis(10),
                Duration.standardSeconds(1),
                Duration.standardSeconds(1),
                Duration.standardSeconds(1),
                1
        );

        source = Q1_NAME;
        destination = Q2_NAME;
    }

    private void setupConnections() {
        factory = new CachingConnectionFactory(connectionData.host.toString(), connectionData.port.getPort());

        factory.setUsername(connectionData.username);
        factory.setPassword(connectionData.password);
        factory.setVirtualHost(connectionData.virtualHost);

        factory.setPublisherConfirms(true);

        try {
            factory.afterPropertiesSet();
        } catch (Exception e) {
            logger.error("Connection factory for {} configuration failed: {}", connectionData.host, e);
        }

        admin = new RabbitAdmin(factory);

        admin.afterPropertiesSet();

        Queue queue1 = new Queue(Q1_NAME);
        Queue queue2 = new Queue(Q2_NAME);

        admin.declareQueue(queue1);
        admin.declareQueue(queue2);

        listener = new BatchMessageListenerContainer(factory, new Object());
    }

    @Test
    public void confirmationPerformanceTest() throws InterruptedException {
        try {
            setupConnections();
        } catch (Exception e) {
            logger.warn("Could not connect to rabbit {}", e);
            return;
        }
        prefetchCount = 100;
        listener.setPrefetchCount(prefetchCount);
        listener.setTxSize(prefetchCount);
        listener.setExclusive(false);
        listener.setConcurrentConsumers(10);
        listener.setMaxConcurrentConsumers(10);
        listener.setAcknowledgeMode(AcknowledgeMode.AUTO);
        listener.setBatchMessageListener(messages -> batchSendConfirmed("", destination, messages, SEND_TIMEOUT));

        int q1StartCount = (Integer) admin.getQueueProperties(Q1_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        int q2StartCount = (Integer) admin.getQueueProperties(Q2_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        boolean q1ToQ2 = false;
        if (q1ToQ2) {
            source = Q1_NAME;
            destination = Q2_NAME;
        } else {
            source = Q2_NAME;
            destination = Q1_NAME;
        }

        listener.setQueues(new Queue(source));

        listener.start();
        Thread.sleep(10000);
        listener.stop();
        Thread.sleep(1000);

        int q1StopCount = (Integer) admin.getQueueProperties(Q1_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        int q2StopCount = (Integer) admin.getQueueProperties(Q2_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        logger.info("Transition result: Q1 {}, Q2 {}", q1StopCount - q1StartCount, q2StopCount - q2StartCount);
    }

    @Test
    public void partialAckTest() throws InterruptedException {
        try {
            setupConnections();
        } catch (Exception e) {
            logger.warn("Could not connect to rabbit {}", e);
            return;
        }
        prefetchCount = 120;
        int ackCoeff = 4;

        listener.setPrefetchCount(prefetchCount);
        listener.setTxSize(prefetchCount);
        listener.setExclusive(false);
        listener.setConcurrentConsumers(1);
        listener.setMaxConcurrentConsumers(1);
        listener.setQueues(new Queue(Q1_NAME));

        listener.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        listener.setBatchMessageListener(messages -> {
            ListF<Long> tags = messages.map(m -> m.getMessageProperties().getDeliveryTag());
            return tags.map(t -> t % ackCoeff == 0);
        });

        int q1StartCount = (Integer) admin.getQueueProperties(Q1_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        if (q1StartCount < prefetchCount) {
            logger.warn("Not enough messages in queue");
            return;
        }

        listener.start();
        listener.stop();

        Thread.sleep(2000);

        int q1StopCount = (Integer) admin.getQueueProperties(Q1_NAME).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        logger.info("Partial ack result: {}", q1StopCount - q1StartCount);

        Assert.equals(-prefetchCount / ackCoeff, q1StopCount - q1StartCount);
    }
}
