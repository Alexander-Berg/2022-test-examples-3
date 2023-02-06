package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentIncrementalLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        AssortmentIncrementalLoader.class
})
public class AssortmentIncrementalLoaderQueryTest extends YqlQueryTest {

    @Autowired
    private AssortmentIncrementalLoader assortmentIncrementalLoader;

    @Test
    public void testQuery() {
        assertEquals(
                TestUtils.readResource("/queries/expected_assortment_incremental.yt.sql"),
                assortmentIncrementalLoader.getQuery(123L, 1000));
    }

}
