package ru.yandex.market.logistics.nesu.base.partner;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest.PartnerShipmentConfirmRequestBuilder;
import ru.yandex.market.logistics.nesu.client.validation.ValidExternalId;
import ru.yandex.market.logistics.nesu.model.CheckouterFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentConfirmTest extends AbstractPartnerShipmentTest {

    protected static final long VALID_ORDER_ID = 200;
    protected static final long INVALID_ORDER_ID = 100;
    private static final String INVALID_EXTERNAL_ID = String.valueOf(INVALID_ORDER_ID);
    private static final String VALID_EXTERNAL_ID = String.valueOf(VALID_ORDER_ID);

    @BeforeEach
    public void setup() {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));

        TMFactory.mockOutboundUnits(transportManagerClient, List.of(
            RegisterUnitDto.builder()
                .partialIds(List.of(
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value("200").build(),
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value("100").build()
                ))
                .build()
        ));

        mockOutbounds();

        when(checkouterAPI.getOrders(
                any(RequestClientInfo.class),
                any(OrderSearchRequest.class)
            )
        ).thenReturn(new PagedOrders(List.of(), null));

        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
        mockWarehouseFrom();
        mockPartnerRelation();
        mockHandlingTime(0);
    }

    @AfterEach
    public void verifyMocks() {
        verifyNoMoreInteractions(outboundApi);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void requestValidation(
        ValidationErrorDataBuilder error,
        PartnerShipmentConfirmRequestBuilder request
    ) throws Exception {
        confirmShipment(request.build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("partnerShipmentConfirmRequest")));
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "validOrderIds",
                    "Must specify either orderIds or excludedOrderIds",
                    "AssertTrue"
                ),
                PartnerShipmentConfirmRequest.builder()
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "validOrderIds",
                    "Must specify either orderIds or excludedOrderIds",
                    "AssertTrue"
                ),
                PartnerShipmentConfirmRequest.builder().orderIds(List.of(100L)).excludedOrderIds(List.of(200L))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "orderIds",
                    ErrorType.size(1, 10000)
                ),
                PartnerShipmentConfirmRequest.builder().orderIds(List.of())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "orderIds",
                    ErrorType.size(1, 10000)
                ),
                PartnerShipmentConfirmRequest.builder().orderIds(tooManyLongs())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "orderIds",
                    ErrorType.NOT_NULL_ELEMENTS
                ),
                PartnerShipmentConfirmRequest.builder().orderIds(Collections.singletonList(null))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "excludedOrderIds",
                    ErrorType.size(0, 10000)
                ),
                PartnerShipmentConfirmRequest.builder().excludedOrderIds(tooManyLongs())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "excludedOrderIds",
                    ErrorType.NOT_NULL_ELEMENTS
                ),
                PartnerShipmentConfirmRequest.builder().excludedOrderIds(Collections.singletonList(null))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "externalId",
                    ErrorType.size(1, 20)
                ),
                PartnerShipmentConfirmRequest.builder().externalId("").excludedOrderIds(List.of())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "externalId",
                    ErrorType.size(1, 20)
                ),
                PartnerShipmentConfirmRequest.builder()
                    .externalId("x".repeat(21))
                    .excludedOrderIds(List.of())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder(
                    "externalId",
                    "must contain only latin letters, digits, dashes, back and forward slashes, and underscore",
                    "Pattern"
                ).withArguments(Map.of("regexp", ValidExternalId.DEFAULT_EXTERNAL_ID_REGEXP)),
                PartnerShipmentConfirmRequest.builder().externalId("абв").excludedOrderIds(List.of())
            )
        );
    }

    @Nonnull
    private static List<Long> tooManyLongs() {
        return LongStream.range(0, 10001).boxed().collect(Collectors.toList());
    }

    @Test
    @DisplayName("Отгрузка другого партнёра")
    void wrongPartner() throws Exception {
        doReturn(
            TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .partner(TransportationPartnerExtendedInfoDto.builder().id(-TMFactory.PARTNER_ID).build())
                    .build(),
                TMFactory.defaultMovement().build()
            )
        )
            .when(transportManagerClient)
            .getTransportation(TMFactory.SHIPMENT_ID);

        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER_SHIPMENT] with ids [500]"));
    }

    @Test
    @DisplayName("Отгрузка ещё не создана в l4s")
    void notCreated() throws Exception {
        mockEmptyOutbounds();
        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.NOT_CREATED));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Отгрузка уже подтверждена")
    void alreadyConfirmed() throws Exception {
        mockInvalidOutbounds();
        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.ALREADY_CONFIRMED));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Катофф ещё не наступил")
    void cutoffNotReached() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 15).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.CUTOFF_NOT_REACHED));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Учёт времени на сборку")
    void handlingTime() throws Exception {
        mockHandlingTime(1);

        clock.setFixed(
            TMFactory.SHIPMENT_DATE.minusDays(1).atTime(12, 35).toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        mockOrder(List.of());
        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(VALID_ORDER_ID))
                .build()
        ).andExpect(status().isOk());

        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
    }

    @Test
    @DisplayName("В отгрузке нет заказов")
    void noOrders() throws Exception {
        TMFactory.mockOutboundUnits(transportManagerClient, List.of(
            RegisterUnitDto.builder()
                .partialIds(List.of())
                .build()
        ));

        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.NO_ORDERS));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Неверный список включённых заказов")
    void invalidIncludedOrderIds() throws Exception {
        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(300L))
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.fieldError(
                "orderIds",
                "must be a part of all shipment orders",
                "partnerShipmentConfirmRequest",
                "ValidOrderIds"
            )));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Неверный список исключённых заказов")
    void invalidExcludedOrderIds() throws Exception {
        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .excludedOrderIds(List.of(300L))
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.fieldError(
                "excludedOrderIds",
                "must be a part of all shipment orders",
                "partnerShipmentConfirmRequest",
                "ValidOrderIds"
            )));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Отгрузка второго партнёра")
    void secondPartnerShipment() throws Exception {
        doReturn(TMFactory.transportation(
            TMFactory.defaultOutbound()
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(TMFactory.SECOND_PARTNER_ID)
                        .build()
                )
                .registers(List.of(TMFactory.outboundRegister().build()))
                .build(),
            TMFactory.defaultMovement().build()
        )).when(transportManagerClient).getTransportation(TMFactory.SHIPMENT_ID);

        mockPartnerRelation(TMFactory.SECOND_PARTNER_ID);
        mockHandlingTime(0, TMFactory.SECOND_PARTNER_ID);

        String shipmentExternalId = "shipment-external-id";
        mockOrder(List.of());

        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .externalId(shipmentExternalId)
                .orderIds(List.of(VALID_ORDER_ID))
                .build()
        ).andExpect(status().isOk());

        verifyConfirmOutbound(shipmentExternalId);
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Список включённых заказов")
    void includedOrderIds() throws Exception {
        String shipmentExternalId = "shipment-external-id";
        mockOrder(List.of());

        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .externalId(shipmentExternalId)
                .orderIds(List.of(VALID_ORDER_ID))
                .build()
        ).andExpect(status().isOk());

        verifyConfirmOutbound(shipmentExternalId);
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Список исключённых заказов")
    void excludedOrderIds() throws Exception {
        mockOrder(List.of());
        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .excludedOrderIds(List.of(INVALID_ORDER_ID))
                .build()
        ).andExpect(status().isOk());

        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
    }

    @Test
    @DisplayName("В отгрузке не осталось заказов после исключения")
    void noOrdersAfterExclusion() throws Exception {
        confirmShipment(defaultConfirmRequest().excludedOrderIds(List.of(INVALID_ORDER_ID, VALID_ORDER_ID)).build())
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.NO_ORDERS));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Отменённый заказ явно включен")
    void cancelledOrderInclude() throws Exception {
        mockOrders(
            Set.of(INVALID_EXTERNAL_ID, VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(INVALID_EXTERNAL_ID)
                    .setStatus(OrderStatus.CANCELLED),
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setUnits(defaultPlace())
            )
        );
        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(INVALID_ORDER_ID, VALID_ORDER_ID))
                .build()
        )
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.INVALID_ORDERS))
            .andExpect(jsonPath("invalidOrders.CANCELLED").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("invalidOrders.CANCELLED[0]").value(INVALID_ORDER_ID));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Отменённый заказ не в исключенных")
    void cancelledOrderExclude() throws Exception {
        mockOrders(
            Set.of(INVALID_EXTERNAL_ID, VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(INVALID_EXTERNAL_ID)
                    .setStatus(OrderStatus.CANCELLED),
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setItems(List.of())
                    .setUnits(defaultPlace())
            )
        );

        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isOk());

        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Заказ без коробок явно включен")
    void noPlacesOrderInclude() throws Exception {
        mockOrders(
            Set.of(INVALID_EXTERNAL_ID, VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(INVALID_EXTERNAL_ID)
                    .setUnits(List.of()),
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setUnits(defaultPlace())
            )
        );

        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(INVALID_ORDER_ID, VALID_ORDER_ID))
                .build()
        )
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.INVALID_ORDERS))
            .andExpect(jsonPath("invalidOrders.NO_PLACES").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("invalidOrders.NO_PLACES[0]").value(INVALID_ORDER_ID));

        verifyGetOutbound();
    }

    @Test
    @DisplayName("Заказ без коробок не в исключенных")
    void noPlacesOrderExclude() throws Exception {
        mockOrders(
            Set.of(INVALID_EXTERNAL_ID, VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(INVALID_EXTERNAL_ID)
                    .setUnits(List.of()),
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setItems(List.of())
                    .setUnits(defaultPlace())
            )
        );

        confirmShipment(defaultConfirmRequest().build())
            .andExpect(status().isOk());

        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Коробки из чекаутера")
    void checkouterPlaces() throws Exception {
        mockOrders(
            Set.of(VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setItems(List.of())
                    .setUnits(List.of())
            )
        );

        RequestClientInfo requestClientInfo = RequestClientInfo.builder(ClientRole.SHOP)
            .withClientIds(Set.of(SHOP_ID))
            .build();
        OrderSearchRequest orderSearchRequest = CheckouterFactory.checkouterSearchRequest(VALID_ORDER_ID);
        doReturn(new PagedOrders(
            List.of(CheckouterFactory.checkouterOrder(VALID_ORDER_ID, OrderSubstatus.READY_TO_SHIP)),
            null
        ))
            .when(checkouterAPI)
            .getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest));

        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(VALID_ORDER_ID))
                .build()
        ).andExpect(status().isOk());


        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
        verify(checkouterAPI).getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest));
    }

    @Test
    @DisplayName("Коробки из чекаутера. Список shopId")
    void checkouterPlacesConfirmWithShopIds() throws Exception {
        mockOrders(
            Set.of(VALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(VALID_EXTERNAL_ID)
                    .setItems(List.of())
                    .setUnits(List.of())
            )
        );

        RequestClientInfo requestClientInfo = RequestClientInfo.builder(ClientRole.SHOP)
            .withClientIds(Set.of(SHOP_ID, SECOND_SHOP_ID))
            .build();
        OrderSearchRequest orderSearchRequest = CheckouterFactory.checkouterSearchRequest(VALID_ORDER_ID);
        doReturn(new PagedOrders(
            List.of(
                CheckouterFactory.checkouterOrder(VALID_ORDER_ID, OrderSubstatus.READY_TO_SHIP),
                CheckouterFactory.checkouterOrder(INVALID_ORDER_ID, OrderSubstatus.READY_TO_SHIP)
            ),
            null
        ))
            .when(checkouterAPI)
            .getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest));

        mockMvc.perform(
            request(HttpMethod.POST, url(TMFactory.SHIPMENT_ID), orderSearchRequest)
                .param("userId", "100")
                .param("shopIds", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
        ).andExpect(status().isOk());

        verifyConfirmOutbound(TMFactory.outboundId());
        verifyGetOutbound();
        verify(checkouterAPI).getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest));
    }

    @Test
    @DisplayName("Слишком ранний статус заказа в чекаутере")
    void invalidCheckouterStatus() throws Exception {
        mockOrders(
            Set.of(INVALID_EXTERNAL_ID),
            List.of(
                new OrderDto()
                    .setExternalId(INVALID_EXTERNAL_ID)
                    .setItems(List.of())
                    .setUnits(List.of())
            )
        );

        OrderSearchRequest orderSearchRequest = CheckouterFactory.checkouterSearchRequest(VALID_ORDER_ID);
        doReturn(new PagedOrders(
            List.of(CheckouterFactory.checkouterOrder(INVALID_ORDER_ID, OrderSubstatus.PACKAGING)),
            null
        ))
            .when(checkouterAPI)
            .getOrdersByShop(safeRefEq(orderSearchRequest), eq(SHOP_ID));

        confirmShipment(
            PartnerShipmentConfirmRequest.builder()
                .orderIds(List.of(INVALID_ORDER_ID))
                .build()
        )
            .andExpect(status().isPreconditionFailed())
            .andExpect(confirmationValidationError())
            .andExpect(errorType(PartnerShipmentConfirmationErrorType.INVALID_ORDERS))
            .andExpect(jsonPath("invalidOrders.NO_PLACES").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("invalidOrders.NO_PLACES[0]").value(INVALID_ORDER_ID));

        verifyGetOutbound();
    }

    @Nonnull
    private PartnerShipmentConfirmRequestBuilder defaultConfirmRequest() {
        return PartnerShipmentConfirmRequest.builder().excludedOrderIds(List.of());
    }

    @Nonnull
    private List<StorageUnitDto> defaultPlace() {
        return List.of(OrderDtoFactory.createPlaceUnitBuilder().dimensions(null).build());
    }

    private void mockOrder(List<ItemDto> items) {
        String externalId = String.valueOf(VALID_ORDER_ID);
        mockOrders(
            Set.of(externalId),
            List.of(
                new OrderDto()
                    .setExternalId(externalId)
                    .setItems(items)
                    .setUnits(defaultPlace())
            )
        );
    }

    private void mockOrders(Set<String> externalIds, List<OrderDto> orders) {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder()
                .platformClientId(1L)
                .externalIds(externalIds)
                .build(),
            Pageable.unpaged()
        )).thenReturn(new PageResult<OrderDto>().setData(orders));
    }

    @Nonnull
    private ResultMatcher confirmationValidationError() {
        return jsonPath("type").value("PARTNER_SHIPMENT_CONFIRMATION_VALIDATION");
    }

    @Nonnull
    private ResultMatcher errorType(PartnerShipmentConfirmationErrorType code) {
        return jsonPath("error").value(code.name());
    }

    @Nonnull
    private ResultActions confirmShipment(PartnerShipmentConfirmRequest request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, url(TMFactory.SHIPMENT_ID), request)
                .param("userId", "100")
                .param("shopId", String.valueOf(SHOP_ID))
        );
    }

    @Nonnull
    protected abstract String url(long shipmentId);

    protected abstract void verifyGetOutbound();

    protected abstract void verifyConfirmOutbound(String externalId);

    protected abstract void mockOutbounds();

    protected abstract void mockInvalidOutbounds();

    protected abstract void mockEmptyOutbounds();
}
