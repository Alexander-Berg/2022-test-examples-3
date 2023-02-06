package ru.yandex.market.tsum.pipelines.common.jobs.aqua;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.aqua.beans.Launch;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.misc.test.Assert;

public class AquaLaunchFinishedTelegramNotificationTest {
    @Test
    public void getTelegramComment() throws Exception {
        JobContext jobContext = Mockito.mock(JobContext.class);
        Mockito.when(jobContext.getJobLaunchDetailsUrl()).thenReturn("http://example.org");
        Mockito.when(jobContext.getPipeLaunchUrl()).thenReturn("http://example.org");

        Launch launch = AquaLaunchFinishedCommentTest.createLaunch();
        AquaJob.AquaLaunchFinishedTelegramNotification sut = new AquaJob.AquaLaunchFinishedTelegramNotification(
            launch, jobContext
        );

        String message = sut.getTelegramMessage();

        System.out.println(message);

        Assert.assertContains(message, launch.getLaunchUrl());
        Assert.assertContains(message, launch.getReportRequestUrl());
    }
}
