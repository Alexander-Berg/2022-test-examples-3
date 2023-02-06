package ru.yandex.market.tsum.core.clients.github;

import com.google.gson.Gson;
import org.eclipse.egit.github.core.PullRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class,})
public class GitHubClientIntegrationTest {
    @Value("${tsum.github.token}")
    private String gitHubToken;

    @Value("${tsum.github.host}")
    private String gitHubHost;

    private GitHubClient client;

    @Before
    public void setUp() throws Exception {
        client = new GitHubClient(gitHubHost, gitHubToken);
    }

    @Test
    public void testCreateTag() {
        client.createTag(
            "market-infra/test-pipeline", "client_test_6", "de21894ddb99eac71d4e310d31db82f6767c2dac"
        );
    }

    @Test
    public void getPullRequest() {
        PullRequest pullRequest = client.getPullRequest("market-infra/market-health", 4701);
        System.out.println(pullRequest.getHtmlUrl());
        System.out.println(new Gson().toJson(pullRequest));
    }
}
