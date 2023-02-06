package ru.yandex.market.tsum.pipelines.sre.rtc.jobs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.ResourceStatistics;

import static ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.calculateResourceStatistics;
import static ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJobComputeStatisticsTest.REQUESTS;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 16/01/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class StartJobTest {
    private static final ResourceStatistics RESOURCE_STATISTICS = calculateResourceStatistics(REQUESTS);

    @InjectMocks
    private CreateRtcServiceSpecJob startJob;

    @Test
    public void getIssueComment_approvalRequired() {
        String comment = startJob.getIssueDescription(
            "service_name",
            "https://tsum.yandex-team.ru/bla-bla",
            true,
            "https://tsum.yandex-team.ru/foo-bar",
            RESOURCE_STATISTICS);
        Assert.assertNotNull(comment);
    }

    @Test
    public void getIssueComment_approvalNotRequired() {
        String comment = startJob.getIssueDescription(
            "service_name",
            "https://tsum.yandex-team.ru/bla-bla",
            false,
            "https://tsum.yandex-team.ru/foo-bar",
            RESOURCE_STATISTICS);
        Assert.assertNotNull(comment);
    }
}
