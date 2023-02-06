package ru.yandex.market.logistics.nesu.controller.partner;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.dto.partner.PartnerSettingsRequest;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopPartnerType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/partner/settings/setup.xml")
@DisplayName("Обновление настроек синхронизации для партнеров")
class PartnerControllerUpdateSettingsTest extends AbstractPartnerControllerSettingsTest {

    @Test
    @DisplayName("Успешное изменение настроек Dropship")
    void successDropship() throws Exception {
        mockGetPartner(1);
        PartnerSettingDto lmsSettingsRequest = defaultSettings().stockSyncEnabled(false).build();
        mockUpdatePartnerSettings(1, lmsSettingsRequest);

        updatePartnerSettings(1, 1, ShopPartnerType.DROPSHIP, false)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/settings/update_dropship_settings_response.json"));

        verifyGetPartner(1);
        verifyUpdatePartnerSettings(1, lmsSettingsRequest);
    }

    @Test
    @DisplayName("Успешное изменение настроек Dropship by Seller")
    void successDropshipBySeller() throws Exception {
        mockGetPartner(
            2,
            partner -> partner.marketId(100L)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                .korobyteSyncEnabled(false)
                .stockSyncEnabled(false)
        );
        PartnerSettingDto lmsSettingsRequest = defaultSettings().korobyteSyncEnabled(false).build();
        mockUpdatePartnerSettings(2, lmsSettingsRequest);

        updatePartnerSettings(2, 2, ShopPartnerType.DROPSHIP_BY_SELLER, true)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/settings/update_dropship_by_seller_settings_response.json"));

        verifyGetPartner(2);
        verifyUpdatePartnerSettings(2, lmsSettingsRequest);
    }

    @Test
    @DisplayName("Невалидный запрос")
    void requestValidation() throws Exception {
        updatePartnerSettings(1, 1, ShopPartnerType.DROPSHIP, null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fieldErrorBuilder("stockSyncEnabled", ErrorType.NOT_NULL).forObject("partnerSettingsRequest")
            ));
    }

    @Test
    @DisplayName("Партнёр из связки не найден")
    void partnerNotFound() throws Exception {
        updatePartnerSettings(1, 1, ShopPartnerType.DROPSHIP, false)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [1]"));

        verifyGetPartner(1);
    }

    @Test
    @DisplayName("Передан неверный тип партнёра")
    void incorrectShopPartnerType() throws Exception {
        updatePartnerSettings(1, 1, ShopPartnerType.SUPPLIER, false)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find relation between shop 1 and SUPPLIER partner 1"));
    }

    @Test
    @DisplayName("Передан неверный партнёр")
    void incorrectPartnerId() throws Exception {
        updatePartnerSettings(1, 2, ShopPartnerType.DROPSHIP_BY_SELLER, false)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find relation between shop 1 and DROPSHIP_BY_SELLER partner 2"));
    }

    @Nonnull
    private ResultActions updatePartnerSettings(
        long shopId,
        long partnerId,
        ShopPartnerType shopPartnerType,
        Boolean stockSyncEnabled
    ) throws Exception {
        return mockMvc.perform(
            request(
                HttpMethod.PUT,
                "/back-office/partner/" + partnerId + "/settings",
                new PartnerSettingsRequest().setStockSyncEnabled(stockSyncEnabled)
            )
                .param("userId", "1")
                .param("shopId", String.valueOf(shopId))
                .param("partnerType", String.valueOf(shopPartnerType))
        );
    }

    @Nonnull
    private PartnerSettingDto.Builder defaultSettings() {
        return PartnerSettingDto.newBuilder()
            .stockSyncEnabled(true)
            .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
            .locationId(213)
            .trackingType("Tracking type")
            .autoSwitchStockSyncEnabled(true)
            .korobyteSyncEnabled(true);
    }

    private void mockUpdatePartnerSettings(long partnerId, PartnerSettingDto expectedSettings) {
        when(lmsClient.updatePartnerSettings(partnerId, expectedSettings)).thenReturn(expectedSettings);
    }

    private void verifyUpdatePartnerSettings(long partnerId, PartnerSettingDto expectedSettings) {
        verify(lmsClient).updatePartnerSettings(partnerId, expectedSettings);
    }
}
