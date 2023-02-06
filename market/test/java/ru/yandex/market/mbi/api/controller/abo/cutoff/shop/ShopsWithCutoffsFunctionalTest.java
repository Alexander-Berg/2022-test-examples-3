package ru.yandex.market.mbi.api.controller.abo.cutoff.shop;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(
        before = "ShopsWithCutoffsFunctionalTest.before.csv"
)
public class ShopsWithCutoffsFunctionalTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "testShopsWithCutoffs.before.csv")
    void testShopsWithCutoffs() {
        List<Long> shopIds = mbiApiClient.getShopsWithAboCutoff(AboCutoff.ORDER_FINAL_STATUS_NOT_SET, PartnerPlacementProgramType.DROPSHIP_BY_SELLER);

        assertEquals(2, shopIds.size());
        assertTrue(shopIds.contains(774L));
        assertTrue(shopIds.contains(775L));
    }

    @Test
    @DbUnitDataSet(before = "testShopsWithCutoffsNoProgram.before.csv")
    void testShopsWithCutoffsNoProgram() {
        List<Long> shopIds = mbiApiClient.getShopsWithAboCutoff(AboCutoff.ORDER_FINAL_STATUS_NOT_SET, null);

        assertEquals(1, shopIds.size());
        assertTrue(shopIds.contains(774L));
    }

    @Test
    void testShopsWithCutoffsEmptyList() {
        List<Long> shopIds = mbiApiClient.getShopsWithAboCutoff(AboCutoff.ORDER_FINAL_STATUS_NOT_SET, PartnerPlacementProgramType.DROPSHIP_BY_SELLER);

        assertTrue(shopIds.isEmpty());
    }
}
