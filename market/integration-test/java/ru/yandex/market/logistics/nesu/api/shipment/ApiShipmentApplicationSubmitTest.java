package ru.yandex.market.logistics.nesu.api.shipment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ShipmentConfirmationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.error.EntityError;
import ru.yandex.market.logistics.lom.model.error.EntityError.ErrorCode;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.api.model.shipment.ShipmentApplicationsSubmitRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение заявок в Open API")
@DatabaseSetup("/repository/shipments/database_prepare.xml")
class ApiShipmentApplicationSubmitTest extends AbstractApiTest {

    private static final long SHOP_ID = 1L;
    private static final long APPLICATION_ID = 10L;
    public static final long MARKET_ID = 100L;

    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, SHOP_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("requestValidationSource")
    @DisplayName("Валидация запроса")
    void requestValidation(
        ValidationErrorData error,
        UnaryOperator<ShipmentApplicationsSubmitRequest> request
    ) throws Exception {
        submit(request.apply(defaultRequest()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> requestValidationSource() {
        return Stream.of(
            Arguments.of(
                fieldError(
                    "cabinetId",
                    "must not be null",
                    "shipmentApplicationsSubmitRequest",
                    "NotNull"
                ),
                (UnaryOperator<ShipmentApplicationsSubmitRequest>) r -> r.setCabinetId(null)
            ),
            Arguments.of(
                fieldError(
                    "shipmentApplicationIds",
                    "must not be null",
                    "shipmentApplicationsSubmitRequest",
                    "NotNull"
                ),
                (UnaryOperator<ShipmentApplicationsSubmitRequest>) r -> r.setShipmentApplicationIds(null)
            ),
            Arguments.of(
                fieldError(
                    "shipmentApplicationIds",
                    "must not contain nulls",
                    "shipmentApplicationsSubmitRequest",
                    "NotNullElements"
                ),
                (UnaryOperator<ShipmentApplicationsSubmitRequest>) r -> r
                    .setShipmentApplicationIds(Collections.singletonList(null))
            ),
            Arguments.of(
                fieldError(
                    "shipmentApplicationIds",
                    "size must be between 1 and 100",
                    "shipmentApplicationsSubmitRequest",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                (UnaryOperator<ShipmentApplicationsSubmitRequest>) r -> r.setShipmentApplicationIds(List.of())
            ),
            Arguments.of(
                fieldError(
                    "shipmentApplicationIds",
                    "size must be between 1 and 100",
                    "shipmentApplicationsSubmitRequest",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                (UnaryOperator<ShipmentApplicationsSubmitRequest>) r -> r
                    .setShipmentApplicationIds(LongStream.range(0, 101).boxed().collect(Collectors.toList()))
            )
        );
    }

    @Test
    @DisplayName("Неизвестный магазин")
    void unknownShop() throws Exception {
        submit(defaultRequest().setCabinetId(-1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [-1]"));
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        submit(defaultRequest().setCabinetId(2L))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Отключенный магазин")
    @DatabaseSetup(value = "/repository/shop/before/disabled_shop.xml", type = DatabaseOperation.UPDATE)
    void disabledShop() throws Exception {
        submit(defaultRequest())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Заявка не найдена")
    void shipmentNotFound() throws Exception {
        when(lomClient.searchShipments(safeRefEq(searchFilter(APPLICATION_ID)), any()))
            .thenReturn(new PageResult<ShipmentSearchDto>().setData(List.of()));

        submit(defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipment/application/submit/not_found.json"));
    }

    @Test
    @DisplayName("Ошибки валидации")
    void validationError() throws Exception {
        long secondApplication = 11L;

        mockSearch(APPLICATION_ID, secondApplication);

        when(lomClient.confirmShipmentApplication(APPLICATION_ID))
            .thenReturn(ShipmentConfirmationDto.builder().errors(List.of(
                EntityError.builder()
                    .id(20L)
                    .entityType(EntityType.ORDER)
                    .errorCode(ErrorCode.DRAFT)
                    .build(),
                EntityError.builder()
                    .id(APPLICATION_ID)
                    .entityType(EntityType.SHIPMENT_APPLICATION)
                    .errorCode(ErrorCode.INVALID_STATUS)
                    .build()
            )).build());

        when(lomClient.confirmShipmentApplication(secondApplication))
            .thenReturn(ShipmentConfirmationDto.builder().errors(List.of(
                EntityError.builder()
                    .id(secondApplication)
                    .entityType(EntityType.SHIPMENT_APPLICATION)
                    .errorCode(ErrorCode.CUTOFF_REACHED)
                    .build()
            )).build());

        submit(defaultRequest().setShipmentApplicationIds(List.of(APPLICATION_ID, secondApplication)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipment/application/submit/validation_error.json"));
    }

    @Test
    @DisplayName("Ошибка подтверждения")
    void error() throws Exception {
        mockSearch(APPLICATION_ID);

        doThrow(new RuntimeException("test exception"))
            .when(lomClient).confirmShipmentApplication(APPLICATION_ID);

        submit(defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipment/application/submit/error.json"));

    }

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        mockSearch(APPLICATION_ID);

        when(lomClient.confirmShipmentApplication(APPLICATION_ID))
            .thenReturn(ShipmentConfirmationDto.builder().build());

        submit(defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipment/application/submit/success.json"));
    }

    private void mockSearch(Long... applicationIds) {
        when(lomClient.searchShipments(safeRefEq(searchFilter(applicationIds)), any()))
            .thenReturn(new PageResult<ShipmentSearchDto>().setData(
                Stream.of(applicationIds)
                    .map(id -> ShipmentSearchDto.builder().applicationId(id).build()).collect(
                    Collectors.toList()))
            );
    }

    @Nonnull
    private ShipmentSearchFilter searchFilter(Long... applicationIds) {
        return ShipmentSearchFilter.builder()
            .marketIdFrom(MARKET_ID)
            .withApplication(true)
            .shipmentApplicationIds(Set.of(applicationIds))
            .build();
    }

    @Nonnull
    private static ShipmentApplicationsSubmitRequest defaultRequest() {
        return new ShipmentApplicationsSubmitRequest()
            .setCabinetId(SHOP_ID)
            .setShipmentApplicationIds(List.of(APPLICATION_ID));
    }

    @Nonnull
    private ResultActions submit(ShipmentApplicationsSubmitRequest request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/api/shipments/applications/submit", request));
    }

}
