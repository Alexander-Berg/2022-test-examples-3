package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.queue.payload.itemsremoval.OrderItemsRemovalPayload;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderItemsRequest;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmOrderChangedByPartnerRequest;
import ru.yandex.market.logistics.lom.model.dto.change.DenyOrderChangedByPartnerRequest;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.order_service.client.api.OrdersLinesCommonApi;
import ru.yandex.market.order_service.client.model.ChangeOrderLinesRequest;
import ru.yandex.market.order_service.client.model.ChangeOrderLinesResponse;
import ru.yandex.market.order_service.client.model.ChangeOrderLinesResponseDto;
import ru.yandex.market.order_service.client.model.ChangeRequestStatusType;
import ru.yandex.market.order_service.client.model.ChangedOrderItemDto;
import ru.yandex.market.order_service.client.model.OrderLineChange;
import ru.yandex.market.order_service.client.model.UpdateOrderReasonType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Удаление товаров в LOM")
@ParametersAreNonnullByDefault
class OrderItemsRemovalProcessorTest extends AbstractIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private OrderItemsRemovalProcessor orderItemsRemovalProcessor;
    @Autowired
    private OrdersLinesCommonApi ordersLinesCommonApi;
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешная обработка заявки на изменение заказа - изменение выполнено")
    void changeRequestProcessing() {
        mockMbiOrderService(updateItemsRequest());
        orderItemsRemovalProcessor.execute(defaultPayloadBuilder().build());
        verify(lomClient).processChangeOrderRequest(1L, confirmRequest());
    }

    @Test
    @DisplayName("Успешная обработка заявки на изменение заказа - изменение выполнено, товары с кол-вом 0 пропущены")
    void changeRequestProcessingItemCountIsZero() {
        mockMbiOrderService(updateItemsRequest(
            List.of(
                new ChangedOrderItemDto()
                    .partnerId(101L)
                    .price(BigDecimal.TEN)
                    .count(3)
                    .offerName("offerName")
                    .initialCount(5)
                    .shopSku("100500672239"),
                new ChangedOrderItemDto()
                    .partnerId(101L)
                    .price(BigDecimal.TEN)
                    .count(0)
                    .offerName("noSuchOffer")
                    .initialCount(5)
                    .shopSku("100500672239")
            )
        ));
        orderItemsRemovalProcessor.execute(defaultPayloadBuilder().build());
        verify(lomClient).processChangeOrderRequest(1L, confirmRequest());
    }

    @Test
    @DisplayName("Успешная обработка заявки на изменение заказа - изменение не выполнено")
    void changeRequestRejected() {
        mockMbiOrderService(new ChangeOrderLinesResponse().result(
            new ChangeOrderLinesResponseDto().changeRequestStatus(ChangeRequestStatusType.REJECTED)
        ));
        orderItemsRemovalProcessor.execute(defaultPayloadBuilder().build());
        verify(lomClient).processChangeOrderRequest(1L, new DenyOrderChangedByPartnerRequest());
    }

    @DisplayName("Валидация payload")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void invalidPayload(
        @SuppressWarnings("unused") String displayName,
        OrderItemsRemovalPayload payload,
        String field,
        String errorMessage
    ) {
        softly.assertThatThrownBy(() -> orderItemsRemovalProcessor.execute(payload))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interpolatedMessage='%s'".formatted(errorMessage))
            .hasMessageContaining("propertyPath=%s".formatted(field));
    }

    @Nonnull
    static Stream<Arguments> invalidPayload() {
        return Stream.of(
            payload(),
            dimensions()
        )
            .flatMap(Function.identity());
    }

    @Nonnull
    static Stream<Arguments> payload() {
        return Stream.of(
            Arguments.of(
                "no externalId",
                defaultPayloadBuilder().externalId(null).build(),
                "externalId",
                "must not be null"
            ),
            Arguments.of(
                "no barcode",
                defaultPayloadBuilder().barcode(null).build(),
                "barcode",
                "must not be blank"
            ),
            Arguments.of(
                "blank barcode",
                defaultPayloadBuilder().barcode("").build(),
                "barcode",
                "must not be blank"
            ),
            Arguments.of(
                "no shopId",
                defaultPayloadBuilder().shopId(null).build(),
                "shopId",
                "must not be null"
            ),
            Arguments.of(
                "no itemsPayload",
                defaultPayloadBuilder().remainedItemsPayload(null).build(),
                "remainedItemsPayload",
                "must not be null"
            ),
            Arguments.of(
                "no itemInfo",
                defaultPayloadBuilder().itemInfo(null).build(),
                "itemInfo",
                "must not be empty"
            ),
            Arguments.of(
                "empty itemInfo",
                defaultPayloadBuilder().itemInfo(Map.of()).build(),
                "itemInfo",
                "must not be empty"
            ),
            Arguments.of(
                "no lomChangeOrderRequestId",
                defaultPayloadBuilder().lomChangeOrderRequestId(null).build(),
                "lomChangeOrderRequestId",
                "must not be null"
            ),
            Arguments.of(
                "no tariffId",
                defaultPayloadBuilder().tariffId(null).build(),
                "tariffId",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> dimensions() {
        return Stream.of(
            Quadruple.of(
                "no weight",
                defaultDimensionsBuilder().weight(null),
                "itemInfo[100500672239].dimensions.weight",
                "must not be null"
            ),
            Quadruple.of(
                "zero weight",
                defaultDimensionsBuilder().weight(BigDecimal.ZERO),
                "itemInfo[100500672239].dimensions.weight",
                "must be greater than 0"
            ),
            Quadruple.of(
                "no length",
                defaultDimensionsBuilder().length(null),
                "itemInfo[100500672239].dimensions.length",
                "must not be null"
            ),
            Quadruple.of(
                "zero length",
                defaultDimensionsBuilder().length(0),
                "itemInfo[100500672239].dimensions.length",
                "must be greater than 0"
            ),
            Quadruple.of(
                "no width",
                defaultDimensionsBuilder().width(null),
                "itemInfo[100500672239].dimensions.width",
                "must not be null"
            ),
            Quadruple.of(
                "zero width",
                defaultDimensionsBuilder().width(0),
                "itemInfo[100500672239].dimensions.width",
                "must be greater than 0"
            ),
            Quadruple.of(
                "no height",
                defaultDimensionsBuilder().height(null),
                "itemInfo[100500672239].dimensions.height",
                "must not be null"
            ),
            Quadruple.of(
                "zero height",
                defaultDimensionsBuilder().height(0),
                "itemInfo[100500672239].dimensions.height",
                "must be greater than 0"
            )
        )
            .map(quadruple -> Arguments.of(
                quadruple.getFirst(),
                defaultPayloadBuilder().itemInfo(Map.of(
                    "100500672239",
                    defaultItemInfoBuilder().dimensions(quadruple.getSecond().build()).build()
                )).build(),
                quadruple.getThird(),
                quadruple.getFourth()
            ));
    }

    @Test
    @DisplayName("Не пришли новые данные товарах")
    void noNewItems() {
        mockMbiOrderService(new ChangeOrderLinesResponse().result(
            new ChangeOrderLinesResponseDto().changeRequestStatus(ChangeRequestStatusType.PROCESSING)
        ));
        softly.assertThatThrownBy(() -> orderItemsRemovalProcessor.execute(defaultPayloadBuilder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No items to update in LOM");
    }

    @Test
    @DisplayName("Пришли данные по неизвестному товару")
    void unknownSsku() {
        mockMbiOrderService(new ChangeOrderLinesResponse().result(
            new ChangeOrderLinesResponseDto()
                .items(List.of(new ChangedOrderItemDto().price(BigDecimal.TEN).shopSku("unknown").count(1)))
                .changeRequestId(123L)
                .changeRequestStatus(ChangeRequestStatusType.PROCESSING)
        ));
        softly.assertThatThrownBy(() -> orderItemsRemovalProcessor.execute(defaultPayloadBuilder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No additional info for item unknown");
    }

    @Nonnull
    @SneakyThrows
    private static OrderItemsRemovalPayload.OrderItemsRemovalPayloadBuilder<?, ?> defaultPayloadBuilder() {
        return OrderItemsRemovalPayload.builder()
            .externalId(2L)
            .remainedItemsPayload(OBJECT_MAPPER.readValue(
                "[{\"article\":\"100500672239\",\"count\":3,\"reason\":null,\"vendorId\":101}]",
                JsonNode.class
            ))
            .barcode("FF-102")
            .shopId(101L)
            .lomChangeOrderRequestId(1L)
            .itemInfo(Map.of(
                "100500672239",
                defaultItemInfoBuilder().build()
            ))
            .tariffId(127L);
    }

    @Nonnull
    @SneakyThrows
    private static OrderItemsRemovalPayload.ItemInfo.ItemInfoBuilder defaultItemInfoBuilder() {
        return OrderItemsRemovalPayload.ItemInfo.builder()
            .msku(123L)
            .dimensions(defaultDimensionsBuilder().build())
            .vatType("NO_VAT")
            .cargoTypes(List.of(10));
    }

    @Nonnull
    private static OrderItemsRemovalPayload.DimensionsPayload.DimensionsPayloadBuilder defaultDimensionsBuilder() {
        return OrderItemsRemovalPayload.DimensionsPayload.builder()
            .weight(BigDecimal.valueOf(4.9))
            .length(1)
            .width(2)
            .height(3);
    }

    @Nonnull
    private ChangeOrderLinesResponse updateItemsRequest() {
        return updateItemsRequest(
            List.of(
                new ChangedOrderItemDto()
                    .partnerId(101L)
                    .price(BigDecimal.TEN)
                    .count(3)
                    .offerName("offerName")
                    .initialCount(5)
                    .shopSku("100500672239")
            )
        );
    }

    @Nonnull
    private ChangeOrderLinesResponse updateItemsRequest(List<ChangedOrderItemDto> newItems) {
        return new ChangeOrderLinesResponse()
            .result(
                new ChangeOrderLinesResponseDto()
                    .items(newItems)
                    .changeRequestId(123L)
                    .changeRequestStatus(ChangeRequestStatusType.PROCESSING)
            );
    }

    @Nonnull
    private ConfirmOrderChangedByPartnerRequest confirmRequest() {
        return new ConfirmOrderChangedByPartnerRequest().setUpdateOrderItemsRequest(
            UpdateOrderItemsRequest.builder()
                .barcode("FF-102")
                .items(List.of(
                    ItemDto.builder()
                        .name("offerName")
                        .vendorId(101L)
                        .article("100500672239")
                        .count(3)
                        .price(
                            MonetaryDto.builder()
                                .currency("RUB")
                                .value(new BigDecimal("0.10"))
                                .exchangeRate(BigDecimal.ONE)
                                .build()
                        )
                        .assessedValue(
                            MonetaryDto.builder()
                                .currency("RUB")
                                .value(new BigDecimal("0.10"))
                                .exchangeRate(BigDecimal.ONE)
                                .build()
                        )
                        .dimensions(
                            KorobyteDto.builder()
                                .length(1)
                                .width(2)
                                .height(3)
                                .weightGross(BigDecimal.valueOf(4.9))
                                .build()
                        )
                        .vatType(VatType.NO_VAT)
                        .removableIfAbsent(false)
                        .cargoTypes(Set.of(CargoType.DOCUMENTS_AND_SECURITIES))
                        .msku(123L)
                        .build()
                ))
                .cost(CostDto.builder()
                    .paymentMethod(PaymentMethod.PREPAID)
                    .cashServicePercent(BigDecimal.ZERO)
                    .assessedValue(new BigDecimal("0.30"))
                    .delivery(BigDecimal.ZERO)
                    .deliveryForCustomer(BigDecimal.ZERO)
                    .manualDeliveryForCustomer(BigDecimal.ZERO)
                    .isFullyPrepaid(true)
                    .total(new BigDecimal("0.30"))
                    .services(List.of(
                        OrderServiceDto.builder()
                            .code(ShipmentOption.INSURANCE)
                            .cost(BigDecimal.ZERO)
                            .customerPay(false)
                            .build()
                    ))
                    .tariffId(127L)
                    .build())
                .externalRequestId("123")
                .build()
        );
    }

    private void mockMbiOrderService(ChangeOrderLinesResponse response) {
        when(ordersLinesCommonApi.postChangeOrderLines(
            eq(101L),
            eq(2L),
            refEq(
                new ChangeOrderLinesRequest()
                    .reason(UpdateOrderReasonType.ITEMS_NOT_FOUND)
                    .lines(List.of(new OrderLineChange().ssku("100500672239").count(3)))
            ),
            isNull(),
            isNull()
        ))
            .thenReturn(response);
    }

}
