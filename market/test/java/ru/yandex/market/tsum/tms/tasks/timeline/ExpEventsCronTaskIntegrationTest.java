package ru.yandex.market.tsum.tms.tasks.timeline;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryIdempotentWithSleepPolicy;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.exp.AbApiClient;
import ru.yandex.market.tsum.core.config.MongoConfig;
import ru.yandex.market.tsum.event.TsumEventApiGrpc;
import ru.yandex.market.tsum.grpc.trace.TraceClientInterceptor;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/11/2016
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource({"classpath:tsum-tms-test.properties"})
@ContextConfiguration(classes = {ExpEventsCronTask.class, MongoConfig.class, ExpEventsCronTaskIntegrationTest.Config.class})
public class ExpEventsCronTaskIntegrationTest {

    @Autowired
    private ExpEventsCronTask expEventsCronTask;

    @Test
    @Ignore
    public void execute() throws Exception {
        expEventsCronTask.execute(null);
    }

    @Configuration
    public static class Config {
        @Value("${tsum.ab.api-url:}")
        private String abApiUrl;

        @Value("${tsum.external-services.retry-count:5}")
        private int retryCount;

        @Value("${tsum.external-services.retry-sleep-millis:5000}")
        private int retrySleepMillis;

        @Value("${tsum.external-services.read-timeout-millis}")
        private int externalServiceReadTimeoutMillis;

        @Value("${tsum.external-services.connect-timeout-millis}")
        private int externalServiceConnectTimeoutMillis;

        @Value("${tsum.api.grpc-url}")
        private String tsumApiGrpcUrl;

        @Bean
        public AbApiClient abApiClient() {
            return new AbApiClient(abApiUrl, "", defaultHttpClientContext());
        }

        @Bean
        public NettyHttpClientContext defaultHttpClientContext() {
            HttpClientConfig config = new HttpClientConfig();
            config.setConnectTimeoutMillis(externalServiceConnectTimeoutMillis);
            config.setReadTimeoutMillis(externalServiceReadTimeoutMillis);
            config.setRetryPolicy(new RetryIdempotentWithSleepPolicy(retryCount, retrySleepMillis));
            return new NettyHttpClientContext(config);
        }

        @Bean(name = "tsumEventApi")
        public TsumEventApiGrpc.TsumEventApiBlockingStub tsumEventApiBlockingApiClient() {
            Channel channel = ManagedChannelBuilder.forTarget(tsumApiGrpcUrl)
                .usePlaintext().intercept(new TraceClientInterceptor(Module.TSUM_API)).build();
            return TsumEventApiGrpc.newBlockingStub(channel);
        }
    }

}
