package ru.yandex.market.tsum.clients.github;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 17.01.18
 */
public class GithubUtilsTest {

    @Test
    public void extractsPR() {
        List<Integer> pullRequests = GithubUtils.extractPullRequest("#1 CSADMIN-21272 Add cs_duty tag (#1241) #2");
        Assert.assertEquals(3, pullRequests.size());
        Assert.assertEquals(1, (int) pullRequests.get(0));
        Assert.assertEquals(1241, (int) pullRequests.get(1));
        Assert.assertEquals(2, (int) pullRequests.get(2));

        pullRequests = GithubUtils.extractPullRequest(" #1523612534000  2#2 если");
        Assert.assertEquals(0, pullRequests.size());
    }

    @Test
    public void getsCommits() {
        List<RepositoryCommit> commits = Arrays.asList(
            new RepositoryCommit().setSha("4"),
            new RepositoryCommit().setSha("3"),
            new RepositoryCommit().setSha("2"),
            new RepositoryCommit().setSha("1")
        );

        List<RepositoryCommit> inclusiveCommits = GithubUtils.getCommits(commits.stream(), "2", "3", true);
        List<RepositoryCommit> exclusiveCommits = GithubUtils.getCommits(commits.stream(), "2", "3", false);

        Assert.assertEquals(2, inclusiveCommits.size());
        Assert.assertEquals("3", inclusiveCommits.get(0).getSha());
        Assert.assertEquals("2", inclusiveCommits.get(1).getSha());

        Assert.assertEquals(1, exclusiveCommits.size());
        Assert.assertEquals("3", exclusiveCommits.get(0).getSha());
    }

    @Test
    public void createsRepoUrlHttps() {
        String url = GithubUtils.createRepoUrl("market/test");
        Assert.assertEquals(GithubUtils.HTTPS_TEMPLATE_URI + "/market/test", url);
    }

    @Test
    public void createsRepoUrlGit() throws URISyntaxException {
        String url = GithubUtils.createRepoUrl("market/test", new URI("git://github.yandex-team.ru"));
        Assert.assertEquals("git://github.yandex-team.ru/market/test", url);
    }

    @Test
    public void createsRepoUrlSsh() throws URISyntaxException {
        String url = GithubUtils.createRepoUrl("market/test", new URI("ssh://git@github.yandex-team.ru"));
        Assert.assertEquals("ssh://git@github.yandex-team.ru/market/test", url);
    }
}
