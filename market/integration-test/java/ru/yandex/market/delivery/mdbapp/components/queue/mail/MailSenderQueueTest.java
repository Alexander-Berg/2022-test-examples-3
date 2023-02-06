package ru.yandex.market.delivery.mdbapp.components.queue.mail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.email.Mail;
import ru.yandex.market.delivery.mdbapp.components.email.sender.MailSender;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

@TestPropertySource(properties = {
    "mdb.zookeeper.connectionString=#{testZK.connectString}",
    "mdb.poller.shipments.enabled=true",
    "feature.accept-fake-order-events-enabled=false",
    "queue.mailsender.retryInterval=100",
    "queue.mailsender.noTaskTimeout=100",
    "queue.mailsender.betweenTaskTimeout=100",
    "queue.cancel.order.retryInterval=100",
    "queue.cancel.order.noTaskTimeout=100",
    "queue.cancel.order.betweenTaskTimeout=100"
})
public class MailSenderQueueTest extends AllMockContextualTest {

    @Autowired
    private QueueProducer<Mail> producer;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private MailSender sender;

    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    public void mailProducerTest() throws InterruptedException {
        producer.enqueue(EnqueueParams.create(createMail()));
        countDownLatch.await(2, TimeUnit.SECONDS);

        Mockito.verify(sender).send(Mockito.any(Mail.class));
    }

    @Test
    public void whenFirstSendingFailedThenEnqueueAndResend() throws InterruptedException {
        Mockito.doThrow(new RuntimeException("thrown intentionally"))
            .doNothing().when(sender).send(Mockito.any(Mail.class));

        producer.enqueue(EnqueueParams.create(createMail()));
        countDownLatch.await(2, TimeUnit.SECONDS);

        countDownLatch = new CountDownLatch(1);

        Mockito.verify(sender, Mockito.times(1)).send(Mockito.any(Mail.class));

        countDownLatch.await(2, TimeUnit.SECONDS);

        Mockito.verify(sender, Mockito.times(2)).send(Mockito.any(Mail.class));
    }

    private Mail createMail() {
        Mail mail = new Mail();
        mail.setBody("body");
        mail.setFrom("from");
        mail.setTo("to");
        mail.setSubject("subject");
        return mail;
    }
}
