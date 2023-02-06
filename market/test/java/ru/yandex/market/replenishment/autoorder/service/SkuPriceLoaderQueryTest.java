package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SkuPriceLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        SkuPriceLoader.class
})
public class SkuPriceLoaderQueryTest extends YqlQueryTest {

    @Autowired
    private SkuPriceLoader skuPriceLoader;

    @Test
    public void testQuery() {
        assertEquals(
                TestUtils.readResource("/queries/expected_sku_price.yt.sql"),
                skuPriceLoader.getQuery(LocalDate.of(2021, 6, 15)));
    }
}
