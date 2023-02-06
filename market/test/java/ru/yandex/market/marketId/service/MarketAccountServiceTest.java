package ru.yandex.market.marketId.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.model.entity.MarketAccountEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "../Test.before.csv")
public class MarketAccountServiceTest extends FunctionalTest {

    @Autowired
    private MarketAccountService marketAccountService;

    @Test
    @DisplayName("Тест репозитория Маркет ID")
    void marketAccountSimpleTest() {
        MarketAccountEntity expected = new MarketAccountEntity();
        expected.setMarketId(1);
        Optional<MarketAccountEntity> actual = marketAccountService.findById(1L);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }
}
