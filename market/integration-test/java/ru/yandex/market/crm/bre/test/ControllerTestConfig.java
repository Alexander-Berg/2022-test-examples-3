package ru.yandex.market.crm.bre.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.crm.bre.http.WebContextConfig;
import ru.yandex.market.crm.bre.services.ServicesConfig;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.suppliers.TestSubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.test.CoreTestSuppliersConfig;
import ru.yandex.market.crm.core.test.LocalYtConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.core.test.TestNetConfig;
import ru.yandex.market.crm.core.test.utils.CoreTestUtilsConfig;
import ru.yandex.market.crm.external.blackbox.YandexBlackboxClient;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;

/**
 * @author apershukov
 */
@Configuration
@Import({
        WebContextConfig.class,
        LocalYtConfig.class,
        TestNetConfig.class,
        TestEnvironmentResolver.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        ServicesConfig.class,
        CoreTestSuppliersConfig.class,
        CoreTestUtilsConfig.class
})
class ControllerTestConfig {

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }

    @Bean
    public SubscriptionsTypesSupplier subscriptionsTypesSupplier() {
        return new TestSubscriptionsTypesSupplier();
    }

    @Bean
    public MockMvc mockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Bean
    public YandexBlackboxClient yandexBlackboxClient() {
        return Mockito.mock(YandexBlackboxClient.class);
    }

    @Bean
    public ChangelogProvider breChangelogProvider() {
        return () -> "/sql/changelog.xml";
    }
}
