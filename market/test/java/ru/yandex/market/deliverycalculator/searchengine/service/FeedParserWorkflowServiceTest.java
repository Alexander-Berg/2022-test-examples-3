package ru.yandex.market.deliverycalculator.searchengine.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.workflow.service.ShopSettingsCacheService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedParserWorkflowServiceTest extends FunctionalTest {

    @Autowired
    private FeedParserWorkflowService tested;
    @Autowired
    private ShopSettingsCacheService shopSettingsCacheService;

    @AfterEach
    void cleanUp() {
        tested.updateActiveGenerationId();
    }

    @Test
    @DbUnitDataSet(before = "data/db/shopModifiersGenerations.csv")
    void importShopModifiersGenerations() {
        tested.updateActiveGenerationId();
        tested.importGenerations();

        assertFalse(shopSettingsCacheService.getCacheValue(1L, 10).isPresent());
        assertTrue(shopSettingsCacheService.getCacheValue(1L, 2).isPresent());
        assertTrue(shopSettingsCacheService.getCacheValue(2L, 10).isPresent());
    }

    @Test
    @DbUnitDataSet(before = "data/db/shopModifiersGenerations.csv")
    void outdateShopModifiersGenerations() {
        tested.updateActiveGenerationId();
        tested.importGenerations();

        tested.outdateGenerations();

        assertFalse(shopSettingsCacheService.getCacheValue(1L, 10).isPresent());
        assertFalse(shopSettingsCacheService.getCacheValue(2L, 4).isPresent());
        assertTrue(shopSettingsCacheService.getCacheValue(2L, 5).isPresent());
    }
}
