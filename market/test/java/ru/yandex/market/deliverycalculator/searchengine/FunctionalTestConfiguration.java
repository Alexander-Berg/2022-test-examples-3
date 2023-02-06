package ru.yandex.market.deliverycalculator.searchengine;

import java.io.IOException;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.deliverycalculator.searchengine.config.AsyncConfiguration;
import ru.yandex.market.deliverycalculator.searchengine.config.ScheduledTasksConfig;
import ru.yandex.market.deliverycalculator.searchengine.config.SearchEngineServicesConfig;
import ru.yandex.market.deliverycalculator.searchengine.config.SearchEngineTariffWorkflowConfig;
import ru.yandex.market.deliverycalculator.searchengine.config.SearchEngineWebConfig;
import ru.yandex.market.deliverycalculator.searchengine.service.parallel.OfferBucketsServiceSettings;
import ru.yandex.market.deliverycalculator.storage.configs.DeliveryCalculatorStorageTestConfig;
import ru.yandex.market.deliverycalculator.workflow.DeliveryCalculatorWorkflowConfig;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;

import static org.mockito.Mockito.when;

@Import({
        SearchEngineTariffWorkflowConfig.class,
        SearchEngineServicesConfig.class,
        SearchEngineWebConfig.class,
        DeliveryCalculatorWorkflowConfig.class,
        AsyncConfiguration.class,
        DeliveryCalculatorStorageTestConfig.class,
        ScheduledTasksConfig.class,
        SolomonTestJvmConfig.class
})
@Configuration
@EnableWebMvc
public class FunctionalTestConfiguration {

    @Autowired
    private WebApplicationContext applicationContext;

    @Bean
    public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
        final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:application.properties"));

        return ppc;
    }

    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    @Bean
    public MockMvc mockMvc() {
        MockitoAnnotations.initMocks(this);
        return MockMvcBuilders.webAppContextSetup(applicationContext)
                .build();
    }

    @Bean
    @Primary
    public BoilingSolomonService boilingSolomonServiceSpy(final BoilingSolomonService boilingSolomonService) {
        return Mockito.spy(boilingSolomonService);
    }

    @Bean
    @Primary
    public OfferBucketsServiceSettings offerBucketsServiceSettings() {
        OfferBucketsServiceSettings settings = Mockito.mock(OfferBucketsServiceSettings.class);
        when(settings.getPoolSize()).thenReturn(1);
        when(settings.getOffersCountThreshold()).thenReturn(2);
        when(settings.getParallelism()).thenReturn(2);
        return settings;
    }
}
