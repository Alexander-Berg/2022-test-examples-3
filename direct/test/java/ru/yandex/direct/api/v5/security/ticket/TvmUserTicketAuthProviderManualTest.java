package ru.yandex.direct.api.v5.security.ticket;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthRequest;
import ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthProvider;
import ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthRequest;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.util.HttpUtil;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.core.entity.application.service.ApiAppAccessService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.web.auth.blackbox.BlackboxOauthAuthProvider;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;
import static ru.yandex.direct.tvm.TvmService.DIRECT_API_TEST;

@Api5Test
@RunWith(SpringRunner.class)
@Ignore("For manual run only because tests to use external service")
public class TvmUserTicketAuthProviderManualTest {

    @Autowired
    ApiAppAccessService apiAppAccessService;
    @Autowired
    ApiContextHolder apiContextHolder;
    @Autowired
    private DirectConfig directConfig;
    @Autowired
    private ShardHelper shardHelper;

    private TvmIntegration tvmIntegration;
    private TvmUserTicketAuthProvider tvmUserTicketAuthProvider;
    private DirectApiTokenAuthProvider apiTokenAuthProvider;

    @Before
    public void setUp() {
        tvmIntegration = getTvmIntegration();
        tvmUserTicketAuthProvider = new TvmUserTicketAuthProvider(apiAppAccessService, tvmIntegration, shardHelper,
                directConfig);

        BlackboxClient blackboxClient = getBlackboxClient();
        BlackboxOauthAuthProvider blackboxOauthAuthProvider = new BlackboxOauthAuthProvider(blackboxClient);
        initApiContext();
        apiTokenAuthProvider = new DirectApiTokenAuthProvider(blackboxOauthAuthProvider, null, apiAppAccessService,
                apiContextHolder);
    }

    @Test
    public void authenticate() {
        String operatorLogin = "at-direct-api-test";
        // Токен должен соответствовать логину оператора, по нему получаем из паспорта tvmUserTicket.
        // Получить токен можно по этой инструкции: https://wiki.yandex-team.ru/direct/api/duty/commonproblems/
        String api5AuthToken = "****";
        //Можно получить командой tvmknife get_service_ticket sshkey -s 2009921 -d 2000775
        String tvmServiceTicket = "****";

        DirectApiCredentials credWithToken = createCredentials(api5AuthToken, null, null);
        String tvmUserTicket = getTvmUserTicket(credWithToken);
        DirectApiCredentials credWithTicket = createCredentials(null, tvmServiceTicket, tvmUserTicket);

        DirectApiInternalAuthRequest internalAuthRequest = tvmUserTicketAuthProvider.authenticate(credWithTicket);

        assertThat(internalAuthRequest.getPrincipal()).isEqualTo(operatorLogin);
    }

    private String getTvmUserTicket(DirectApiCredentials credWithToken) {
        DirectApiTokenAuthRequest authRequest = createTokenAuthRequest(credWithToken);
        DirectApiInternalAuthRequest internalAuthRequest = apiTokenAuthProvider.authenticate(authRequest);
        return internalAuthRequest.getTvmUserTicket();
    }

    private DirectApiCredentials createCredentials(String api5AuthToken, String tvmServiceTicket,
                                                   String tvmUserTicket) {
        var request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.111");
        if (tvmUserTicket != null) {
            when(request.getHeader(TvmIntegration.USER_TICKET_HEADER)).thenReturn(tvmUserTicket);
        }
        if (tvmServiceTicket != null) {
            when(request.getHeader(TvmIntegration.SERVICE_TICKET_HEADER)).thenReturn(tvmServiceTicket);
        }
        if (api5AuthToken != null) {
            when(request.getHeaders(HttpUtil.HEADER_AUTHORIZATION)).thenReturn(
                    Collections.enumeration(Collections.singletonList(HttpUtil.BEARER_TOKEN_TYPE + " " + api5AuthToken)));
        }
        when(request.getServerName()).thenReturn("localhost");
        return new DirectApiCredentials(request, null);
    }

    private DirectApiTokenAuthRequest createTokenAuthRequest(DirectApiCredentials credWithToken) {
        String tvmTicketToBlackbox = tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO);
        return new DirectApiTokenAuthRequest(credWithToken, tvmTicketToBlackbox);
    }

    private void initApiContext() {
        ApiContext ctx = new ApiContext();
        currentRequestAttributes().setAttribute(ApiContextHolder.class.getName() + ".CONTEXT", ctx, SCOPE_REQUEST);
    }

    private TvmIntegration getTvmIntegration() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_API_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-api-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.TESTING, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        return TvmIntegrationImpl.create(directConfig, scheduler);
    }

    private BlackboxClient getBlackboxClient() {
        DirectConfig cfg = directConfig.getBranch("blackbox");
        String blackboxUrl = cfg.getString("endpoint");
        Duration blackboxTimeout = cfg.findDuration("timeout").orElse(Duration.ofSeconds(2));
        int blackboxRetries = cfg.findInt("retries").orElse(3);
        final Timeout timeout = Timeout.milliseconds(blackboxTimeout.toMillis());
        final HttpClient httpClient = ApacheHttpClientUtils.multiThreadedClient(timeout, 3);
        BlackboxRawRequestExecutor rawExecuter = new BlackboxRawRequestExecutor(blackboxUrl, httpClient);
        BlackboxRequestExecutorWithRetries executorWithRetries = new BlackboxRequestExecutorWithRetries(rawExecuter,
                blackboxRetries);
        Blackbox2 blackbox2 = new Blackbox2(executorWithRetries);
        return new BlackboxClient(blackbox2);
    }

}
