package ru.yandex.market.tsum.core.notify.common;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryIdempotentWithSleepPolicy;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;
import ru.yandex.market.tsum.core.auth.TsumUserDao;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.clients.notifications.email.EmailClient;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotification;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotificationTarget;
import ru.yandex.market.tsum.core.notify.common.helloworld.BigTelegramNotification;
import ru.yandex.market.tsum.core.notify.common.helloworld.HelloWorldNotification;
import ru.yandex.market.tsum.core.notify.common.helloworld.HelloWorldNotificationTelegram;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekNotificationTarget;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramClient;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.startrek.client.StartrekClientBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 16.02.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, TsumUserDao.class, TsumDebugRuntimeConfig.class})
public class NotifyTaskTest {
    @Value("${tsum.startrek.token}")
    private String startrekToken;
    @Value("${tsum.telegram-bot.token}")
    private String telegramToken;

    @Autowired
    TsumUserDao tsumUserDao;

    private final String telegramApi = "https://api.telegram.org";
    private final String emailAuthor = "robot-mrk-infra-tst@yandex-team.ru";
    private final String smtpHost = "localhost";

    private static final String API_ENDPOINT = "https://st-api.yandex-team.ru";
    private static final int MAX_CONNECTIONS = 10;
    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    private static final int SOCKET_TIMEOUT_SECONDS = 3;
    private int retryCount = 3;
    private int retrySleepMillis = 5000;

    private EmailClient mockEmailClient = Mockito.mock(EmailClient.class);
    private TelegramClient mockTelegramClient = Mockito.mock(TelegramClient.class);
    private StartrekClient mockStartrekClient = Mockito.mock(StartrekClient.class);
    private EmailNotificationTarget emailNotificationTarget = new EmailNotificationTarget("sid-hugo@yandex-team.ru");
    private TelegramNotificationTarget telegramNotificationTarget = new TelegramNotificationTarget(180349965);
    private StartrekNotificationTarget startrekNotificationTarget = new StartrekNotificationTarget("MARKETINFRATEST-2410");
    private HelloWorldNotificationTelegram telegramNotification = new HelloWorldNotificationTelegram();
    private HelloWorldNotification helloWorldNotification = new HelloWorldNotification();

    @Ignore
    @Test
    public void notifyIntegrationTest() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        config.setRetryPolicy(new RetryIdempotentWithSleepPolicy(retryCount, retrySleepMillis));

        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setClients(
            new EmailClient(emailAuthor, smtpHost),
            new StartrekClient(
                StartrekClientBuilder.newBuilder()
                    .uri(API_ENDPOINT)
                    .maxConnections(MAX_CONNECTIONS)
                    .connectionTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .socketTimeout(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build(startrekToken),
                API_ENDPOINT,
                null
            ),
            new TelegramClient(telegramApi, telegramToken, new NettyHttpClientContext(config), tsumUserDao)
        );

        notifyTask.execute(new NotificationParameters(
                helloWorldNotification,
                Arrays.asList(telegramNotificationTarget, startrekNotificationTarget, emailNotificationTarget)),
            null
        );
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionInNotify() throws Exception {
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setClients(
            mockEmailClient,
            mockStartrekClient,
            mockTelegramClient
        );
        notifyTask.execute(new NotificationParameters(
                telegramNotification,
                Arrays.asList(telegramNotificationTarget, startrekNotificationTarget, emailNotificationTarget)),
            null
        );
    }

    @Test
    public void testBigTelegramNotification() throws Exception {
        TelegramClient mockForBigTelegramClient = Mockito.mock(TelegramClient.class);
        Mockito.doAnswer(Answers.CALLS_REAL_METHODS).when(mockForBigTelegramClient).send(Mockito.any(), Mockito.any());
        Mockito.when(mockForBigTelegramClient.sendMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(Futures.immediateFuture(null));

        BigTelegramNotification bigTelegramNotification = new BigTelegramNotification();

        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setClients(
            mockEmailClient,
            mockStartrekClient,
            mockForBigTelegramClient
        );
        notifyTask.execute(new NotificationParameters(
                bigTelegramNotification,
                Collections.singletonList(telegramNotificationTarget)),
            null
        );

        Mockito.verify(mockForBigTelegramClient, Mockito.times(BigTelegramNotification.MESSAGES_COUNT))
            .sendMessage(bigTelegramNotification.getOneMessage(), telegramNotificationTarget.getTargetValue(), false);

        Mockito.verify(mockForBigTelegramClient, Mockito.times(1))
            .sendMessage(TelegramClient.PARSE_MODE_NOT_USED_MESSAGE, telegramNotificationTarget.getTargetValue(), false);
    }

    @Test
    public void notifyTest() throws Exception {
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setClients(
            mockEmailClient,
            mockStartrekClient,
            mockTelegramClient
        );
        notifyTask.execute(new NotificationParameters(
                helloWorldNotification,
                Arrays.asList(telegramNotificationTarget, startrekNotificationTarget, emailNotificationTarget)),
            null
        );
        ArgumentCaptor<TelegramNotification> telegramCaptor = ArgumentCaptor.forClass(TelegramNotification.class);
        Mockito.verify(mockTelegramClient).send(telegramCaptor.capture(), Mockito.eq(telegramNotificationTarget));
        Assert.assertEquals(
            helloWorldNotification.getTelegramMessage(), telegramCaptor.getValue().getTelegramMessage()
        );
        ArgumentCaptor<StartrekCommentNotification> startrekCaptor = ArgumentCaptor.forClass(StartrekCommentNotification.class);
        Mockito.verify(mockStartrekClient).send(startrekCaptor.capture(), Mockito.eq(startrekNotificationTarget));
        Assert.assertEquals(
            helloWorldNotification.getStartrekComment(), startrekCaptor.getValue().getStartrekComment()
        );
        ArgumentCaptor<EmailNotification> emailCaptor = ArgumentCaptor.forClass(EmailNotification.class);
        Mockito.verify(mockEmailClient).send(emailCaptor.capture(), Mockito.eq(emailNotificationTarget));
        Assert.assertEquals(
            helloWorldNotification.getEmailMessage(), emailCaptor.getValue().getEmailMessage()
        );
        Assert.assertEquals(
            helloWorldNotification.getEmailSubject(), emailCaptor.getValue().getEmailSubject()
        );
    }
}
