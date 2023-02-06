package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseRequestBase;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.scheduleDay;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddress;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressMinimalBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseContact;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание склада для DAAS-магазина")
public abstract class AbstractShopWarehouseCreateTest<R extends ShopWarehouseRequestBase>
    extends AbstractShopWarehouseControllerTest {

    @DisplayName("Успешное создание склада с указанием всех доступных полей")
    @Test
    void createShopWarehouseSuccessful() throws Exception {
        when(lmsClient.createLogisticsPoint(any())).thenReturn(createLogisticsPointResponse());

        createWarehouse(1L, createRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verifyLmsCreateLogisticsPointCall(
            createBaseLogisticsPointCreateRequestBuilder()
                .address(addressWithoutCalculatedFields().build())
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .phones(createPhoneDto())
                .contact(LmsFactory.createContactDto())
                .handlingTime(ONE_DAY)
                .businessId(42L)
                .build()
        );
    }

    @DisplayName("Успешное создание склада с указанием координат")
    @Test
    void createShopWarehouseWithCoordinates() throws Exception {
        Address address = addressWithoutCalculatedFields()
            .latitude(BigDecimal.valueOf(100))
            .longitude(BigDecimal.valueOf(200))
            .build();
        when(lmsClient.createLogisticsPoint(any()))
            .thenReturn(logisticsPointResponseBuilder().address(address).build());

        R request = createRequest();
        request
            .setAddress(
                warehouseAddressBuilder()
                    .latitude(BigDecimal.valueOf(100))
                    .longitude(BigDecimal.valueOf(200))
                    .build()
            );
        createWarehouse(1L, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_with_coordinates.json"));

        verifyLmsCreateLogisticsPointCall(
            createBaseLogisticsPointCreateRequestBuilder()
                .address(address)
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .phones(createPhoneDto())
                .contact(LmsFactory.createContactDto())
                .handlingTime(ONE_DAY)
                .businessId(42L)
                .build()
        );
    }

    @DisplayName("Успешное создание склада с незаполненными опциональными полями")
    @Test
    void createShopWarehouseWithOnlyRequiredFieldsSuccessful() throws Exception {
        when(lmsClient.createLogisticsPoint(any())).thenReturn(createMinimalLogisticsPointResponse());

        createWarehouse(1L, createMinimalValidRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_with_only_required_fields.json"));

        verifyLmsCreateLogisticsPointCall(
            createBaseLogisticsPointCreateRequestBuilder()
                .address(
                    Address.newBuilder()
                        .locationId(65)
                        .settlement("Новосибирск")
                        .postCode("649220")
                        .street("Николаева")
                        .region("Новосибирская область")
                        .house("11")
                        .build()
                )
                .schedule(LmsFactory.createScheduleDayDtoSetWithSize(1))
                .businessId(42L)
                .build()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "bodyValidationSourceBase",
        "bodyValidationSource",
    })
    @DisplayName("Валидация тела запроса")
    void bodyValidation(
        ValidationErrorData.ValidationErrorDataBuilder error,
        Consumer<ShopWarehouseRequestBase> requestModifier
    ) throws Exception {
        R request = createMinimalValidRequest();
        requestModifier.accept(request);

        createWarehouse(1L, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject(getObjectName())));
    }

    @Nonnull
    private static Stream<Arguments> bodyValidationSource() {
        return Stream.<Pair<ValidationErrorData.ValidationErrorDataBuilder, Consumer<ShopWarehouseRequestBase>>>of(
            Pair.of(
                fieldErrorBuilder("address", ErrorType.NOT_NULL),
                rq -> rq.setAddress(null)
            ),
            Pair.of(
                fieldErrorBuilder("name", ErrorType.NOT_BLANK),
                rq -> rq.setName(null)
            ),
            Pair.of(
                fieldErrorBuilder("name", ErrorType.NOT_BLANK),
                rq -> rq.setName(" \t\n ")
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @DisplayName("Попытка создать склад для несуществующего магазина")
    @Test
    void createShopWarehouseOfNonexistentShop() throws Exception {
        createWarehouse(10L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @DisplayName("Попытка создать склад для не-DAAS магазина")
    @Test
    void createShopWarehouseForNonDaas() throws Exception {
        createWarehouse(4L, createRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unsupported shop role. id: 4, role: SUPPLIER"));
    }

    @Nonnull
    protected R createRequest() {
        R request = createMinimalValidRequest();
        request.setAddress(warehouseAddress())
            .setContact(warehouseContact())
            .setHandlingTimeDays(1L);

        return request;
    }

    @Nonnull
    private LogisticsPointCreateRequest.Builder createBaseLogisticsPointCreateRequestBuilder() {
        return LogisticsPointCreateRequest.newBuilder()
            .externalId("externalId")
            .type(PointType.WAREHOUSE)
            .name("Имя склада")
            .active(true)
            .isFrozen(false);
    }

    @Nonnull
    protected abstract ResultActions createWarehouse(Long shopId, R request) throws Exception;

    @Nonnull
    protected R createMinimalValidRequest() {
        R request = createMinimalRequest();
        request.setName("Имя склада")
            .setAddress(warehouseAddressMinimalBuilder().build())
            .setSchedule(Set.of(scheduleDay()));
        return request;
    }

    @Nonnull
    protected abstract R createMinimalRequest();

    @Nonnull
    protected abstract String getObjectName();

    private void verifyLmsCreateLogisticsPointCall(LogisticsPointCreateRequest logisticsPointCreateRequest) {
        ArgumentCaptor<LogisticsPointCreateRequest> requestCaptor =
            ArgumentCaptor.forClass(LogisticsPointCreateRequest.class);
        verify(lmsClient).createLogisticsPoint(requestCaptor.capture());

        LogisticsPointCreateRequest request = requestCaptor.getValue();

        softly.assertThat(validator.validate(request)).isEmpty();
        softly.assertThat(request).isEqualTo(logisticsPointCreateRequest);
    }
}
