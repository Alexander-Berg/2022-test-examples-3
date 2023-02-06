package ru.yandex.market.core.config;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.core.feed.assortment.config.AssortmentFeedConfig;
import ru.yandex.market.core.feed.supplier.config.SupplierFeedBaseConfig;
import ru.yandex.market.core.feed.supplier.config.SupplierFeedConfig;
import ru.yandex.market.core.offer.mapping.MboMappingServiceConfig;
import ru.yandex.market.core.offer.mapping.MboMappingXlsHelperConfig;
import ru.yandex.market.core.supplier.SupplierServiceConfig;
import ru.yandex.market.core.supplier.summary.SupplierSummaryInfoServiceConfig;

@ParametersAreNonnullByDefault
@Import({
        SupplierServiceConfig.class,
        SupplierFeedBaseConfig.class,
        TestSupplierXlsHelperConfig.class,
        MboMappingServiceConfig.class,
        MboMappingXlsHelperConfig.class,
        SupplierFeedConfig.StandAloneConfig.class,
        SupplierSummaryInfoServiceConfig.class,
        AssortmentFeedConfig.class,
})
@Configuration
public class TestSupplierFeedConfig {
}
