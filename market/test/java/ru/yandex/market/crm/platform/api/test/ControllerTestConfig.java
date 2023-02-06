package ru.yandex.market.crm.platform.api.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.platform.api.services.ServicesConfig;
import ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig;
import ru.yandex.market.crm.platform.api.test.kv.MappingTableSetter;
import ru.yandex.market.crm.platform.api.test.net.TestNetConfiguration;
import ru.yandex.market.crm.platform.test.TestAppPlaceholderConfigurer;
import ru.yandex.market.crm.platform.test.utils.CoreTestUtilsConfig;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.test.yt.CoreTestYtConfig;
import ru.yandex.market.crm.platform.yt.BackendSelector;
import ru.yandex.market.crm.platform.yt.KvStorageClient;
import ru.yandex.market.crm.platform.yt.SingleBackendSelectorImpl;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.http.tvm.TvmApplicationDescriptorHolder;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestNetConfiguration.class,
        TestWebContextConfig.class,
        TestConfigRepositoryConfig.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        CoreTestYtConfig.class,
        ServicesConfig.class,
        CoreTestUtilsConfig.class
})
public class ControllerTestConfig {

    @Bean
    public TestAppPlaceholderConfigurer placeholderConfigurer() {
        return new TestAppPlaceholderConfigurer("platform-api_test.properties");
    }

    @Bean
    public ChangelogProvider platformApiChangelogProvider() {
        return () -> "/sql/platform-core-changelog.xml";
    }

    @Bean
    public MockMvc mockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Bean
    public MappingTableSetter mappingTableSetter(YtTables ytTables,
                                                 KvStorageClient kvStorageClient,
                                                 YtSchemaTestUtils schemaTestUtils) {
        return new MappingTableSetter(ytTables, kvStorageClient, schemaTestUtils);
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return () -> Environment.INTEGRATION_TEST;
    }

    @Bean
    public BackendSelector backendSelector(YtClient ytClient) {
        return new SingleBackendSelectorImpl(ytClient);
    }

    @Bean
    public TvmApplicationDescriptorHolder tvmApplicationDescriptorHolder() {
        return mock(TvmApplicationDescriptorHolder.class);
    }

    @Bean
    public TvmService tvmService() {
        return mock(TvmService.class);
    }

    @Bean
    public CommunicationProxyClient communicationProxyClient() {
        return Mockito.mock(CommunicationProxyClient.class);
    }
}
