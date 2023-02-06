package ru.yandex.market.pers.tms.yt.yql.tables;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

public class ShopRatingYqlTest extends AbstractPersYqlTest {

    @Test
    public void test() {
        runTest(
                loadScript("/yql/tables/shop_rating.sql"),
                "/tables/shop_rating_expected.json",
                "/tables/shop_rating.mock"
        );
    }
}
