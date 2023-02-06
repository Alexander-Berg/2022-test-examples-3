package ru.yandex.market.logistics.nesu.controller.settings;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление настроек типа доставки для сендера")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
class SettingsControllerDeliveryTypeUpdateSettingsTest extends AbstractSettingsControllerTest {

    @Test
    @DisplayName("Кейс с отсутствием сендера")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void createOrUpdateTariffSettingsSenderNotFound() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request.json"))
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [2]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[2]}"));
    }

    @Test
    @DisplayName("Кейс с отсутствием тарифа")
    void createOrUpdateTariffSettingsTariffNotFound() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/UNKNOWN")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Неизвестный тип доставки: UNKNOWN"));
    }

    @Test
    @DisplayName("Кейс с пустым service_type")
    void createOrUpdateTariffSettingsEmptyServiceType() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_empty_service.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "deliveryTypeServiceSettings[0].serviceType",
                "must not be null",
                "senderDeliveryTypeSettingsDto",
                "NotNull"
            )));
    }

    @Test
    @DisplayName("Кейс с невалидным service_type")
    void createOrUpdateTariffSettingsNotValidServiceType() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_error_service.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid request payload, please refer to method documentation"));
    }

    @Test
    @DisplayName("Кейс с null ссылкой на настройки доп.услуги тип доставки")
    void createOrUpdateTariffSettingsNullTariffSettingsLink() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_null_service_settings.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "deliveryTypeServiceSettings",
                "must not be empty",
                "senderDeliveryTypeSettingsDto",
                "NotEmpty"
            )));
    }

    @Test
    @DisplayName("Кейс с пустым списком настроек")
    void createOrUpdateTariffSettingsEmptySettingsList() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_null_service_settings.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "deliveryTypeServiceSettings",
                "must not be empty",
                "senderDeliveryTypeSettingsDto",
                "NotEmpty"
            )));
    }

    @Test
    @DisplayName("Кейс с содержанием null в списке настроек")
    void createOrUpdateTariffSettingsSettingsListWithNull() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_null_in_service_settings.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "deliveryTypeServiceSettings[0]",
                "must not be null",
                "senderDeliveryTypeSettingsDto",
                "NotNull"
            )));
    }

    @Test
    @DisplayName("Отсутствует возможная доп. услуга")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void createOrUpdateTariffSettingsServiceNotPresent() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/PICKUP")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_service_not_present.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("""
                Указанная услуга SORT не применима к текущему тарифу PICKUP. \
                Список возможных услуг тарифа [RETURN, RETURN_SORT]"""
            ));
    }

    @Test
    @DisplayName("Создание новых настроек")
    @DatabaseSetup("/controller/meta/tariff/before/empty_delivery_type.xml")
    void createOrUpdateTariffSettingsCreate() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(content().json("{}"));

        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request.json"))
        )
            .andExpect(status().isOk());

        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/sender_delivery_type_settings_updated_response.json"));
    }

    @Test
    @DisplayName("Обновление существующих настроек")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void createOrUpdateTariffSettingsUpdate() throws Exception {
        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/sender_delivery_type_settings_response.json"));

        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request.json"))
        )
            .andExpect(status().isOk());

        mockMvc.perform(
            get("/back-office/settings/sender/delivery-types/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/meta/tariff/after/sender_delivery_type_settings_updated_response.json"));
    }

    @Test
    @DisplayName("Деактивация обязательной услуги")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void createOrUpdateTariffSettingsTryDeactivateRequired() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_is_required.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Указанная услуга RETURN для тарифа COURIER не может быть деактивирована т.к. является обязательной."
            ));
    }

    @Test
    @DisplayName("Изменение оплаты клиентом услуги, для которой это запрещено")
    @DatabaseSetup("/controller/meta/tariff/before/default_delivery_type.xml")
    void changeNonEditableCustomerPay() throws Exception {
        mockMvc.perform(
            post("/back-office/settings/sender/delivery-types/update/COURIER")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/meta/tariff/update_request_customer_pay.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Невозможно изменить оплату клиентом услуги RETURN для тарифа COURIER."));
    }
}
