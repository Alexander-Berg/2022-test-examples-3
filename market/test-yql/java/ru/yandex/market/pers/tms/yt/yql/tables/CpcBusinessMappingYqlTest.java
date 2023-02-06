package ru.yandex.market.pers.tms.yt.yql.tables;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

public class CpcBusinessMappingYqlTest extends AbstractPersYqlTest {

    @Test
    public void testCpcBusinessMapping() {
        runTest(loadScript("/yql/tables/cpc_business_mapping.sql"),
            "/tables/cpc_business_mapping_exp.json",
            "/tables/cpc_business_mapping.mock");
    }
}
