package ru.yandex.market.tsum.pipelines.common.jobs.github.commit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 22.05.2019
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TsumDebugRuntimeConfig.class)
public class GitHubCommitJobIntegrationTest {
    @Autowired
    private GitHubClient gitHubClient;

    private final String dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss").format(LocalDateTime.now());

    /**
     * Создаёт пулл-реквест и начинает его поллить. Поллинг должен упасть почти сразу потому что в репозитории
     * подключена проверка наличия тикетов в названиях пулл-реквестов. Должен остаться несмёрженный пулл-реквест.
     * <p>
     * Новый пулл-реквест должен появиться здесь:
     * https://github.yandex-team.ru/market-infra/test-repository-for-pr-merge-cron-task/pulls
     */
    @Test(expected = JobManualFailException.class)
    public void testFail() throws Exception {
        run("");
    }

    /**
     * Создаёт пулл-реквест и начинает его поллить. Поллинг должен почти сразу закончиться мёржем.
     * <p>
     * Новый пулл-реквест должен появиться здесь:
     * https://github.yandex-team.ru/market-infra/test-repository-for-pr-merge-cron-task/pulls?q=is%3Apr+is%3Aclosed
     */
    @Test
    public void testSuccess() throws Exception {
        run("MARKETINFRA-0");
    }

    private void run(String trackerIssue) throws TimeoutException, InterruptedException {
        new TestJob(gitHubClient, Poller.Sleeper.DEFAULT, trackerIssue).execute(createJobContextMock());
    }

    private JobContext createJobContextMock() {
        JobContext jobContext = mock(JobContext.class);
        when(jobContext.getFullJobId())
            .thenReturn(GitHubCommitJobIntegrationTest.class.getSimpleName() + "_" + dateTime);
        when(jobContext.getJobLaunchDetailsUrl()).thenReturn("JOB_LAUNCH_DETAILS_URL");
        when(jobContext.progress()).then(Answers.RETURNS_MOCKS);
        return jobContext;
    }


    private class TestJob extends GitHubCommitJob {
        private final String trackerIssue;

        TestJob(GitHubClient gitHubClient, Poller.Sleeper sleeper, String trackerIssue) {
            super(gitHubClient, sleeper);
            this.trackerIssue = trackerIssue;
        }

        @Override
        protected GitHubCommitJobConfig getConfig(JobContext context) {
            return new GitHubCommitJobConfig.Builder()
                .withRepositoryUserOrOrganization("market-infra")
                .withRepositoryName("test-repository-for-pr-merge-cron-task")
                .withBaseBranchName("master")
                .withPullRequestTitle(trackerIssue + " " + getClass().getSimpleName())
                .withMergeTimeoutMinutes(30)
                .withPollingIntervalSeconds(5)
                .build();
        }

        @Override
        protected void commitChanges(JobContext jobContext, CommitContext commitContext) {
            commitContext.addFile(GitHubCommitJobIntegrationTest.class.getSimpleName() + "/" + dateTime, dateTime);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("75be73f1-a57a-4209-a3da-0d3ea097e234");
        }
    }
}
