package ru.yandex.market.tsum.pipelines.common.jobs.start;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.05.17
 */
public class IssuesInReleaseNotificationTest {
    private static final Issue ISSUE_1 = IssueBuilder.newBuilder("TEST-1").build();
    private static final Issue ISSUE_2 = IssueBuilder.newBuilder("TEST-2").build();
    private static final List<Issue> ISSUES = Arrays.asList(ISSUE_1, ISSUE_2);

    @Test
    public void getStartrekComment() throws Exception {
        String comment = new IssuesInReleaseNotification(ISSUES).getStartrekComment();

        Assert.assertNotNull(comment);
        System.out.println(comment);
    }
}
