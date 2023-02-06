package ru.yandex.market.gutgin.tms.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.market.gutgin.tms.utils.DummyTvmClient;
import ru.yandex.market.logbroker.LogbrokerInteractionException;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.LogbrokerServiceImpl;
import ru.yandex.market.logbroker.model.GrpcLogbrokerCluster;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.logbroker.producer.PooledSimpleAsyncProducer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Configuration
public class TestManualLogbrokerConfig {
    private static final Logger log = LogManager.getLogger();

    @Value("gutign")
    private String moduleName;

    // changeit. to create test topic see
    // https://logbroker.yandex-team.ru/docs/quickstart
    @Value("/logbroker-playground/egoryastrebov/offers")
    private String offersTopic;

    @Value("logbroker.yandex.net")
    private String logbrokerHost;
    @Value("2135")
    private int logbrokerPort;

    @Value("true")
    private boolean useOauth;
    @Value("<empty>") // changeit.
    private String logbrokerOauthToken;
    @Value("2001059")
    private int logbrokerClientId;

    @Value("3")
    private int maxRetries;
    @Value("true")
    private boolean testOnReturn;
    @Value("10000")
    private long maxWaitMillis;
    @Value("20")
    private int maxTotal;
    @Value("20")
    private int maxIdle;
    @Value("20")
    private int minIdle;

    @Bean
    public LogbrokerClientFactory logbrokerClientFactory() {
        return new LogbrokerClientFactory(new ProxyBalancer(logbrokerHost, logbrokerPort));
    }

    @Bean
    public ExecutorService logbrokerExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("logbroker-reader-%d")
            .setUncaughtExceptionHandler((t, e) ->
                log.error("Thread " + t.getName() + " FAILED with exception:", e))
            .build();

        return Executors.newFixedThreadPool(1, threadFactory);
    }

    @Bean
    public TvmClient tvmClient() {
        return new DummyTvmClient();
    }

    @Bean
    public Supplier<Credentials> logbrokerCredentialsSupplier(TvmClient tvmClient) {
        if (useOauth) {
            return () -> Credentials.oauth(logbrokerOauthToken);
        } else {
            return () -> getTvmCredentials(tvmClient);
        }
    }

    @Bean
    public LogbrokerCluster logbrokerCluster(
        Supplier<Credentials> logbrokerCredentialsSupplier,
        ExecutorService logbrokerExecutorService
    ) {
        return new GrpcLogbrokerCluster(
            logbrokerHost,
            logbrokerPort,
            logbrokerCredentialsSupplier,
            logbrokerExecutorService
        );
    }

    private Credentials getTvmCredentials(TvmClient tvmClient) {
        final String serviceTicketFor = tvmClient.getServiceTicketFor(logbrokerClientId);
        if (serviceTicketFor != null) {
            return Credentials.tvm(serviceTicketFor);
        } else {
            throw new IllegalStateException("No TVM ticket for Logbroker client.");
        }
    }

    @Bean
    public GenericObjectPoolConfig<PooledSimpleAsyncProducer> logbrokerPoolConfig() {
        GenericObjectPoolConfig<PooledSimpleAsyncProducer> poolConfig =
            new GenericObjectPoolConfig<>();
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        return poolConfig;
    }

    @Bean
    public RetryTemplate logbrokerRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxRetries,
            ImmutableMap.of(
                ExecutionException.class, true,
                TimeoutException.class, true,
                LogbrokerInteractionException.class, true,
                InterruptedException.class, false
            )
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    @Bean(destroyMethod = "close")
    public LogbrokerService offerLogbrokerService(
        LogbrokerCluster logbrokerCluster,
        RetryTemplate logbrokerRetryTemplate,
        GenericObjectPoolConfig<PooledSimpleAsyncProducer> logbrokerPoolConfig
    ) {
        return new LogbrokerServiceImpl(
            offersTopic,
            moduleName,
            getEnvironmentType(),
            logbrokerCluster,
            logbrokerPoolConfig,
            logbrokerRetryTemplate
        );
    }

    public EnvironmentType getEnvironmentType() {
        return EnvironmentType.getActive();
    }
}
