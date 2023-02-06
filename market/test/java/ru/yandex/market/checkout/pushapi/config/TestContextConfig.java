package ru.yandex.market.checkout.pushapi.config;

import java.net.URL;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.mocks.MockMvcFactory;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.TestHelper;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.service.shop.settings.CachedWrapperFactory;
import ru.yandex.market.checkout.pushapi.service.shop.settings.ExternalSettingsService;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.misc.lang.StringUtils;

import static org.mockito.Mockito.mock;

@ComponentScan(basePackages = {
        "ru.yandex.market.checkout.pushapi.helpers",
        "ru.yandex.market.checkout.pushapi.mybatis",
        "ru.yandex.market.checkout.util"
}, includeFilters = @ComponentScan.Filter(TestHelper.class))
@Configuration
public class TestContextConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertyConfigurer(
    ) {
        PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertyPlaceholderConfigurer.setIgnoreResourceNotFound(true);
        propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        return propertyPlaceholderConfigurer;
    }

    @Bean
    public MockMvcFactory mockMvcFactory() {
        return new MockMvcFactory();
    }

    @Bean
    public MockMvc mockMvc(MockMvcFactory mockMvcFactory) {
        return mockMvcFactory.getMockMvc();
    }

    @Bean
    public SettingsProvider settingsProvider(@Value("http://localhost:#{shopadminStubMock.port()}") String prefix) {
        return new SettingsProvider(prefix);
    }

    @Bean
    public URL warehousesMappingUrl() {
        return getClass().getResource("/files/pushapi/business_warehouse_info.json");
    }

    @Bean
    public CachedWrapperFactory cachedWrapperFactory(ExternalSettingsService externalSettingsService) {
        var factory = mock(CachedWrapperFactory.class);
        Mockito.when(factory.wrap(Mockito.any(SettingsService.class))).thenReturn(externalSettingsService);
        return factory;
    }

    @Bean
    public PersonalMarketService mockPersonalMarketService() {
        return mock(PersonalMarketService.class);
    }

    @Bean
    public Supplier<String> outletLoaderSelector(Environment environment) {
        return () -> StringUtils.trimToEmpty(environment.getProperty("outlet.cache.loader"));
    }

}
