package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentFullLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        AssortmentFullLoader.class
})
public class AssortmentLoaderQueryTest extends YqlQueryTest {

    @Autowired
    private AssortmentFullLoader assortmentFullLoader;

    @Test
    public void testQuery() {
        assertEquals(
                TestUtils.readResource("/queries/expected_assortment_full.yt.sql"),
                assortmentFullLoader.getQuery());
    }

}
