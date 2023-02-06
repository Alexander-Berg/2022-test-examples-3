package ru.yandex.market.logistics.nesu.controller.business;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;
import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.BUSINESS_ID;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipBusinessWarehouseRequest;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipResponse;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipUpdateWarehouseDtoToLms;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.externalIdValidationErrorData;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.validationRequest;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_SHOP_ID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Создание склада магазина с поддержкой уникальности")
class CreateUniqueBusinessWarehouseTest extends AbstractCreateBusinessWarehouseTest {

    @Test
    @SneakyThrows
    @DisplayName("У партнёра более чем одна настройка, настройки не создаются")
    @DatabaseSetup("/controller/business/create-unique/before/invalid_dropship_shop_partner_settings.xml")
    @ExpectedDatabase(
        value = "/controller/business/create-unique/before/invalid_dropship_shop_partner_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidWarehouseSettings() {
        createDropshipBusinessWarehouse()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Found more than 1 partner settings for shop 1 and partnerType DROPSHIP"));

        verifyValidation();
    }

    @Test
    @SneakyThrows
    @DisplayName("Настройки существуют, склада в лмс нет")
    @DatabaseSetup("/controller/business/create-unique/after/dropship_setting_exists.xml")
    void incorrectSettings() {
        doThrow(new HttpTemplateException(404, "Error"))
            .when(lmsClient).updateBusinessWarehouse(eq(DROPSHIP_PARTNER_ID), any());

        createDropshipBusinessWarehouse()
            .andExpect(status().isNotFound());

        verify(lmsClient).updateBusinessWarehouse(eq(DROPSHIP_PARTNER_ID), any());
        verifyValidation();
        verify(lmsClient).validateExternalIdInBusiness(validationRequest(BUSINESS_ID, DROPSHIP_PARTNER_ID));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное создание склада")
    @ExpectedDatabase(
        value = "/controller/business/create-unique/after/dropship_setting_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createNewWarehouseSuccess() {
        createDropshipBusinessWarehouse()
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonContent("controller/business/response/dropship_partner_create_response.json"));

        verifySuccessFlow(true);
    }

    @Test
    @SneakyThrows
    @DisplayName("Склад существовал, успешное обновление")
    @DatabaseSetup("/controller/business/create-unique/after/dropship_setting_exists.xml")
    void updateExistingWarehouseSuccess() {
        mockUpdateBusinessWarehouse();

        createDropshipBusinessWarehouse()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/dropship_partner_create_response.json"));

        verifySuccessFlow(false);
    }

    @Test
    @SneakyThrows
    @DisplayName("Склад существовал, обновляется внешний идентификатор на невалидный")
    @DatabaseSetup("/controller/business/create-unique/after/dropship_setting_exists.xml")
    void invalidExternalIdInUpdate() {
        mockDropshipPartnerApiSettings();
        mockUpdateBusinessWarehouse();
        BusinessWarehouseValidationRequest updateValidationRequest = validationRequest(
            BUSINESS_ID,
            "invalid-ext-id",
            DROPSHIP_PARTNER_ID
        );
        BusinessWarehouseValidationRequest createValidationRequest = validationRequest(
            BUSINESS_ID,
            "invalid-ext-id",
            null
        );
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(createValidationRequest);
        doReturn(BusinessWarehouseValidationStatus.INVALID)
            .when(lmsClient).validateExternalIdInBusiness(updateValidationRequest);

        createBusinessWarehouse(DROPSHIP_SHOP_ID, dropshipBusinessWarehouseRequest().setExternalId("invalid-ext-id"))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(externalIdValidationErrorData()));

        verify(lmsClient).validateExternalIdInBusiness(updateValidationRequest);
        verify(lmsClient).validateExternalIdInBusiness(createValidationRequest);
    }

    private void verifySuccessFlow(boolean isWarehouseCreated) {
        if (isWarehouseCreated) {
            verifyDropshipWarehouseCreated();
        } else {
            verifyDropshipWarehouseUpdated();
        }
        verifyDropshipTasksCreated();
    }

    private void verifyDropshipWarehouseUpdated() {
        verify(lmsClient).updateBusinessWarehouse(DROPSHIP_PARTNER_ID, dropshipUpdateWarehouseDtoToLms());
        verify(lmsClient).validateExternalIdInBusiness(
            validationRequest(BUSINESS_ID, EXTERNAL_ID, DROPSHIP_PARTNER_ID)
        );
        verify(lmsClient).validateExternalIdInBusiness(validationRequest(BUSINESS_ID));
    }

    private void mockUpdateBusinessWarehouse() {
        doReturn(Optional.of(dropshipResponse()))
            .when(lmsClient).getBusinessWarehouseForPartner(DROPSHIP_PARTNER_ID);
        doReturn(dropshipResponse())
            .when(lmsClient).updateBusinessWarehouse(DROPSHIP_PARTNER_ID, dropshipUpdateWarehouseDtoToLms());
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(validationRequest(BUSINESS_ID, DROPSHIP_PARTNER_ID));
    }

    @Nonnull
    @Override
    @SneakyThrows
    ResultActions createBusinessWarehouse(String shopId, BusinessWarehouseRequest dto) {
        MockHttpServletRequestBuilder request = post("/back-office/business/warehouses/unique")
            .param("userId", "1")
            .param("shopId", shopId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto));

        return mockMvc.perform(request);
    }
}
