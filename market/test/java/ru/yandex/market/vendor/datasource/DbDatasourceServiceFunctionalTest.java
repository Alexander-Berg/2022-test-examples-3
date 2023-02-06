package ru.yandex.market.vendor.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.datasource.DatasourceService;
import ru.yandex.cs.billing.datasource.model.Datasource;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;

/**
 * @author fbokovikov
 */
class DbDatasourceServiceFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private DatasourceService datasourceService;

    @Test
    void createDatasource() {
        Datasource created = datasourceService.createDatasource(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, "Test datasource", "Test notes");
        Datasource found = datasourceService.getDatasource(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, 1L);
        Assertions.assertEquals(
                created,
                found
        );
    }
}
