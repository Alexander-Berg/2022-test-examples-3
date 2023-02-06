package ru.yandex.market.core.salesnotes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Функциональные тесты на {@link SalesNotesService}
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "SalesNotesServiceTest.before.csv")
class SalesNotesServiceTest extends FunctionalTest {
    private static final long EXISTING_SHOP = 111L;
    private static final long NEW_SHOP = 222L;
    private static final int ORDER_MIN_COST_VALUE = 150;
    private static final int FREE_DELIVERY_THRESHOLD = 1000;

    @Autowired
    SalesNotesService salesNotesService;

    @Test
    @DbUnitDataSet(after = "SetOrderMinCostEnabledTest.after.csv")
    void setOrderMinCostEnabledTest() {
        salesNotesService.setOrderMinCostEnabled(EXISTING_SHOP, true);
    }

    @Test
    @DbUnitDataSet(after = "SalesNotesServiceTest.before.csv")
    void setOrderMinCostEnabledNewShopTest() {
        salesNotesService.setOrderMinCostEnabled(NEW_SHOP, true);
    }

    @Test
    @DbUnitDataSet(after = "SetOrderMinCostTest.after.csv")
    void setOrderMinCostValueTest() {
        salesNotesService.setOrderMinCostValue(EXISTING_SHOP, ORDER_MIN_COST_VALUE);
    }

    @Test
    @DbUnitDataSet(after = "SetOrderMinCostTest.nullValue.after.csv")
    void setOrderMinCostNullValueTest() {
        salesNotesService.setOrderMinCostValue(EXISTING_SHOP, null);
    }

    @Test
    @DbUnitDataSet(after = "SetOrderMinCostTest.newShop.after.csv")
    void setOrderMinCostValueNewShopTest() {
        salesNotesService.setOrderMinCostValue(NEW_SHOP, ORDER_MIN_COST_VALUE);
    }

    @Test
    @DbUnitDataSet(after = "SetFreeDeliveryThreshold.after.csv")
    void setFreeDeliveryThresholdTest() {
        salesNotesService.setFreeDeliveryThresholdValue(EXISTING_SHOP, FREE_DELIVERY_THRESHOLD);
    }

    @Test
    @DbUnitDataSet(after = "DisableFreeDeliveryThreshold.after.csv")
    void disableFreeDeliveryThresholdTest() {
        salesNotesService.setFreeDeliveryThresholdEnabled(EXISTING_SHOP, false);
    }

    @Test
    @DbUnitDataSet(after = "DeleteFreeDeliveryThreshold.after.csv")
    void deleteFreeDeliveryThresholdTest() {
        salesNotesService.deleteFreeDeliveryThresholdValue(EXISTING_SHOP);
    }
}
