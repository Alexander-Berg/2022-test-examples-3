package ru.yandex.market.tsum.pipelines.common.jobs.github;

import org.eclipse.egit.github.core.PullRequest;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 25.09.17
 */
public class WaitForGitChecksNotificationTest {
    private final ReleaseInfo releaseInfo = new ReleaseInfo(new FixVersion(12345, "fix_version"), "RELEASE-1");

    private final WaitForGitStatusChecksJob job = new WaitForGitStatusChecksJob().setReleaseInfo(releaseInfo);

    private final PullRequest releasePR = TestPullRequestFactory.pullRequest(1234)
        .setHtmlUrl("www.git.com/market-java/mbo");

    @Test
    public void allRequiredStatusChecksSuccessTest() {
        String telegramMessage = job.allRequiredStatusChecksSuccess(releasePR).getTelegramMessage();
        Assert.assertThat(telegramMessage, Matchers.allOf(
            Matchers.containsString(
                String.format("Релизный [пулл-реквест](%s) успешно прошел все необходимые проверки",
                    releasePR.getHtmlUrl()
                )
            ),
            Matchers.containsString(
                String.format("Релиз [%s](%s), релизный тикет [%s](https://st.yandex-team.ru/%s)",
                    releaseInfo.getFixVersion().getName(),
                    releaseInfo.getReleaseFilterUrl(),
                    releaseInfo.getReleaseIssueKey(), releaseInfo.getReleaseIssueKey()
                )
            )
        ));
    }

    @Test
    public void requiredStatusChecksFailedTest() throws Exception {
        String telegramMessage = job.requiredStatusChecksFailed(releasePR).getTelegramMessage();
        Assert.assertThat(telegramMessage, Matchers.allOf(
            Matchers.containsString(
                String.format("Релизный [пулл-реквест](%s) не прошел все необходимые проверки",
                    releasePR.getHtmlUrl()
                )
            ),
            Matchers.containsString(
                String.format("Релиз [%s](%s), релизный тикет [%s](https://st.yandex-team.ru/%s)",
                    releaseInfo.getFixVersion().getName(),
                    releaseInfo.getReleaseFilterUrl(),
                    releaseInfo.getReleaseIssueKey(), releaseInfo.getReleaseIssueKey()
                )
            )
        ));
    }
}
