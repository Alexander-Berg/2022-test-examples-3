package ru.yandex.market.logistics.nesu.controller.internal;

import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.model.shop.UpdateShopDto;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.ShopLocalRegionUpdateRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createAddressDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPhoneDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.updateBusinessWarehouseDtoBuilder;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_EXTERNAL_ID_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_NAME_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_SYMBOLS_IN_EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.externalIdValidationErrorData;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.validationRequest;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление магазина")
@ParametersAreNonnullByDefault
public class InternalShopUpdateTest extends AbstractContextualTest {
    private static final String BLANK_STRING = " \t\n ";
    private static final String NEW_NAME = "brand new name";
    private static final String NEW_EXTERNAL_ID = "new-external-id";
    private static final long BUSINESS_ID = 41;
    private static final long PARTNER_ID = 11;
    private static final String OBJECT_NAME = "updateShopDto";

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/after/after_all_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successAll() throws Exception {
        mockGetWarehouse();
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());

        updateShop(NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isOk());

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            updater -> updater.name(NEW_NAME)
                .readableName(NEW_NAME)
                .externalId(NEW_EXTERNAL_ID)
        );
        verify(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());
    }

    @Test
    @DisplayName("Обновляется только имя")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/after/after_name_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNameOnly() throws Exception {
        mockGetWarehouse();

        updateShop(NEW_NAME, BLANK_STRING)
            .andExpect(status().isOk());

        verify(lmsClient).getBusinessWarehouseForPartner(PARTNER_ID);
        verifyUpdateWarehouse(
            updater -> updater.name(NEW_NAME)
                .readableName(NEW_NAME)
        );
    }

    @Test
    @DisplayName("Обновляется только внешний идентификатор")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/after/after_external_id_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successExternalIdOnly() throws Exception {
        mockGetWarehouse();
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());

        updateShop(BLANK_STRING, NEW_EXTERNAL_ID)
            .andExpect(status().isOk());

        verifyGetWarehouse();
        verifyUpdateWarehouse(updater -> updater.externalId(NEW_EXTERNAL_ID));
        verify(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());
    }

    @Test
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/before/prepare_valid_shop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обновление только внешнего идентификатора на невалидный  идентификатор")
    void invalidExternalId() throws Exception {
        mockGetWarehouse();
        doReturn(BusinessWarehouseValidationStatus.INVALID)
            .when(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());

        updateShop(BLANK_STRING, NEW_EXTERNAL_ID)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(externalIdValidationErrorData()));

        verify(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());
    }

    @Test
    @DisplayName("Пустой запрос, везде null")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/before/prepare_valid_shop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestFieldsAreNull() throws Exception {
        updateShop(null, null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.fieldError(
                "validForUpdate",
                "fields must not be blank",
                OBJECT_NAME,
                "AssertTrue"
            )));
    }

    @Test
    @DisplayName("Пустой запрос с вайтспейсами")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/before/prepare_valid_shop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestFieldsAreBlank() throws Exception {
        updateShop(BLANK_STRING, BLANK_STRING)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.fieldError(
                "validForUpdate",
                "fields must not be blank",
                OBJECT_NAME,
                "AssertTrue"
            )));
    }

    @Test
    @DisplayName("Отсутствуют склады")
    @DatabaseSetup("/controller/shop-update/before/prepare_invalid_shop_no_warehouse.xml")
    void noShopPartnerSettings() throws Exception {
        updateShop(NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Shop with id 1 has no warehouse"));
    }

    @Test
    @DisplayName("Склад не найден")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/before/prepare_valid_shop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void warehouseNotFound() throws Exception {
        updateShop(NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_WAREHOUSE] with ids [11]"));

        verifyGetWarehouse();
    }

    @Test
    @DisplayName("Несколько складов")
    @DatabaseSetup("/controller/shop-update/before/prepare_invalid_shop_multi_warehouse.xml")
    void severalShopPartnerSettings() throws Exception {
        updateShop(NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("More than one shop partner settings found for shop 1"));
    }

    @Test
    @DisplayName("Магазин не существует")
    void shopNotExists() throws Exception {
        updateShop(42, NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [42]"));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    @DisplayName("Некорректная роль магазина")
    @DatabaseSetup("/controller/shop-update/before/prepare_invalid_shop_role.xml")
    void incorrectRole(long shopId) throws Exception {
        updateShop(shopId, NEW_NAME, NEW_EXTERNAL_ID)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Incorrect role for shop " + shopId + ", must be DROPSHIP_BY_SELLER"));
    }

    @MethodSource
    @DisplayName("Проверка валидаций")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void checkValidations(
        String displayName,
        String name,
        String externalId,
        ValidationErrorData.ValidationErrorDataBuilder errorDataBuilder
    ) throws Exception {
        updateShop(name, externalId)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorDataBuilder.forObject(OBJECT_NAME)));
    }

    @Test
    @DisplayName("Обновление локального региона магазина")
    @DatabaseSetup("/controller/shop-update/before/prepare_valid_shop.xml")
    @ExpectedDatabase(
        value = "/controller/shop-update/after/after_local_region_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateLocalRegion() throws Exception {
        int expectedLocalRegion = 213;
        long shopId = 1L;
        mockGetWarehouse();

        updateShop(shopId, expectedLocalRegion).andExpect(status().isOk());

        ShopLocalRegionUpdateRequest updateRequest =
                new ShopLocalRegionUpdateRequest().setLocalRegion((long) expectedLocalRegion);

        verify(lmsClient).getBusinessWarehouseForPartner(PARTNER_ID);
        verify(tarifficatorClient).updateShopLocalRegion(eq(shopId), eq(11L), eq(updateRequest));
        verifyUpdateWarehouse(
            updater -> updater.address(
                Address.newBuilder().locationId(expectedLocalRegion).settlement("Москва").region("Москва").build()
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> checkValidations() {
        return Stream.of(
            Arguments.of(
                "Длина названия превышена",
                INVALID_NAME_LENGTH,
                null,
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.size(0, 256))
            ),
            Arguments.of(
                "Длина внешнего идентификатора превышена",
                null,
                INVALID_EXTERNAL_ID_LENGTH,
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.size(0, 64))
            ),
            Arguments.of(
                "Невалидный внешний идентификатор",
                null,
                INVALID_SYMBOLS_IN_EXTERNAL_ID,
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.VALID_EXTERNAL_ID)
            )
        );
    }

    private void mockGetWarehouse() {
        when(lmsClient.getBusinessWarehouseForPartner(PARTNER_ID))
            .thenReturn(Optional.of(LmsFactory.businessWarehouseBuilder(PARTNER_ID).businessId(BUSINESS_ID).build()));
    }

    private void verifyGetWarehouse() {
        verify(lmsClient).validateExternalIdInBusiness(expectedValidationRequest());
        verify(lmsClient).getBusinessWarehouseForPartner(PARTNER_ID);
    }

    private void verifyUpdateWarehouse(UnaryOperator<UpdateBusinessWarehouseDto.Builder> updateDtoModifier) {
        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateDtoModifier.apply(
                updateBusinessWarehouseDtoBuilder()
                    .address(createAddressDto())
                    .phones(Set.of(createPhoneDto()))
                    .name("Warehouse name")
                    .readableName("Warehouse name")
                    .externalId("external-id-11")
            )
                .build()
        );
    }

    @Nonnull
    private BusinessWarehouseValidationRequest expectedValidationRequest() {
        return validationRequest(BUSINESS_ID, NEW_EXTERNAL_ID, PARTNER_ID);
    }

    @Nonnull
    private ResultActions updateShop(
        @Nullable String name,
        @Nullable String externalId
    ) throws Exception {
        return updateShop(1, name, externalId, null);
    }

    @Nonnull
    private ResultActions updateShop(
            long shopId,
            @Nullable String name,
            @Nullable String externalId
    ) throws Exception {
        return updateShop(shopId, name, externalId, null);
    }

    @Nonnull
    private ResultActions updateShop(
            long shopId,
            int localDeliveryRegion
    ) throws Exception {
        return updateShop(shopId, null, null, localDeliveryRegion);
    }

    @Nonnull
    private ResultActions updateShop(
        long shopId,
        @Nullable String name,
        @Nullable String externalId,
        @Nullable Integer localDeliveryRegion
    ) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/internal/shops/" + shopId,
            UpdateShopDto.builder().name(name).externalId(externalId).localDeliveryRegion(localDeliveryRegion).build()
        ));
    }
}
