package ru.yandex.market.api.partner.rating;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.quality.ShopRatingDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.shop.ShopRating;

public class ShopRatingDaoTest extends FunctionalTest {
    @Autowired
    private ShopRatingDao shopRatingDao;

    @DisplayName("Прямой сценарий, успешно получаем рейтинг магазина по shopId")
    @Test
    @DbUnitDataSet(before = "csv/ShopRatingDaoTest.getShopRatingTest.before.csv")
    void getShopRatingTest() {
        final ShopRating actual = shopRatingDao.getShopRating(1);
        final ShopRating expected = new ShopRating(1, 1.23456789, 0.246913578, Instant.EPOCH);
        Assertions.assertEquals(expected, actual);

    }

    @DisplayName("Если магазина нет, должен возвращаться null")
    @Test
    void getShopRatingTestNotFound() {
        Assertions.assertNull(shopRatingDao.getShopRating(-1));
    }
}
