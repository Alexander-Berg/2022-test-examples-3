package ru.yandex.market.replenishment.autoorder.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentIncrementalLoader;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;

import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.UPDATE;

public class AssortmentIncrementalLoaderTest extends FunctionalTest {

    @Autowired
    private AssortmentIncrementalLoader assortmentIncrementalLoader;

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
        yqlMock = "AssortmentIncrementalLoaderTest_testLoading.yql.mock"
    )
    @DbUnitDataSet(
        before = "AssortmentIncrementalLoaderTest_testLoading.before.csv",
        after = "AssortmentIncrementalLoaderTest_testLoading.after.csv")
    public void testLoading() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                assortmentIncrementalLoader.load(),
            1,
            r -> AuditTestingHelper
                .assertAuditRecord(r.get(0), "appendable_table_timestamps", UPDATE,
                    "last_updated", "2022-03-21T08:56:54.397")
        );
    }
}
