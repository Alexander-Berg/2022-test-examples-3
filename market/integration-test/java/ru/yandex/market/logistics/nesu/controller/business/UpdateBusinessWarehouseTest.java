package ru.yandex.market.logistics.nesu.controller.business;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.addressWithoutCalculatedFields;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.businessWarehouseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.updateBusinessWarehouseDtoBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.createSchedule;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddress;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressExtendedBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseContact;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.externalIdValidationErrorData;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.validationRequest;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Обновление склада бизнеса")
class UpdateBusinessWarehouseTest extends AbstractModifyingBusinessWarehouseTest {

    private static final long BUSINESS_ID = 42;
    private static final String NEW_EXTERNAL_ID = "new-ext-id";
    private static final long PARTNER_ID = 2;

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void setup() {
        BusinessWarehouseResponse.Builder response = businessWarehouseBuilder(PARTNER_ID)
            .logisticsPointId(3L);
        when(lmsClient.updateBusinessWarehouse(eq(PARTNER_ID), any()))
            .thenAnswer(invocation -> {
                    UpdateBusinessWarehouseDto updateRequest = invocation.getArgument(1);
                    response.logisticsPointId(29L);
                    Optional.ofNullable(updateRequest.getName())
                        .ifPresent(name -> {
                            response.name(name);
                            response.logisticsPointName(name);
                        });
                    Optional.ofNullable(updateRequest.getReadableName())
                        .ifPresent(response::readableName);
                    return response.build();
                }
            );
        when(lmsClient.validateExternalIdInBusiness(validationRequest(BUSINESS_ID, EXTERNAL_ID, PARTNER_ID)))
            .thenReturn(BusinessWarehouseValidationStatus.OK);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, mbiApiClient);
    }

    @DatabaseSetup("/controller/business/create-unique/after/dropship_setting_exists.xml")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({"bodyValidationSourceBase"})
    @DisplayName("Валидация тела запроса")
    void validation(
        ValidationErrorDataBuilder validationError,
        Consumer<BusinessWarehouseRequest> requestConsumer
    ) throws Exception {
        BusinessWarehouseRequest request = createMinimalValidRequest();
        requestConsumer.accept(request);
        updateWarehouse(1, 1, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("businessWarehouseRequest")));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("bodyValidationSourceBase")
    @DisplayName("Валидация тела запроса - DSBS")
    void validationDSBS(
        ValidationErrorDataBuilder validationError,
        Consumer<BusinessWarehouseRequest> requestConsumer
    ) throws Exception {
        BusinessWarehouseRequest request = createMinimalValidRequest();
        requestConsumer.accept(request);
        updateWarehouse(4, 1, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("businessWarehouseRequest")));
    }

    @Test
    @DisplayName("Склад не существует")
    void nonExistentWarehouse() throws Exception {
        updateWarehouse(1, 4, createMinimalValidRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Cannot find shop partner setting for shop 1 and partner 4"));
    }

    @Test
    @DisplayName("Некорректная роль магазина")
    void wrongShopType() throws Exception {
        updateWarehouse(2L, 1, createMinimalValidRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Shop must have role [DROPSHIP, SUPPLIER, DROPSHIP_BY_SELLER, RETAIL] for this operation"
            ));
    }

    @Test
    @DisplayName("Отключенный магазин")
    void disabledShop() throws Exception {
        updateWarehouse(5, 1, createMinimalValidRequest())
            .andExpect(status().isForbidden())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Несуществующий магазин")
    void nonExistentShop() throws Exception {
        updateWarehouse(100500, 1, createMinimalValidRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [100500]"));
    }

    @Nonnull
    private BusinessWarehouseRequest createMinimalValidRequest() {
        BusinessWarehouseRequest request = new BusinessWarehouseRequest();
        request.setSchedule(createSchedule(5))
            .setAddress(warehouseAddressExtendedBuilder().build())
            .setContact(warehouseContact())
            .setName("new name");
        return request;
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 3L})
    @DisplayName("Успешная замена склада")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void success(long shopId) throws Exception {
        updateWarehouse(shopId, PARTNER_ID, createMinimalValidRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(PARTNER_ID, updateBusinessWarehouseDtoBuilder().build());
    }

    @Test
    @DisplayName("Успешная замена склада без указания координат")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void successWithoutCoordinates() throws Exception {
        updateWarehouse(1L, PARTNER_ID, createMinimalValidRequest().setAddress(warehouseAddress()))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateBusinessWarehouseDtoBuilder().address(addressWithoutCalculatedFields().locationId(65).build()).build()
        );
    }

    @Test
    @DisplayName("Успешная замена склада без указания городского округа")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void noSubRegion() throws Exception {
        BusinessWarehouseRequest request = createMinimalValidRequest().setAddress(
            warehouseAddressBuilder()
                .latitude(new BigDecimal("1"))
                .longitude(new BigDecimal("2"))
                .subRegion(null)
                .build()
        );

        updateWarehouse(1L, PARTNER_ID, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateBusinessWarehouseDtoBuilder()
                .address(
                    addressWithoutCalculatedFields()
                        .latitude(new BigDecimal("1"))
                        .longitude(new BigDecimal("2"))
                        .locationId(65)
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Успешная замена со сменой адреса, координаты и городской округ не сохраняются")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void newAddressNoCoordinates() throws Exception {
        BusinessWarehouseRequest request = createMinimalValidRequest().setAddress(
            warehouseAddressBuilder().house("12").build()
        );
        updateWarehouse(1L, PARTNER_ID, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateBusinessWarehouseDtoBuilder()
                .address(addressWithoutCalculatedFields().house("12").locationId(65).build()).build()
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 3L})
    @DisplayName("Попытка заменить склад для CPA магазина на склад с некорректным расписанием")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void cpaIncorrectSchedule(long shopId) throws Exception {
        updateWarehouse(shopId, PARTNER_ID, createMinimalValidRequest().setSchedule(createSchedule(4)))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "schedule",
                "Schedule days count must be greater than or equal to 5",
                "businessWarehouseRequest",
                "ValidScheduleDaysCount",
                Map.of("value", 5)
            )));
    }

    @Test
    @DisplayName("Успешная замена со сменой внешнего идентификатора")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void newExternalId() throws Exception {
        when(lmsClient.validateExternalIdInBusiness(validationRequest(BUSINESS_ID, NEW_EXTERNAL_ID, PARTNER_ID)))
            .thenReturn(BusinessWarehouseValidationStatus.OK);

        BusinessWarehouseRequest request = createMinimalValidRequest().setExternalId(NEW_EXTERNAL_ID);
        updateWarehouse(1L, PARTNER_ID, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateBusinessWarehouseDtoBuilder().externalId(NEW_EXTERNAL_ID).build()
        );
        verify(lmsClient).validateExternalIdInBusiness(validationRequest(BUSINESS_ID, NEW_EXTERNAL_ID, PARTNER_ID));
    }

    @Test
    @DisplayName("Успешная замена с пустым внешнем идентификатором")
    @DatabaseSetup("/controller/business/before/setting.xml")
    void newExternalIdBlank() throws Exception {
        BusinessWarehouseRequest request = createMinimalValidRequest().setExternalId("    ");
        updateWarehouse(1L, PARTNER_ID, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/update_success.json"));

        verify(lmsClient).updateBusinessWarehouse(
            PARTNER_ID,
            updateBusinessWarehouseDtoBuilder().build()
        );
    }

    @Test
    @DatabaseSetup("/controller/business/before/setting.xml")
    @DisplayName("Обновление внешнего идентификатора на невалидный")
    void newInvalidExternalId() throws Exception {
        BusinessWarehouseValidationRequest validationRequest = validationRequest(
            BUSINESS_ID,
            "invalid-ext-id",
            PARTNER_ID
        );
        when(lmsClient.validateExternalIdInBusiness(validationRequest))
            .thenReturn(BusinessWarehouseValidationStatus.INVALID);

        updateWarehouse(1L, 2L, createMinimalValidRequest().setExternalId("invalid-ext-id"))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(externalIdValidationErrorData()));

        verify(lmsClient).validateExternalIdInBusiness(validationRequest);
    }

    @Nonnull
    private PartnerRelationFilter partnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(PARTNER_ID)
            .build();
    }

    @Nonnull
    private PartnerRelationEntityDto partnerRelation() {
        return PartnerRelationEntityDto.newBuilder()
            .shipmentType(ShipmentType.WITHDRAW)
            .enabled(true)
            .fromPartnerId(PARTNER_ID)
            .build();
    }

    @Nonnull
    private ResultActions updateWarehouse(
        long shopId,
        long warehouseId,
        BusinessWarehouseRequest request
    ) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/business/warehouses/" + warehouseId, request)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }
}
