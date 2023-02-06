package ru.yandex.direct.jobs.configuration;

import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.env.EnvironmentTestProxy;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.hourglass.SchedulerInstancePinger;
import ru.yandex.direct.hourglass.implementations.SimpleSchedulerInstancePinger;

import static ru.yandex.direct.config.EssentialConfiguration.OVERRIDING_CONFIG_BEAN_NAME;

class JobsConfigurationTest {

    private final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

    @BeforeEach
    void init() {
        applicationContext.register(ConfigurationForTest.class);
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentType.class)
    void configIsCreated(EnvironmentType environmentType) {
        System.setProperty("yandex.environment.type", environmentType.name().toLowerCase());
        EnvironmentTestProxy.clearCachedEnv();
        applicationContext.refresh();
    }

    @AfterEach
    void shutdown() {
        EnvironmentTestProxy.clearCachedEnv();
        applicationContext.close();
    }

    @Configuration
    @Import({JobsConfiguration.class})
    public static class ConfigurationForTest {
        // оригинальный бин в postConstruct пишет в базу (что заставляло тесты грустить)
        @Bean
        public SchedulerInstancePinger schedulerInstancePinger() {
            return new SimpleSchedulerInstancePinger();
        }

        @Bean(OVERRIDING_CONFIG_BEAN_NAME)
        public Config overridingConfig() {
            return ConfigFactory.parseMap(
                    Map.ofEntries(
                            Map.entry("db_config", "classpath:///db-config.db_testing.json"),
                            Map.entry("mds.direct_files.token_file_url", "classpath:///mds-auth-testing.txt"),
                            Map.entry("dssclient.user_credentials_file_url", "classpath:dss-user-credentials-dummy" +
                                    ".json"),
                            Map.entry("dssclient.client_credentials_file_url", "classpath:dss-client-credentials" +
                                    "-dummy.json"),
                            Map.entry("sendmail.yservice_salt_file_url", "classpath:sendmail-yservice-salt.txt"),
                            Map.entry("sendmail.hmac_salt_file_url", "classpath:sendmail-hmac-salt.txt"),
                            Map.entry("tvm.enabled", "false"),
                            Map.entry("cloud_iam_default.enabled", "false"),
                            Map.entry("network_config", "classpath:///network-config.allow-all.json"),
                            Map.entry("startrek.robot_ads_audit.token_path", "memory://fake"),
                            Map.entry("canvas.token", "fake"),
                            Map.entry("moderation_service.tvm.enabled", "false"),
                            Map.entry("bannerstorage_api.token", "fake"),
                            Map.entry("redis_cache.name", "redis"),
                            Map.entry("mediascope_client.settings_salt_token_path", "memory://fake"),
                            Map.entry("mediascope_client.client_id_path", "memory://fake"),
                            Map.entry("mediascope_client.client_secret_path", "memory://fake"),
                            Map.entry("job-scheduler-version", "0.0-0"),
                            Map.entry("startrek.robot_direct_daas.token_file", "memory://fake"),
                            Map.entry("ess.tvm.enabled", "false"),
                            Map.entry("mds-s3.token_file_url", ""),
                            Map.entry("scheduler.version_file", "memory://0.0-0"),
                            Map.entry("yav_client.token", "memory://"),
                            Map.entry("telegram.direct-feature.token", "memory://"),
                            Map.entry("startrek.robot_direct_feature.token_path", "memory://"),
                            Map.entry("object_api.service_holder.token", "")
                    )
            );
        }
    }
}
