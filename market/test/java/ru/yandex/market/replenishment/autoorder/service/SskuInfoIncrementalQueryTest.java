package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SskuInfoIncrementalLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        AppendableTableTimestampService.class,
        SskuInfoIncrementalLoader.class
})
public class SskuInfoIncrementalQueryTest extends YqlQueryTest {

    @Autowired
    SskuInfoIncrementalLoader sskuInfoIncrementalLoader;

    @Test
    public void testSskuInfoIncrementalQuery() {
        String query = sskuInfoIncrementalLoader.getQuery(1614841511000L);
        assertEquals(TestUtils.readResource("/queries/expected_ssku_info_incremental.yt.sql"), query);
    }
}
