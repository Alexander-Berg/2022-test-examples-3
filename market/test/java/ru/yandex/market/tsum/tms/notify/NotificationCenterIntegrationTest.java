package ru.yandex.market.tsum.tms.notify;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.TestZooKeeper;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.core.auth.TsumUserDao;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.core.notify.common.NotifyTask;
import ru.yandex.market.tsum.clients.notifications.email.EmailClient;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotification;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotificationTarget;
import ru.yandex.market.tsum.core.notify.common.helloworld.HelloWorldNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekNotificationTarget;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramClient;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.market.tsum.tms.config.BazingaConfig;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 26.02.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource({"classpath:bazinga-test.properties", "classpath:test.properties"})
@ContextConfiguration(classes = {TestZooKeeper.class, TestMongo.class, NotifyTask.class, BazingaConfig.class, NotificationCenterIntegrationTest.ClientsConfig.class})
public class NotificationCenterIntegrationTest {
    @Autowired
    private BazingaTaskManager bazingaTaskManager;

    @Autowired
    private TelegramClient telegramClient;
    @Autowired
    private EmailClient emailClient;
    @Autowired
    private StartrekClient startrekClient;

    @Test
    public void notifyIntegrationTest() throws InterruptedException {
        NotificationCenter notificationCenter = new NotificationCenter(bazingaTaskManager);

        HelloWorldNotification helloWorldNotification = new HelloWorldNotification();
        long chatId = 123456;
        String emailAddress = "user42@yandex-team.ru";
        String startrekTicket = "TESTQUEUE-666";

        FullJobId jobId = notificationCenter.notify(
            helloWorldNotification,
            new TelegramNotificationTarget(chatId),
            new EmailNotificationTarget(emailAddress),
            new StartrekNotificationTarget(startrekTicket)
        );

        for (int i = 0; i < 1000; i++) {
            Thread.sleep(10);
            if (bazingaTaskManager.getOnetimeJob(jobId).get().getValue().getStatus().isCompleted()) {
                ArgumentCaptor<TelegramNotification> telegramNotificationCaptor = ArgumentCaptor.forClass(TelegramNotification.class);
                ArgumentCaptor<TelegramNotificationTarget> telegramTargetCaptor = ArgumentCaptor.forClass(TelegramNotificationTarget.class);
                Mockito.verify(telegramClient).send(telegramNotificationCaptor.capture(), telegramTargetCaptor.capture());

                ArgumentCaptor<EmailNotification> emailNotificationCaptor = ArgumentCaptor.forClass(EmailNotification.class);
                ArgumentCaptor<EmailNotificationTarget> emailTargetCaptor = ArgumentCaptor.forClass(EmailNotificationTarget.class);
                Mockito.verify(emailClient).send(emailNotificationCaptor.capture(), emailTargetCaptor.capture());

                ArgumentCaptor<StartrekCommentNotification> startrekNotificationCaptor = ArgumentCaptor.forClass(StartrekCommentNotification.class);
                ArgumentCaptor<StartrekNotificationTarget> startrekTargetCaptor = ArgumentCaptor.forClass(StartrekNotificationTarget.class);
                Mockito.verify(startrekClient).send(startrekNotificationCaptor.capture(), startrekTargetCaptor.capture());

                Assert.assertEquals(helloWorldNotification.getTelegramMessage(), telegramNotificationCaptor.getValue().getTelegramMessage());
                Assert.assertEquals(helloWorldNotification.getEmailMessage(), emailNotificationCaptor.getValue().getEmailMessage());
                Assert.assertEquals(helloWorldNotification.getStartrekComment(), startrekNotificationCaptor.getValue().getStartrekComment());

                Assert.assertEquals(chatId, telegramTargetCaptor.getValue().getChatId());
                Assert.assertEquals(emailAddress, emailTargetCaptor.getValue().getTargetValue());
                Assert.assertEquals(startrekTicket, startrekTargetCaptor.getValue().getTicketId());

                return;
            }
        }
        Assert.fail("Task did not executed");
    }

    @Configuration
    public static class ClientsConfig {
        @Bean
        public TsumUserDao tsumUserDao() {
            return Mockito.mock(TsumUserDao.class);
        }

        @Bean
        public EmailClient emailClient() {
            return Mockito.mock(EmailClient.class);
        }

        @Bean
        public TelegramClient telegramClient() {
            return Mockito.mock(TelegramClient.class);
        }

        @Bean
        public StartrekClient startrekClient() {
            return Mockito.mock(StartrekClient.class);
        }
    }
}
