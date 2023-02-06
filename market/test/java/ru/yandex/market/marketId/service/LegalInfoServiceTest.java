package ru.yandex.market.marketId.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.model.entity.LegalInfoEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "../Test.before.csv")
class LegalInfoServiceTest extends FunctionalTest {

    @Autowired
    private LegalInfoService legalInfoService;


    @Test
    @DisplayName("Поиск Юр. инфо по маркет ид")
    void findLegalInfoByMarketAccount() {
        List<LegalInfoEntity> legalInfoEntityList =
                legalInfoService.findByMarketAccountId(1L);
        assertEquals(legalInfoEntityList.size(), 5);
    }

    @Test
    @DisplayName("Поиск Юр. инфо по маркет ид. Ничего не найдено")
    void findLegalInfoByMarketAccountNotFound() {
        List<LegalInfoEntity> legalInfoEntityList =
                legalInfoService.findByMarketAccountId(3L);
        assertNotNull(legalInfoEntityList);
        assertTrue(legalInfoEntityList.isEmpty());
    }
}
