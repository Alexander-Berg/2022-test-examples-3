package ru.yandex.market.wms.servicebus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.dropping.client.DroppingClient;
import ru.yandex.market.wms.servicebus.configuration.DBClickHouseTestConfig;
import ru.yandex.market.wms.servicebus.configuration.DbTestConfiguration;
import ru.yandex.market.wms.servicebus.configuration.VendorSettingsRepositoryMockConfiguration;
import ru.yandex.market.wms.shippingsorter.client.ShippingsorterClient;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLog;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;
import ru.yandex.market.wms.transportation.client.TransportationClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Import(value = {
        VendorSettingsRepositoryMockConfiguration.class,
        DbTestConfiguration.class,
        DBClickHouseTestConfig.class
})
@Configuration
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.market.wms.servicebus",
        "ru.yandex.market.wms.common.spring.servicebus",
        "ru.yandex.market.wms.common.spring.service.health",
        "ru.yandex.market.wms.common.spring.controller.pagematch",
        "ru.yandex.market.wms.transportation.client",
        "ru.yandex.market.wms.shippingsorter.client",
        "ru.yandex.market.wms.receiving.client",
        "ru.yandex.market.wms.common.converter"
})
public class IntegrationTestConfig {
    @Bean
    public RequestTraceLog requestTraceLog(Module applicationModuleName) {
        return new RequestTraceLogBase(applicationModuleName.toString() + "-trace");
    }

    @Bean
    public TransportationClient transportationClient() {
        return Mockito.mock(TransportationClient.class);
    }

    @Bean
    public ShippingsorterClient shippingsorterClient() {
        return Mockito.mock(ShippingsorterClient.class);
    }

    @Bean
    public DroppingClient droppingClient() {
        return Mockito.mock(DroppingClient.class);
    }

    @Bean({"iris", "trustworthyInfoClient"})
    public TrustworthyInfoClient trustworthyInfoClient() {
        return Mockito.mock(TrustworthyInfoClient.class);
    }

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
    }

    @Bean
    @Primary
    public DbConfigService dbConfigService() {
        final DbConfigService dbConfigService = Mockito.mock(DbConfigService.class);
        when(dbConfigService.getConfig(eq("WAREHOUSE_PREFIX"), any())).thenReturn("99");
        when(dbConfigService.getConfigAsInteger(eq("PUT_REFERENCE_ITEMS_BATCH_SIZE"), any())).thenReturn(500);
        return dbConfigService;
    }
}
