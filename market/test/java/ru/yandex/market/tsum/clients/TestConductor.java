package ru.yandex.market.tsum.clients;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryIdempotentWithSleepPolicy;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.conductor.ConductorLogParser;
import ru.yandex.market.tsum.clients.conductor.detectors.AptGetInstallDetector;
import ru.yandex.market.tsum.clients.conductor.detectors.PServiceDetector;
import ru.yandex.market.tsum.clients.conductor.detectors.ServiceRestartDetector;
import ru.yandex.market.tsum.clients.conductor.detectors.SettingUpDetector;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 08.11.16
 */
@Configuration
@Lazy
@PropertySource("classpath:test.properties")
public class TestConductor {
    @Value("${tsum.conductor.url}")
    private String conductorUrl;
    @Value("${tsum.conductor.oauth-token}")
    private String conductorRobotOAuthToken;

    @Value("${tsum.external-services.retry-count:5}")
    private int retryCount;

    @Value("${tsum.external-services.retry-sleep-millis:5000}")
    private int retrySleepMillis;

    @Bean
    public ConductorClient conductorClient() {
        HttpClientConfig config = new HttpClientConfig();
        config.setRetryPolicy(new RetryIdempotentWithSleepPolicy(retryCount, retrySleepMillis));
        return new ConductorClient(conductorUrl, conductorRobotOAuthToken, new NettyHttpClientContext(config));
    }

    @Bean
    public ConductorLogParser conductorLogParser() {
        return new ConductorLogParser(Arrays.asList(
            new SettingUpDetector(),
            new PServiceDetector(),
            new AptGetInstallDetector(),
            new ServiceRestartDetector()
        ));
    }
}
