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
 * Тесты для {@link OfferBidsDataAccess}.
 *
 * @author vbudnev
 */
class OfferBidsDataAccessTest extends FunctionalTest {
    @Autowired
    private OfferBidsDataAccess offerBidsDataAccess;

    /**
     * Smoke test.
     * Проверяет, что кодогенеренный запрос для партнера SHOP отрабатывает синтаксически и что-то поднимает из базы.
     */
    @DisplayName("SmokeTest на получение овверных ставок. Partner=SHOP")
    @DbUnitDataSet(before = "db/OffersBidsDataAccessTest.before.csv")
    @Test
    void test_loadSmokeTest() {
        BasicPartner.Builder builder = new BasicPartner.Builder().id(SHOP_774);
        offerBidsDataAccess.load(builder);

        assertThat(builder.build().offerBids(), hasSize(2));
    }

    /**
     * Smoke test.
     * Проверяет, что кодогенеренный запрос для партнера SHOP отрабатывает синтаксически и и производит удаление из базы.
     */
    @DisplayName("SmokeTest на удаление офферных ставок. Partner=SHOP")
    @DbUnitDataSet(
            before = "db/OffersBidsDataAccessTest.before.csv",
            after = "db/OffersBidsDataAccessTest.clean_smoke_test.after.csv"
    )
    @Test
    void test_cleanSmokeTest() {
        offerBidsDataAccess.cleanBids(SHOP_774, 1, 1);
    }

}