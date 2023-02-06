package ru.yandex.market.logistics.nesu.controller.business;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.controller.partner.AbstractNewRelationTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Сохранение настроек отгрузки бизнес-склада")
class SaveBusinessWarehouseShipmentTest extends AbstractNewRelationTest {

    @Nonnull
    @Override
    protected String getUrl(long partnerId) {
        return "/back-office/business/warehouses/" + partnerId + "/shipment-settings";
    }

    @Test
    @DisplayName("Отсутствуют настройки связи магазина и партнера")
    void noShopPartnerSettings() throws Exception {
        saveRelation(1, 2, defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PARTNER_SETTINGS] with fromPartnerId 2"));
    }
}
