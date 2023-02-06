package ru.yandex.market.fmcg.bff.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ru.yandex.market.fmcg.bff.connector.BlackboxConnector;
import ru.yandex.market.fmcg.bff.pers.GradeService;
import ru.yandex.market.fmcg.bff.region.GeobaseRegionService;
import ru.yandex.market.fmcg.bff.report.HttpReportSearchService;
import ru.yandex.market.fmcg.bff.report.ReportSearchService;
import ru.yandex.market.fmcg.bff.suggestion.SuggestionService;
import ru.yandex.market.fmcg.bff.tvm.TvmClientWrapper;
import ru.yandex.market.fmcg.bff.util.StringToShopOutletConverter;
import ru.yandex.market.fmcg.client.backend.FmcgBackClient;
import ru.yandex.market.fmcg.core.search.index.IndexService;
import ru.yandex.market.pers.notify.PersNotifyClient;

/**
 * @author semin-serg
 */
@EnableWebMvc
@Configuration
public class FmcgBffMockConfig implements WebMvcConfigurer {
    @Autowired
    private StringToShopOutletConverter stringToShopOutletConverter;

    @Bean
    GradeService gradeService() {
        return FmcgBffMockFactory.gradeService();
    }

    @Bean
    ReportSearchService reportSearchService() {
        HttpReportSearchService result = new HttpReportSearchService("localhost", MockServerUtil.INSTANCE.getPort());
        result.initHttpClient();
        return result;
    }

    @Bean
    SuggestionService suggestionService() {
        return FmcgBffMockFactory.suggestionService();
    }

    @Bean
    FmcgBackClient fmcgBackClient() {
        return FmcgBffMockFactory.fmcgBackClient();
    }

    @Bean
    GeobaseRegionService geobaseRegionService() {
        return FmcgBffMockFactory.geobaseRegionService();
    }

    @Bean
    BlackboxConnector authenticationService() {
        return FmcgBffMockFactory.authenticationService();
    }

    @Bean
    public RestTemplate geoexportRestTemplate() {
        return FmcgBffMockFactory.geoexportRestTemplate();
    }

    @Bean
    public AuthenticationConfiguration authenticationConfiguration() {
        return FmcgBffMockFactory.authenticationConfiguration();
    }

    @Bean
    public MockRestServiceServer shopIntegrationMockServer(
        @Qualifier("shopIntegrationRestTemplate") RestTemplate shopIntegrationRestTemplate
    ) {
        return MockRestServiceServer.createServer(shopIntegrationRestTemplate);
    }

    @Autowired
    private WebApplicationContext wac;

    @Bean
    MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Bean
    public TvmClientWrapper tvmClientWrapper() {
        return new TvmClientWrapper(null, 0);
    }

    @Bean
    public IndexService indexService() {
        return FmcgBffMockFactory.indexService();
    }

    @Bean
    public RestTemplate searchRestTemplate() {
        return FmcgBffMockFactory.restTemplate();
    }

    @Bean
    public PersNotifyClient persNotifyClient() {
        return FmcgBffMockFactory.persNotifyClient();
    }

    //todo Это костыль. Разобраться, почему указанный выше мок не спасает от создания в тестах реального бина
    // persNotifyClient, требующего persNotifyRestTemplate
    @Bean
    public RestTemplate persNotifyRestTemplate() {
        return FmcgBffMockFactory.restTemplate();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToShopOutletConverter);
    }
}
