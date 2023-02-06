package ru.yandex.market.crm.platform;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.platform.config.ClusterGroup;
import ru.yandex.market.crm.platform.services.config.ConfigRepositoryImpl;
import ru.yandex.market.crm.platform.services.config.ConfigurationInitializerImpl;
import ru.yandex.market.crm.platform.services.config.PlatformConfiguration;
import ru.yandex.market.crm.platform.yt.YtClusters;
import ru.yandex.market.crm.platform.yt.YtClusters.YtCluster;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ConfigurationTest.Config.class)
public class ConfigurationTest {

    @Configuration
    @Import(PlatformConfiguration.class)
    static class Config {

        @Bean
        public EnvironmentResolver environmentResolver() {
            return new TestEnvironmentResolver();
        }

        @Bean
        public YtClusters ytClusters() {
            return new YtClusters(
                    new YtCluster("markov"),
                    Set.of(
                            new YtCluster("vla", "arnold", ClusterGroup.OFFLINE),
                            new YtCluster("sas", "hahn", ClusterGroup.OFFLINE),
                            new YtCluster("man", "seneca-man", ClusterGroup.ONLINE),
                            new YtCluster("sas", "seneca-sas", ClusterGroup.ONLINE),
                            new YtCluster("vla", "seneca-vla", ClusterGroup.ONLINE)
                    )
            );
        }

        @Bean
        public CommunicationProxyClient communicationProxyClient() {
            return mock(CommunicationProxyClient.class);
        }

        @Bean
        public PersonalService personalService() {
            return mock(PersonalService.class);
        }
    }

    private static class TestEnvironmentResolver implements EnvironmentResolver {

        private Environment environment = Environment.PRODUCTION;

        @Nonnull
        @Override
        public Environment get() {
            return environment;
        }

        void set(Environment environment) {
            this.environment = environment;
        }
    }

    @Inject
    private TestEnvironmentResolver environmentResolver;

    @Inject
    private ConfigurationInitializerImpl factsInitializer;

    /**
     * Проверяем, что конфигурация приложения валидна.
     *
     * <p>Ожидаем отсутствия исключения инициализации конфигурации.</p>
     */
    @Test
    public void testProductionConfig() {
        environmentResolver.set(Environment.PRODUCTION);
        new ConfigRepositoryImpl(factsInitializer);
    }

    @Test
    public void testTestingConfig() {
        environmentResolver.set(Environment.TESTING);
        new ConfigRepositoryImpl(factsInitializer);
    }
}
