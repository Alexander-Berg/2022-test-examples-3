package ru.yandex.market.bidding.engine.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.bidding.FunctionalTest;
import ru.yandex.market.bidding.engine.BasicPartner;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.bidding.AuctionTestCommons.SHOP_774;

/**
 * Тесты для {@link CategoryBidsDataAccess}.
 *
 * @author vbudnev
 */
class CategoryBidsDataAccessTest extends FunctionalTest {

    @Autowired
    private CategoryBidsDataAccess categoryBidsDataAccess;

    /**
     * Smoke test.
     * Проверяет, что кодогенеренный запрос для партнера SHOP отрабатывает синтаксически и что-то поднимает из базы.
     */
    @DisplayName("SmokeTest на получение категорийных ставок. Partner=SHOP")
    @DbUnitDataSet(before = "db/CategoryBidsDataAccessTest.before.csv")
    @Test
    void test_loadSmokeTest() {
        BasicPartner.Builder builder = new BasicPartner.Builder().id(SHOP_774);
        categoryBidsDataAccess.load(builder);

        assertThat(builder.build().getCategoryBids(), hasSize(2));
    }

    /**
     * Smoke test.
     * Проверяет, что кодогенеренный запрос для партнера SHOP отрабатывает синтаксически и производит удаление из базы.
     */
    @DisplayName("SmokeTest на удаление категорийныъ ставок. Partner=SHOP")
    @DbUnitDataSet(
            before = "db/CategoryBidsDataAccessTest.before.csv",
            after = "db/CategoryBidsDataAccessTest.clean_smoke_test.after.csv"
    )
    @Test
    void test_cleanSmokeTest() {
        categoryBidsDataAccess.cleanBids(SHOP_774);
    }
}