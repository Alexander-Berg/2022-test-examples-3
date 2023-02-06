package ru.yandex.market.logistics.nesu.controller.settings;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты получения настроек СД АПИ SettingsController")
class SettingsControllerGetDeliverySettingsTest extends AbstractSettingsControllerTest {

    @Test
    @DisplayName("Получение информации о службах доставки по не представленному идентификатору сендера")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_deliveries_dataset.xml")
    void readDeliverySettingsNoSender() throws Exception {
        getDeliverySettings("2", "40")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Test
    @DisplayName("Получение информации о службах доставки сендера при отсутствии связей")
    @DatabaseSetup("/repository/settings/sender/before/common_sender.xml")
    void readDeliverySettingsEmptyList() throws Exception {
        getDeliverySettings("1", "213")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/get_delivery_empty_response.json"));
    }

    @Test
    @DisplayName("Получение информации о службах доставки сендера с указанием локации: "
        + "нет настроек СЦ для сендера в регионе")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_no_active_deliveries_dataset.xml")
    void readDeliverySettingsNoSenderRegionSettings() throws Exception {
        getDeliverySettings("1", "40")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/get_delivery_empty_response.json"));
    }

    @Test
    @DisplayName("Получение информации о службах доставки сендера с указанием локации")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_no_active_deliveries_dataset.xml")
    void readDeliverySettingsInLocation() throws Exception {
        getDeliverySettings("1", "1")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/get_delivery_in_location_response.json"));
    }

    @Nonnull
    private ResultActions getDeliverySettings(String senderId, String locationId) throws Exception {
        return mockMvc.perform(
            get("/back-office/settings/sender/delivery")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", senderId)
                .param("locationId", locationId)
        );
    }
}
