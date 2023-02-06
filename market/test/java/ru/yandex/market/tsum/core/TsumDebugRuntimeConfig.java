package ru.yandex.market.tsum.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.arcadia.TrunkArcadiaClient;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.clients.telegraph.TelegraphApiClient;
import ru.yandex.market.tsum.clients.tvm.TvmAuthProvider;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;

import static org.mockito.Mockito.mock;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 11/01/2017
 */
@Configuration
@Lazy
@PropertySource("classpath:test.properties")
@PropertySource(value = {"classpath:99_local-overrides.properties"}, ignoreResourceNotFound = true)
public class TsumDebugRuntimeConfig {

    @Value("${tsum.staff.auth-token}")
    private String staffApiToken;

    @Value("${tsum.startrek.token}")
    private String startrekApiToken;

    @Value("${tsum.telegraph.token}")
    private String telegraphApiToken;

    @Value("${tsum.conductor.url}")
    private String conductorUrl;
    @Value("${tsum.conductor.oauth-token}")
    private String conductorRobotOAuthToken;

    @Value("${tsum.github.token}")
    private String gitHubToken;

    @Value("${tsum.bitbucket.token:}")
    private String bitBucketToken;

    @Value("${tsum.github.host}")
    private String gitHubHost;

    @Value("${tsum.robot.login}")
    private String robotLogin;

    @Value("${tsum.robot.password}")
    private String robotPassword;

    @Value("${tsum.arcadia.url}")
    private String arcadiaUrl;

    @Value("${tsum.arcadia.trunk.url}")
    private String arcadiaTrunkUrl;

    @Value("${tsum.arcanum.url}")
    private String arcanumUrl;

    @Value("${tsum.arcanum.token}")
    private String arcanumToken;

    //ssh -f -N -L 8080:calendar-api.tools.yandex.net:80 blacksmith01h.market.yandex.net
    @Bean
    public CalendarClient calendarClient() {
        return new CalendarClient(
            "http://localhost:8080/internal/",
            "https://calendar.yandex-team.ru",
            mock(TvmAuthProvider.class),
            "localhost",
            "127.0.0.1"
        );
    }

    @Bean
    public StaffApiClient staffApiClient() {
        return new StaffApiClient("https://staff-api.yandex-team.ru/", staffApiToken);
    }

    @Bean
    public TelegraphApiClient telegraphApiClient() {
        return new TelegraphApiClient("https://telegraph.yandex-team.ru/", telegraphApiToken);
    }

    @Bean
    public ConductorClient conductorClient() {
        return new ConductorClient(conductorUrl, conductorRobotOAuthToken);
    }

    @Bean
    public Session startrekSession() {
        return StartrekClientBuilder.newBuilder()
            .uri("https://st-api.yandex-team.ru")
            .build(startrekApiToken);
    }

    @Bean
    public StartrekClient startrekClient() {
        return new StartrekClient(startrekSession(), "https://st-api.yandex-team.ru", startrekApiToken);
    }

    @Bean
    public Issues startrekIssues() {
        return startrekSession().issues();
    }

    @Bean
    public GitHubClient gitHub() {
        return new GitHubClient(gitHubHost, gitHubToken);
    }

    @Bean
    public BitbucketClient bitbucketClient() {
        return new BitbucketClient(
            bitBucketToken, null
        );
    }

    @Bean
    public TrunkArcadiaClient arcadiaClient() {
        return new TrunkArcadiaClient(arcadiaUrl + arcadiaTrunkUrl, arcanumUrl, arcanumToken, svnClientManager(),
            SVNWCUtil.createDefaultAuthenticationManager(robotLogin, robotPassword));
    }

    @Bean
    public RootArcadiaClient rootArcadiaClient() {
        return new RootArcadiaClient(arcadiaUrl, arcanumUrl, arcanumToken, svnClientManager(),
            SVNWCUtil.createDefaultAuthenticationManager(robotLogin, robotPassword));
    }

    private SVNClientManager svnClientManager() {
        return SVNClientManager.newInstance(null,
            SVNWCUtil.createDefaultAuthenticationManager(robotLogin, robotPassword.toCharArray()));
    }
}
