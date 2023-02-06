package ru.yandex.market.tsum.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 25.01.18
 */
@Configuration
@Lazy
@PropertySource("classpath:test.properties")
public class TestTeamcity {

    @Value("${tsum.teamcity.url}")
    private String teamcityUrl;
    @Value("OAuth ${tsum.teamcity.auth-token}")
    private String token;


    @Bean
    public TeamcityClient teamcityClient() {
        return new TeamcityClient(teamcityUrl, token);
    }
}
