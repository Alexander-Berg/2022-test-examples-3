package ru.yandex.market.mboc.common.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.config.KeyValueConfig;
import ru.yandex.market.mboc.common.datacamp.HashCalculator;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;

/**
 * @author yuramalinov
 * @created 26.08.2019
 */

@Profile("test")
@TestConfiguration
public class DataCampTestConfig  {

    private final KeyValueConfig keyValueConfig;
    private final CategoryInfoCache categoryInfoCache;

    public DataCampTestConfig(KeyValueConfig keyValueConfig, CategoryInfoCache categoryInfoCache) {
        this.keyValueConfig = keyValueConfig;
        this.categoryInfoCache = categoryInfoCache;
    }

    @Bean
    public DataCampIdentifiersService dataCampIdentifiersService() {
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        return new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, SupplierConverterServiceMock.BERU_BUSINESS_ID,
            supplierConverterService);
    }

    @Bean
    public HashCalculator hashCalculator() {
        return new HashCalculator(keyValueConfig.storageKeyValueService(), dataCampIdentifiersService(),
            categoryInfoCache);
    }

}
