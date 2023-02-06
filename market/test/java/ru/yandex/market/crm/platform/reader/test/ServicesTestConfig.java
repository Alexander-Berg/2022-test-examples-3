package ru.yandex.market.crm.platform.reader.test;

import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.platform.config.ClusterGroup;
import ru.yandex.market.crm.platform.reader.config.ConfigurationInitializerImplTest.MapperStub;
import ru.yandex.market.crm.platform.services.config.PlatformConfiguration;
import ru.yandex.market.crm.platform.context.ContextConfig;
import ru.yandex.market.crm.platform.reader.checkouter.CheckouterConfiguration;
import ru.yandex.market.crm.platform.reader.export.yt.SingleColumnMinimalExampleMapper;
import ru.yandex.market.crm.platform.reader.puid.passport.PassportEmailMatchServiceTestConfig;
import ru.yandex.market.crm.platform.reader.services.ServicesConfig;
import ru.yandex.market.crm.platform.reader.services.yt.YtImportService;
import ru.yandex.market.crm.platform.reader.yt.YtClients;
import ru.yandex.market.crm.platform.services.facts.FactsService;
import ru.yandex.market.crm.platform.services.json.SerializationConfiguration;
import ru.yandex.market.crm.platform.test.TestAppPlaceholderConfigurer;
import ru.yandex.market.crm.platform.test.utils.CoreTestUtilsConfig;
import ru.yandex.market.crm.platform.yt.YtClusters;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.TestHttpClientFactory;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmApplicationDescriptorHolder;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.lock.LockServiceConfiguration;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Import({
        TestYtConfig.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        ServicesConfig.class,
        PlatformConfiguration.class,
        ContextConfig.class,
        CheckouterConfiguration.class,
        SerializationConfiguration.class,
        LockServiceConfiguration.class,
        CoreTestUtilsConfig.class,
        PassportEmailMatchServiceTestConfig.class
})
@Configuration
public class ServicesTestConfig {

    @Bean
    public TestAppPlaceholderConfigurer placeholderConfigurer() {
        return new TestAppPlaceholderConfigurer("platform-reader_test.properties");
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return () -> Environment.INTEGRATION_TEST;
    }

    @Bean
    public HttpClientFactory httpClientFactory(HttpEnvironment environment, ConfigurableBeanFactory beanFactory) {
        return new TestHttpClientFactory(environment, beanFactory);
    }

    @Bean
    public HttpEnvironment httpEnvironment() {
        return new HttpEnvironment();
    }

    @Bean("SingleColumnMinimalExampleMapper")
    public SingleColumnMinimalExampleMapper testMapper() {
        return new SingleColumnMinimalExampleMapper();
    }

    @Bean
    public YtImportService ytImportService(YtClients ytClients,
                                           FactsService factsService,
                                           LockService lockService) {
        return new YtImportService(ytClients, factsService, lockService);
    }

    @Bean
    public ChangelogProvider platformReaderChangelogProvider() {
        return () -> "/ru/yandex/market/crm/platform/reader/test-changelog.xml";
    }

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }


    @Bean
    public TvmApplicationDescriptorHolder tvmApplicationDescriptorHolder() {
        return mock(TvmApplicationDescriptorHolder.class);
    }

    @Bean
    public CommunicationProxyClient communicationProxyClient() {
        return Mockito.mock(CommunicationProxyClient.class);
    }

    @Bean
    public YtClusters ytClusters() {
        return new YtClusters(
                new YtClusters.YtCluster("markov"),
                Set.of(
                        new YtClusters.YtCluster("vla", "arnold", ClusterGroup.OFFLINE),
                        new YtClusters.YtCluster("sas", "hahn", ClusterGroup.OFFLINE),
                        new YtClusters.YtCluster("man", "seneca-man", ClusterGroup.ONLINE),
                        new YtClusters.YtCluster("sas", "seneca-sas", ClusterGroup.ONLINE),
                        new YtClusters.YtCluster("vla", "seneca-vla", ClusterGroup.ONLINE)
                )
        );
    }

    @Bean("MapperStub")
    public MapperStub mapperStub() {
        return new MapperStub();
    }
}
