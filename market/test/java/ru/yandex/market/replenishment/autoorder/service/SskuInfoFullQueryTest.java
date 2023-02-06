package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SskuInfoFullLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        SskuInfoFullLoader.class
})
public class SskuInfoFullQueryTest extends YqlQueryTest {

    @Autowired
    SskuInfoFullLoader sskuInfoFullLoader;

    @Test
    public void testSskuInfoFullQuery() {
        String query = sskuInfoFullLoader.getQuery();
        assertEquals(TestUtils.readResource("/queries/expected_ssku_info_full.yt.sql"), query);
    }
}
