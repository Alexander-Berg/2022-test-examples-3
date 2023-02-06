package ru.yandex.direct.jobs.monitoring;


import java.util.Map;

import javax.annotation.Nonnull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigPropertySource;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.env.EnvironmentTypeProvider;
import ru.yandex.direct.jobs.configuration.JobsConfiguration;
import ru.yandex.direct.yav.client.YavClient;
import ru.yandex.direct.yav.client.YavClientStub;
import ru.yandex.grut.client.GrutClient;
import ru.yandex.grut.client.GrutGrpcClient;
import ru.yandex.grut.client.ServiceHolder;
import ru.yandex.grut.client.tracing.TraceCallback;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static org.mockito.Mockito.mock;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;
import static ru.yandex.direct.config.EssentialConfiguration.ENVIRONMENT_TYPE_PROVIDER_BEAN_NAME;
import static ru.yandex.direct.config.EssentialConfiguration.OVERRIDING_CONFIG_BEAN_NAME;
import static ru.yandex.direct.core.configuration.CoreConfiguration.GRUT_CLIENT_FOR_WATCHLOG;

@Configuration
@Import({JobsConfiguration.class})
public class MonitoringTestConfiguration {
    @Bean(ENVIRONMENT_TYPE_PROVIDER_BEAN_NAME)
    @Scope(SCOPE_SINGLETON)
    public EnvironmentTypeProvider mutableEnvironmentTypeProvider() {
        return new MutableEnvironmentTypeProvider();
    }

    @Bean
    public MutableDirectConfigPropertySource propertySource(DirectConfig config) {
        return new MutableDirectConfigPropertySource(config);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer properties(ConfigurableEnvironment env,
                                                           MutableDirectConfigPropertySource directConfigSource) {

        MutablePropertySources propertySources = env.getPropertySources();
        propertySources.addLast(directConfigSource);

        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        pspc.setPropertySources(propertySources);

        return pspc;
    }

    @Bean
    public YavClient yavClient() {
        return new YavClientStub(Map.of());
    }

    @Bean
    @Primary
    public GrutClient grutClient() {
        return new GrutGrpcClient(mock(ServiceHolder.class), mock(TraceCallback.class), mock(MetricRegistry.class));
    }

    @Bean(GRUT_CLIENT_FOR_WATCHLOG)
    public GrutClient grutClientForWatchlog(GrutClient grutClient) {
        return grutClient;
    }

    @Bean(OVERRIDING_CONFIG_BEAN_NAME)
    public Config overridingConfig() {
        return ConfigFactory.parseMap(
                Map.ofEntries(
                        Map.entry("telegram.direct-feature.token", "memory://"),
                        Map.entry("startrek.robot_direct_feature.token_path", "memory://")
                )
        );
    }

    protected static class MutableDirectConfigPropertySource extends DirectConfigPropertySource {
        private DirectConfigPropertySource override;

        private MutableDirectConfigPropertySource(DirectConfig source) {
            super("conf", source);
        }

        @Override
        public Object getProperty(String name) {
            if (override != null) {
                return override.getProperty(name);
            }
            return super.getProperty(name);
        }

        void set(DirectConfigPropertySource override) {
            this.override = override;
        }
    }

    protected static class MutableEnvironmentTypeProvider implements EnvironmentTypeProvider {

        private EnvironmentType environmentType = EnvironmentType.DEVELOPMENT;

        @Override
        @Nonnull
        public EnvironmentType get() {
            return environmentType;
        }

        public void set(EnvironmentType environmentType) {
            this.environmentType = environmentType;
        }
    }
}
