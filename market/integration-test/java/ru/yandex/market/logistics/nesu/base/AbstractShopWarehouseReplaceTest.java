package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseUpdateRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.createSchedule;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddress;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseContact;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Замена склада")
public abstract class AbstractShopWarehouseReplaceTest<R extends ShopWarehouseUpdateRequest>
    extends AbstractShopWarehouseControllerTest {

    @Autowired
    protected MbiApiClient mbiApiClient;

    @BeforeEach
    void setupMocks() {
        LogisticsPointResponse logisticsPointResponse = createLogisticsPointResponse();

        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(logisticsPointResponse));
        when(lmsClient.updateLogisticsPoint(eq(1L), any())).thenReturn(logisticsPointResponse);
    }

    @Test
    @DisplayName("Успешная замена склада")
    void success() throws Exception {
        mockAccess(1L);
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        updateWarehouse(1L, createRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .handlingTime(ONE_DAY)
                .build()
        );
    }

    @Test
    @DisplayName("Замена склада с совпадающим businessId")
    void correctBusinessId() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        mockAccess(1L);

        R request = createRequest();
        request.setSchedule(createSchedule(6));
        updateWarehouse(1L, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(6))
                .businessId(42L)
                .handlingTime(ONE_DAY)
                .build()
        );
    }

    @Test
    @DisplayName("Успешная замена склада без указания координат")
    void successWithoutCoordinates() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        mockAccess(1L);
        R request = createRequest();
        request.setAddress(warehouseAddress());
        updateWarehouse(1L, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .address(addressWithoutCalculatedFields().build())
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .handlingTime(ONE_DAY)
                .build()
        );
    }

    @Test
    @DisplayName("Успешная замена склада без указания городского округа")
    void noSubRegion() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        mockAccess(1L);
        R request = createRequest();
        request.setAddress(
            warehouseAddressBuilder()
                .latitude(new BigDecimal("54.858076"))
                .longitude(new BigDecimal("83.110392"))
                .subRegion(null)
                .build()
        );
        updateWarehouse(1L, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .handlingTime(ONE_DAY)
                .build()
        );
    }

    @Test
    @DisplayName("Успешная замена со сменой адреса, координаты и городской округ не сохраняются")
    void newAddressNoCoordinates() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        mockAccess(1L);
        R request = createRequest();
        request.setAddress(warehouseAddressBuilder().house("12").build());
        updateWarehouse(1L, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .address(addressWithoutCalculatedFields().house("12").build())
                .handlingTime(ONE_DAY)
                .build()
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {3, 4})
    @DisplayName("Попытка замены склада для CPA магазина")
    void cpaSuccessReplace(long shopId) throws Exception {
        mockAccess(shopId);
        R request = createRequest();
        request.setSchedule(createSchedule(5));

        updateWarehouse(shopId, request)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                String.format("Unsupported shop role. id: %d, role: %s", shopId, shopId == 3 ? "DROPSHIP" : "SUPPLIER")
            ));
    }

    @Test
    @DisplayName("Изменение только расписания склада, минимально возможный запрос")
    void scheduleOnlyChange() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createDaasLogisticsPointResponse()));
        mockAccess(1L);
        updateWarehouse(1L, createMinimalValidRequest())
            .andExpect(status().isOk());

        verifyGetWarehouse();
        verifyUpdateWarehouse(
            createBaseLogisticsPointUpdateRequestBuilder()
                .externalId("externalId")
                .address(addressBuilder().comment(null).build())
                .phones(null)
                .contact(null)
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .build()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("bodyValidationSourceBase")
    @DisplayName("Валидация тела запроса")
    void bodyValidation(
        ValidationErrorData.ValidationErrorDataBuilder error,
        Consumer<R> requestModifier
    ) throws Exception {
        mockAccess(1L);
        R request = createMinimalValidRequest();
        requestModifier.accept(request);

        updateWarehouse(1L, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject(getObjectName())));
    }

    @Test
    @DisplayName("Заменить склад несуществующего магазина")
    void nonexistentShop() throws Exception {
        mockAccess(10L);
        updateWarehouse(10L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @Test
    @DisplayName("Заменить склад, который принадлежит другому партнеру")
    void otherPartner() throws Exception {
        mockAccess(2L);
        updateWarehouse(2L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyGetWarehouse();
    }

    @Test
    @DisplayName("Попытка заменить склад с другим businessId")
    void wrongBusinessId() throws Exception {
        mockAccess(1L);
        doReturn(Optional.of(
            logisticsPointResponseBuilder().businessId(41L).build()
        )).when(lmsClient).getLogisticsPoint(1L);

        updateWarehouse(1L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyGetWarehouse();
    }

    @Test
    @DisplayName("Заменить несуществующий склад")
    void nonexistentWarehouse() throws Exception {
        mockAccess(1L);
        doReturn(Optional.empty()).when(lmsClient).getLogisticsPoint(1L);

        updateWarehouse(1L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyGetWarehouse();
    }

    @Nonnull
    protected R createRequest() {
        R request = createMinimalValidRequest();
        request.setAddressComment("как проехать")
            .setAddress(
                warehouseAddressBuilder()
                    .latitude(new BigDecimal("54.858076"))
                    .longitude(new BigDecimal("83.110392"))
                    .build()
            )
            .setName("Имя склада")
            .setHandlingTimeDays(1L)
            .setContact(warehouseContact());
        return request;
    }

    @Nonnull
    protected abstract R createMinimalValidRequest();

    @Nonnull
    protected abstract ResultActions updateWarehouse(Long shopId, R request) throws Exception;

    @Nonnull
    protected abstract String getObjectName();

    @Nonnull
    private LogisticsPointUpdateRequest.Builder createBaseLogisticsPointUpdateRequestBuilder() {
        return LogisticsPointUpdateRequest.newBuilder()
            .name("Имя склада")
            .pickupPointType(null)
            .address(createAddressDto())
            .phones(createPhoneDto())
            .active(true)
            .contact(LmsFactory.createContactDto())
            .cashAllowed(true)
            .prepayAllowed(false)
            .cardAllowed(false)
            .returnAllowed(false)
            .instruction("test_instructions")
            .services(Set.of(new Service(
                ServiceCodeName.CASH_SERVICE,
                true,
                "test_service_name",
                "description"
            )))
            .storagePeriod(3)
            .maxWeight(20.0)
            .maxLength(20)
            .maxWidth(10)
            .maxHeight(15)
            .maxSidesSum(100)
            .isFrozen(false)
            .businessId(42L)
            .marketBranded(false);
    }

    private void verifyGetWarehouse() {
        verify(lmsClient).getLogisticsPoint(1L);
    }

    private void verifyUpdateWarehouse(LogisticsPointUpdateRequest logisticsPointUpdateRequest) {
        ArgumentCaptor<LogisticsPointUpdateRequest> requestCaptor =
            ArgumentCaptor.forClass(LogisticsPointUpdateRequest.class);
        verify(lmsClient).updateLogisticsPoint(eq(1L), requestCaptor.capture());

        LogisticsPointUpdateRequest request = requestCaptor.getValue();
        softly.assertThat(validator.validate(request)).isEmpty();
        softly.assertThat(request).isEqualTo(logisticsPointUpdateRequest);
    }

    protected void mockAccess(Long allowedId) {
        //do nothing
    }
}
