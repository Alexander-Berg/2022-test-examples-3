package ru.yandex.market.tsum.pipelines.mbi.jobs;

import java.util.Date;
import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.test_data.TestRepositoryCommitFactory.commit;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 05.03.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MbiGenerateTicketsListJobTest {
    @Mock
    private GitHubClient gitHubClient;
    @Mock
    private DeliveryPipelineParams deliveryPipelineParams;

    @Autowired
    private JobTester jobTester;

    @Before
    public void setUp() {
        gitHubClient = mock(GitHubClient.class);
        Iterator<RepositoryCommit> commitIterator = Stream.of(
            commit("5", "", "MBI-5 kirya last", new Date(5)),
            commit("4", "", "MBI-4 kirya", new Date(4)),
            commit("3", "", "MBI-3 test", new Date(3)),
            commit("2", "", "MBI-2 поправить кодировку с KOI на UTF-8", new Date(2)),
            commit("1", "", "MBI-1 test", new Date(1))
        ).iterator();

        when(gitHubClient.getCommitIterator(any(), any())).thenReturn(commitIterator);
    }

    @Test
    public void generatesTicketsList() throws Exception {
        JobExecutor sut = jobTester.jobInstanceBuilder(MbiGenerateTicketsListJob.class)
            .withBean(gitHubClient)
            .withResources(
                MarketTeamcityBuildConfig.builder().withJobName("").build(),
                new DeliveryPipelineParams("2", "1", null),
                new GithubRepo("market-java/mbi")
            )
            .create();

        TestJobContext context = getJobContext();
        sut.execute(context);
        TicketsList tickets = context.getResource(TicketsList.class);
        MatcherAssert.assertThat(
            tickets.getTickets(),
            Matchers.containsInAnyOrder("MBI-2")
        );
    }

    @Test
    public void generatesTicketsListDuringAnotherReleaseIsRunning() throws Exception {
        JobExecutor sut = jobTester.jobInstanceBuilder(MbiGenerateTicketsListJob.class)
            .withBean(gitHubClient)
            .withResources(
                MarketTeamcityBuildConfig.builder().withJobName("").build(),
                new DeliveryPipelineParams("4", "1", "2"),
                new GithubRepo("market-java/mbi")
            )
            .create();

        TestJobContext context = getJobContext();
        sut.execute(context);
        TicketsList tickets = context.getResource(TicketsList.class);
        MatcherAssert.assertThat(
            tickets.getTickets(),
            Matchers.containsInAnyOrder("MBI-3", "MBI-4")
        );
    }

    private TestJobContext getJobContext() {
        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(Mockito.mock(JobState.class));
        return jobContext;
    }
}
