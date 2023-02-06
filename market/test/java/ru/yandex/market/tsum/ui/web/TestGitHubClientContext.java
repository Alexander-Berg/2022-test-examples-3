package ru.yandex.market.tsum.ui.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.tsum.clients.github.GitHubClient;

import static org.mockito.Mockito.mock;

@Configuration
public class TestGitHubClientContext {
    @Bean
    public GitHubClient gitHubClient() {
        return mock(GitHubClient.class);
    }
}
