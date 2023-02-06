package ru.yandex.market.mboc.common.config;

import lombok.AllArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mboc.db.config.SqlDatasourceConfig;

@TestConfiguration
@Import(
    SqlDatasourceConfig.class
)
@AllArgsConstructor
public class SupplierConverterServiceConfiguration {
    private final SqlDatasourceConfig sqlDatasourceConfig;

    @Bean
    public SupplierConverterService supplierConverterService() {
        return new SupplierConverterServiceImpl(sqlDatasourceConfig.sqlJdbcTemplate(),
            beruIdMock(), SupplierConverterService.TableName.CATEGORY_SUPPLIER);
    }

    @Bean
    public BeruId beruIdMock() {
        return new BeruIdMock(465852);
    }
}
