package ru.yandex.market.logistics.nesu.controller.partner;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopPartnerType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/partner/settings/setup.xml")
@DisplayName("Получение настроек синхронизации для партнеров")
class PartnerControllerGetSettingsTest extends AbstractPartnerControllerSettingsTest {

    @Test
    @DisplayName("Успешное получение настроек Dropship")
    void successDropship() throws Exception {
        mockGetPartner(1);

        getPartnerSettings(1, 1, ShopPartnerType.DROPSHIP)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/settings/get_dropship_settings_response.json"));

        verifyGetPartner(1);
    }

    @Test
    @DisplayName("Успешное получение настроек Dropship by Seller")
    void successDropshipBySeller() throws Exception {
        mockGetPartner(
            2,
            partner -> partner.marketId(100L)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                .korobyteSyncEnabled(false)
                .stockSyncEnabled(false)
        );

        getPartnerSettings(2, 2, ShopPartnerType.DROPSHIP_BY_SELLER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/settings/get_dropship_by_seller_settings_response.json"));

        verifyGetPartner(2);
    }

    @Test
    @DisplayName("Партнёр из связки не найден")
    void partnerNotFound() throws Exception {
        getPartnerSettings(1, 1, ShopPartnerType.DROPSHIP)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [1]"));

        verifyGetPartner(1);
    }

    @Test
    @DisplayName("Передан неверный тип партнёра")
    void incorrectShopPartnerType() throws Exception {
        getPartnerSettings(1, 1, ShopPartnerType.SUPPLIER)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find relation between shop 1 and SUPPLIER partner 1"));
    }

    @Test
    @DisplayName("Передан неверный партнёр")
    void incorrectPartnerId() throws Exception {
        getPartnerSettings(1, 2, ShopPartnerType.DROPSHIP_BY_SELLER)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find relation between shop 1 and DROPSHIP_BY_SELLER partner 2"));
    }

    @Nonnull
    private ResultActions getPartnerSettings(
        long shopId,
        long partnerId,
        ShopPartnerType shopPartnerType
    ) throws Exception {
        return mockMvc.perform(
            get("/back-office/partner/{partnerId}/settings", partnerId)
                .param("userId", "1")
                .param("shopId", String.valueOf(shopId))
                .param("partnerType", String.valueOf(shopPartnerType))
        );
    }
}
