package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SskuInfoFullLoader;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.UPDATE;
@ActiveProfiles("unittest")
public class SskuInfoFullLoaderTest extends FunctionalTest {

    @Autowired
    SskuInfoFullLoader sskuInfoFullLoader;

    @Autowired
    AuditTestingHelper auditTestingHelper;

    @Test
    @DbUnitDataSet(before = "SskuInfoLoaderTest_importSskuInfos.before.csv",
            after = "SskuInfoLoaderTest_importSskuInfos.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/latest",
                    "//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest",
                    "//home/market/production/mbo/mboc/offer-mapping",
                    "//home/market/production/mstat/dictionaries/mdm/master_data/latest",
                    "//home/market/production/mdm/dictionaries/reference_item/1d/latest",
                    "//home/market/production/deepmind/dictionaries/ssku_status/latest",
            },
            csv = "SskuInfoLoaderTest_importSskuInfos.yql.csv",
            yqlMock = "SskuInfoFullLoaderTest_simple.yql.mock"
    )
    public void importSskuInfos() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                        sskuInfoFullLoader.load(),
                1,
                r -> AuditTestingHelper.assertAuditRecord(r.get(0), "appendable_table_timestamps", UPDATE,
                        "last_updated", "2021-03-04T18:08:34"
                )
        );
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/latest",
                    "//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest",
                    "//home/market/production/mbo/mboc/offer-mapping",
                    "//home/market/production/mstat/dictionaries/mdm/master_data/latest",
                    "//home/market/production/mdm/dictionaries/reference_item/1d/latest",
                    "//home/market/production/deepmind/dictionaries/ssku_status/latest",
            },
            csv = "SskuInfoLoaderTest_importSskuInfos.yql.csv",
            yqlMock = "SskuInfoFullLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "SskuInfoLoaderTest_testExpanding.before.csv",
            after = "SskuInfoLoaderTest_testExpanding.after.csv")
    public void testExpanding() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                        sskuInfoFullLoader.load(),
                1,
                r -> AuditTestingHelper.assertAuditRecord(r.get(0), "appendable_table_timestamps", UPDATE,
                        "last_updated", "2021-03-04T18:08:34"
                )
        );
    }
}
