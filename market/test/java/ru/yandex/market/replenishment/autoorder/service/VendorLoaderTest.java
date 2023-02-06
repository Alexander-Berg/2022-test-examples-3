package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.VendorRepository;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.VendorLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class VendorLoaderTest extends FunctionalTest {

    @Autowired
    VendorRepository vendorRepository;

    @Autowired
    VendorLoader vendorLoader;

    @Test
    @DbUnitDataSet(after = "VendorLoaderTest_importVendors.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest",
                    "//home/market/production/mstat/dictionaries/mbo/all_vendors/latest",
                    "//home/market/production/mstat/dictionaries/deepmind/ssku_status/latest"
            },
            csv = "VendorLoaderTest_importVendors.yql.csv",
            yqlMock = "VendorLoaderTest.yql.mock"
    )

    public void importVendors() {
        vendorLoader.load();
    }
}
