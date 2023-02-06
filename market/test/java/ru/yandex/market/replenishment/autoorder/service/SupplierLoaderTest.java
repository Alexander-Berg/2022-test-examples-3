package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SupplierLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class SupplierLoaderTest extends FunctionalTest {

    @Autowired
    SupplierLoader supplierLoader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
                    "//home/market/production/mstat/dictionaries/suppliers/latest"
            },
            csv = "SupplierLoaderTest_importSuppliers.yql.csv",
            yqlMock = "SupplierLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "SupplierLoaderTest_importSuppliers.before.csv",
            after = "SupplierLoaderTest_importSuppliers.after.csv")
    public void importSuppliers() {
        supplierLoader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
                    "//home/market/production/mstat/dictionaries/suppliers/latest"
            },
            csv = "SupplierLoaderTest_importSuppliers.yql.csv",
            yqlMock = "SupplierLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "SupplierLoaderTest_importSuppliersWithFake3p.before.csv",
            after = "SupplierLoaderTest_importSuppliersWithFake3p.after.csv")
    public void importSuppliersWithFake3p() {
        supplierLoader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
                    "//home/market/production/mstat/dictionaries/suppliers/latest"
            },
            csv = "SupplierLoaderTest_importSuppliers.yql.csv",
            yqlMock = "SupplierLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "SupplierLoaderTest_testExpanding.before.csv",
            after = "SupplierLoaderTest_testExpanding.after.csv")
    public void testExpanding() {
        supplierLoader.load();
    }
}
