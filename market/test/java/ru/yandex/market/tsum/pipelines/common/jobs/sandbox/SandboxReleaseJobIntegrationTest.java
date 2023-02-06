package ru.yandex.market.tsum.pipelines.common.jobs.sandbox;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryAllWithSleepPolicy;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;

@Ignore("integration test")
public class SandboxReleaseJobIntegrationTest {
    int retryCount = 5;
    int retrySleepMillis = 5000;
    int externalServiceReadTimeoutMillis = 10000;
    int externalServiceConnectTimeoutMillis = 10000;
    int externalServiceRequestTimeoutMillis = 10000;
    String sandboxApiUrl = "https://sandbox.yandex-team.ru/api/v1.0";
    String sandboxResourceUrl = "https://proxy.sandbox.yandex-team.ru";

    private static class SandboxReleaseTestJob extends SandboxReleaseJob {
        @Override
        protected Poller.PollerBuilder<SandboxTask> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("33f98653-b8d0-44b9-b45f-c53b3cc92551");
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

    @Test
    public void integrationTest() throws Exception {
        SandboxReleaseJob sut = new SandboxReleaseTestJob();

        sut.sandboxClient = sandboxClient();
        sut.setConfig(
            SandboxReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("MARKET_SRE_TMS_JAVA")
                .build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("MARKET_YA_PACKAGE", 682493650L, "MARKET_SRE_TMS_JAVA"))
        );

        TestJobContext context = new TestJobContext();
        context.setFullJobId(UUID.randomUUID().toString().substring(1, 11));
        sut.execute(context);
    }
}
