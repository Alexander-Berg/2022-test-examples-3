package ru.yandex.market.tsum.pipelines.common.jobs.nanny;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryAllWithSleepPolicy;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.NannyTicketApiClient;
import ru.yandex.market.tsum.clients.nanny.UntypedNannyClient;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.rollback.RedeployType;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.08.17
 */
@Ignore("integration test")
public class NannyReleaseJobIntegrationTest {
    int retryCount = 5;
    int retrySleepMillis = 5000;
    int externalServiceReadTimeoutMillis = 10000;
    int externalServiceConnectTimeoutMillis = 10000;
    int externalServiceRequestTimeoutMillis = 10000;
    String sandboxApiUrl = "https://sandbox.yandex-team.ru/api/v1.0";
    String sandboxResourceUrl = "https://proxy.sandbox.yandex-team.ru";
    String nannyApiUrl = "https://nanny.yandex-team.ru/";
    String nannyOAuthToken = "<YOUR TOKEN>";

    private static class NannyReleaseTestJob extends NannyReleaseJob {
        @Override
        protected Poller.PollerBuilder<SandboxTask> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("700016af-cdc1-4773-8c6a-9182e99a9f51");
        }
    }

    public HttpClientConfig defaultHttpClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        config.setConnectTimeoutMillis(externalServiceConnectTimeoutMillis);
        config.setRequestTimeoutMillis(externalServiceRequestTimeoutMillis, TimeUnit.MILLISECONDS);
        config.setReadTimeoutMillis(externalServiceReadTimeoutMillis);
        config.setRetryPolicy(new RetryAllWithSleepPolicy(retryCount, retrySleepMillis));
        return config;
    }

    public NettyHttpClientContext defaultHttpClientContext(HttpClientConfig httpClientConfig) {
        return new NettyHttpClientContext(httpClientConfig);
    }

    public SandboxClient sandboxClient() {
        String sandboxOAuthToken = "<YOUR TOKEN>";
        String login = "<YOUR LOGIN>";
        return new SandboxClient(
            sandboxApiUrl, sandboxResourceUrl, sandboxOAuthToken,
            defaultHttpClientContext(defaultHttpClientConfig()), login
        );
    }

    public NannyTicketApiClient nannyTicketApiClient() {
        return new NannyTicketApiClient(nannyApiUrl, nannyOAuthToken,
            defaultHttpClientContext(defaultHttpClientConfig()));
    }

    @Test
    public void integrationTest() throws Exception {
        NettyHttpClientContext defaultHttpClientContext = defaultHttpClientContext(defaultHttpClientConfig());
        NannyReleaseJob sut = new NannyReleaseTestJob();

        sut.sandboxClient = sandboxClient();
        sut.nannyTicketApiClient = nannyTicketApiClient();
        sut.nannyClient = new NannyClient("https://nanny.yandex-team.ru/", "<YOUR TOKEN>",
            defaultHttpClientContext);
        sut.untypedNannyClient = new UntypedNannyClient("https://nanny.yandex-team.ru/", "<YOUR TOKEN>",
            defaultHttpClientContext);

        sut.setConfig(
            NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("MARKET_SRE_TMS_JAVA")
                .withRedeploy(true)
                .withRedeployType(RedeployType.APP_ONLY)
                .withRecipeId("testing")
                .withDashboardId("market_market_sre_tms")
                .build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");
        sut.setTsumNannyUrl("https://nanny-dev.yandex-team.ru");

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("MARKET_YA_PACKAGE", 682493650L, "MARKET_SRE_TMS_JAVA"))
        );

        TestJobContext context = new TestJobContext();
        context.setFullJobId(UUID.randomUUID().toString().substring(1, 11));
        sut.execute(context);

    }

}
