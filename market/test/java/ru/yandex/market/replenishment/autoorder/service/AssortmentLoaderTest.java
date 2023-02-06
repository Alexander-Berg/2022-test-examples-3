package ru.yandex.market.replenishment.autoorder.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentFullLoader;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;

import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.UPDATE;

public class AssortmentLoaderTest extends FunctionalTest {

    @Autowired
    private AssortmentFullLoader assortmentFullLoader;

    @Autowired
    private AuditTestingHelper auditTestingHelper;

    @Autowired
    TableNamesTestQueryService tableNamesTestQueryService;

    @Before
    public void setUp() {
        tableNamesTestQueryService.setNotUseLocalTables(true);
    }

    @After
    public void cleanUp() {
        tableNamesTestQueryService.setNotUseLocalTables(false);
    }

    @Test
    @YqlPrefilledDataTest(
        queries = {
            @YqlPrefilledDataTest.Query(
                suffix = " WHERE a.msku IN (101282719896);",
                name = "assortment.yt.sql"
            )
        },
        yqlMock = "AssortmentLoaderTest_testExpanding.yql.mock"
    )
    @DbUnitDataSet(
        before = "AssortmentLoaderTest_testExpanding.before.csv",
        after = "AssortmentLoaderTest_testExpanding.after.csv")
    public void testExpanding() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                assortmentFullLoader.load(),
            1,
            r -> AuditTestingHelper
                .assertAuditRecord(r.get(0), "appendable_table_timestamps", UPDATE,
                    "last_updated", "2022-03-21T08:56:54.397")
        );
    }

    @Test
    @YqlPrefilledDataTest(
        queries = {
            @YqlPrefilledDataTest.Query(
                suffix = " WHERE a.msku IN (100789385728, 100745679993, 100545143950," +
                    " 14016243, 100548402149, 100545142911, 100490563070, 100367386753," +
                    " 100964635524, 85845068, 100647632183, 144765324, 100900440976," +
                    " 277442059, 143465451, 10061161770, 101288276948," +
                    " 101282719896," +
                    " 101343854606,101326621186,101669472054);",
                name = "assortment.yt.sql"
            )
        },
        yqlMock = "AssortmentLoaderTest_testLoading.yql.mock"
    )
    @DbUnitDataSet(
        before = "AssortmentLoaderTest_testLoading.before.csv",
        after = "AssortmentLoaderTest_testLoading.after.csv")
    public void testLoading() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                assortmentFullLoader.load(),
            1,
            r -> AuditTestingHelper
                .assertAuditRecord(r.get(0), "appendable_table_timestamps", UPDATE,
                    "last_updated", "2022-04-05T11:10:54.732")
        );
    }
}
