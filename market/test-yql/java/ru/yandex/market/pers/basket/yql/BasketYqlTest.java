package ru.yandex.market.pers.basket.yql;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.yt.yqlgen.YqlLoader;
import ru.yandex.yt.yqltest.YqlTestScript;
import ru.yandex.yt.yqltest.spring.AbstractYqlTest;

/**
 * @author grigor-vlad
 * 05.03.2022
 */
public class BasketYqlTest extends AbstractYqlTest {

    @Test
    public void testBasketItems() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/basket/basket_items.sql")),
            "/basket/basket_items_expected.json",
            "/basket/basket_items.mock"
        );
    }
}
