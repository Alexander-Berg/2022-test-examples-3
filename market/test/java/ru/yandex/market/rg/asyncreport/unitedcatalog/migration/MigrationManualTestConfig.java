package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.category.CategoryService;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.feed.supplier.tanker.SupplierTankerService;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.indexer.parser.FeedLogJsonDeserializer;
import ru.yandex.market.mbi.environment.ApplicationContextEnvironmentService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.rg.asyncreport.assortment.AssortmentReportWriteService;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@Configuration
public class MigrationManualTestConfig {
    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }

    @Bean
    JdbcTemplate jdbcTemplate() {
        return mock(JdbcTemplate.class);
    }

    @Bean
    AssortmentReportWriteService assortmentReportWriteService() {
        return mock(AssortmentReportWriteService.class);
    }

    @Bean
    CategoryService categoryService() {
        return mock(CategoryService.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    MemCachingService memCachingService() {
        return mock(MemCachingService.class);
    }

    @Bean
    EnvironmentService environmentService(AbstractBeanFactory bf) {
        return new ApplicationContextEnvironmentService(bf);
    }

    @Bean
    FeedLogJsonDeserializer feedLogJsonDeserializer() {
        return new FeedLogJsonDeserializer();
    }

    @Bean
    UnitedCatalogEnvironmentService unitedCatalogEnvironmentService() {
        return new UnitedCatalogEnvironmentService(environmentService(null));
    }

    @Bean
    ExecutorService asyncMboMappingServiceThreadPool() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    SupplierTankerService supplierTankerService() {
        return mock(SupplierTankerService.class);
    }


    @Bean
    ReportsMdsStorage<?> reportsMdsStorage() {
        return mock(ReportsMdsStorage.class);
    }

    @Bean
    ReportsService<?> reportsService() {
        return mock(ReportsService.class);
    }

    @Bean
    Module sourceModule() {
        return Module.MBI_REPORT_GENERATOR;
    }
}
