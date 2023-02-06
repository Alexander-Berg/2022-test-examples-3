package ru.yandex.market.tpl.billing.config;

import javax.validation.Validator;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tpl.billing.service.HealthService;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.yt.YtService;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.util.EnumConverter;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.yadoc.YaDocClient;

@ContextConfiguration
@Import({
        ClockConfig.class,
        TestDatabaseConfig.class,
        DbQueueConfiguration.class,
        DbUnitConfig.class,
        ExcelSerializerConfig.class,
        JacksonConfig.class,
        PvzDetailedReportConfig.class,
        YtExportConfig.class,
        ExportTransactionsConfig.class
})
@MockBean({
        BillingClient.class,
        PvzClient.class,
        StartrekClient.class,
        StartrekService.class,
        TarifficatorClient.class,
        TariffService.class,
        Yt.class,
        YtOperations.class,
        YtService.class,
        YtTables.class,
        YaDocClient.class,
        HttpGeobase.class,
        TmsMonitoringService.class,
        BillingClient.class
})
@SpyBean({
        HealthService.class
})
public class IntegrationTestConfig {
    @Bean
    Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    EnumConverter enumConverter() {
        return new EnumConverter();
    }

    @Bean
    MockMvc mockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .alwaysDo(MockMvcResultHandlers.print())
                .build();
    }
}
