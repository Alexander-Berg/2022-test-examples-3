package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringRunner.class)
public class ChangelogToTicketsJobTest {
    @Autowired
    private JobTester jobTester;

    @Test
    public void shouldNotSkipCherryPicks() throws Exception {
        TestJobContext jobContext = new TestJobContext();

        List<ChangelogEntry> changelogEntries = new ArrayList<>();
        changelogEntries.add(new ChangelogEntry("223456", "cherry-pick 7440469: MARKETCHECKOUT-16107. Тестовый коммит" +
            " для хотфикса."));
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries);

        ChangelogToTicketsJob job = jobTester.jobInstanceBuilder(ChangelogToTicketsJob.class)
            .withResources(changelogInfo)
            .withResource(new StartrekSettings("MARKETCHECKOUT"))
            .create();

        job.execute(jobContext);

        TicketsList ticketsList = jobContext.getResource(TicketsList.class);

        Assert.assertThat(ticketsList.getTickets(), hasSize(1));
    }

    @Test
    public void shouldSkipCommitsToOtherQueues() throws Exception {
        TestJobContext jobContext = new TestJobContext();

        List<ChangelogEntry> changelogEntries = new ArrayList<>();
        changelogEntries.add(new ChangelogEntry("223456", "MARKETSOMETHING-123456"));
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries);

        ChangelogToTicketsJob job = jobTester.jobInstanceBuilder(ChangelogToTicketsJob.class)
            .withResources(changelogInfo)
            .withResource(new StartrekSettings("MARKET"))
            .create();

        job.execute(jobContext);

        TicketsList ticketsList = jobContext.getResource(TicketsList.class);

        Assert.assertThat(ticketsList.getTickets(), empty());
    }

    @Configuration
    @Import(JobTesterConfig.class)
    public static class Config {
    }

}
