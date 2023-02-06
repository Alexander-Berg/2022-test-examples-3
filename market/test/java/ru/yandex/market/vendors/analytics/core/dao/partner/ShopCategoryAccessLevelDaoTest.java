package ru.yandex.market.vendors.analytics.core.dao.partner;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.jpa.entity.partner.ShopCategoryAccessLevel;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.AccessLevel;

/**
 * Функциональные тесты для {@link ShopCategoryAccessLevelDao}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "ShopCategoryAccessLevelDaoTest.before.csv")
public class ShopCategoryAccessLevelDaoTest extends FunctionalTest {

    @Autowired
    private ShopCategoryAccessLevelDao shopCategoryAccessLevelDao;

    @Test
    void getBrandNamesTest() {
        List<ShopCategoryAccessLevel> expected = List.of(
                ShopCategoryAccessLevel.builder()
                        .partnerId(100)
                        .applicationId(1)
                        .businessName("ООО Яндекс")
                        .hid(999)
                        .categoryName("31_имя")
                        .accessLevel(AccessLevel.FROM_1_TO_5)
                        .build(),
                ShopCategoryAccessLevel.builder()
                        .partnerId(100)
                        .applicationId(1)
                        .businessName("ООО Яндекс")
                        .hid(20)
                        .categoryName("1_имя")
                        .accessLevel(AccessLevel.FROM_0_5_TO_1)
                        .build()
        );
        List<ShopCategoryAccessLevel> actual = shopCategoryAccessLevelDao.findAll();
        Assertions.assertEquals(expected, actual);
    }
}
