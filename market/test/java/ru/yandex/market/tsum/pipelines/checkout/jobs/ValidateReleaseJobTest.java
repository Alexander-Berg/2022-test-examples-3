package ru.yandex.market.tsum.pipelines.checkout.jobs;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;

public class ValidateReleaseJobTest {
    @Test
    public void testNotification() {
        JobContext jobContext = Mockito.mock(JobContext.class);
        Mockito.when(jobContext.getJobLaunchDetailsUrl()).thenReturn("http://example.org");
        Mockito.when(jobContext.getPipeLaunchUrl()).thenReturn("http://example.org");

        TelegramNotification notification = new ValidateReleaseJob.ValidateReleaseJobTelegramNotification(
            jobContext, "http://example.org", 1337
        );

        String telegramMessage = notification.getTelegramMessage();
        Assert.assertEquals(
            "[Найдены](http://example.org) тикеты в статусах " +
                "\"Открыт\", \"Код ревью\" или \"В работе\": 1337\n\n[Перейти к пайплайну](http://example.org)" +
                "\n[Перейти к пайплайн задаче](http://example.org)\n",
            telegramMessage
        );
    }
}
