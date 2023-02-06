package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import NSprav.UnifierReplyOuterClass.UnifierReply;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.altay.model.SignalOuterClass;
import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest.DeliverySearchRequestBuilder;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.lom.model.dto.AbstractOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.TaxSystem;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.dto.MultiplaceItem;
import ru.yandex.market.logistics.nesu.dto.Place;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftShipment;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.utils.SenderAvailableDeliveriesUtils;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliveryOptionServices;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getDefaultAddressBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getSimpleUnifierReplyWithAddress;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockDeliveryOptionValidation;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.YANDEX_INN;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultLomItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultLomPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDeliveryInterval;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDimensions;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createKorobyte;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomItemBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createWithdrawBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOption;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOptionServices;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultLomDeliveryServices;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.deliveryServiceBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCreateOrderTest extends AbstractCreateOrderMultiplaceCasesTest {
    @BeforeEach
    void before() {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of());
    }

    @Test
    @DisplayName("405 при создании заказа магазином не из списка разрешенных")
    void shopIdNotAllowedToCreateOrder() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of());
        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isMethodNotAllowed())
            .andExpect(errorMessage("Method is no longer allowed for shopId=1"));
    }

    @Test
    @DisplayName("Склад с другим businessId")
    void wrongBusinessId() throws Exception {
        mockGetLogisticsPoints(
            createLogisticsPointResponseBuilder(3L, null, "warehouse1", PointType.WAREHOUSE)
                .businessId(42L)
                .build(),
            WAREHOUSE_TO,
            PICKUP_POINT
        );

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [3]"));
    }

    @Test
    @DisplayName("ПВЗ не найден")
    void createOrderDraftPickupPointNotFoundTest() throws Exception {
        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(101L, 3L, 4L), true))
        ))
            .thenReturn(List.of(WAREHOUSE_FROM, WAREHOUSE_TO));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [LOGISTICS_POINT] with ids [101]\","
                + "\"resourceType\":\"LOGISTICS_POINT\",\"identifiers\":[101]}"));
    }

    @Test
    @DisplayName("Склад не найден")
    void createOrderDraftWarehouseNotFoundTest() throws Exception {
        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 4L, 101L), true))
        ))
            .thenReturn(List.of(WAREHOUSE_FROM, PICKUP_POINT));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [LOGISTICS_POINT] with ids [4]\","
                + "\"resourceType\":\"LOGISTICS_POINT\",\"identifiers\":[4]}"));
    }

    @Test
    @DisplayName("По умолчанию доступны только активные службы")
    void createOrderDraftDeliveryServiceNotValidTest() throws Exception {
        SearchPartnerFilter partnerFilter = LmsFactory.createPartnerFilter(
            Set.of(5L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        );
        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of());

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [5]"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @Test
    @DisplayName("Отгрузка партнеру СД, отличающейся от региональных настроек")
    void createOrderDraftDifferentShipmentPartnerTo() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L, 42L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(42L, PartnerType.DELIVERY)
            ));

        LogisticsPointResponse warehouseTo =
            LmsFactory.createLogisticsPointResponse(4L, 202L, 42L, "warehouse2", PointType.WAREHOUSE);

        mockGetLogisticsPoints(WAREHOUSE_FROM, warehouseTo, PICKUP_POINT);

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(d -> d.getShipment().setPartnerTo(42L))
                .andThen(d -> d.setPlaces(null))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Для сендера id=1 нет настройки для СД id=42 в регионе 1."));
    }

    @Test
    @DisplayName("Партнёр СД в опциях доставки отличается от региональных настроек")
    void createOrderDraftDifferentDeliveryOptionPartnerTo() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L, 42L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(42L, PartnerType.DELIVERY)
            ));

        LogisticsPointResponse pick =
            LmsFactory.createLogisticsPointResponse(101L, 42L, "pick", PointType.PICKUP_POINT);

        mockGetLogisticsPoints(WAREHOUSE_FROM, WAREHOUSE_TO, pick);

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(d -> d.setPlaces(null))
                .andThen(d -> d.getDeliveryOption().setPartnerId(42L))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Если не используется сортировочный центр, идентификатор партнера (5) в информации об отгрузке, "
                    + "должен совпадать с идентификатором партнера (42), указанным в опции доставки."
            ));
    }

    @Test
    @DisplayName("Сендер не найден")
    void createOrderDraftSenderNotFound() throws Exception {
        createOrder(OrderDtoFactory.defaultOrderDraft(), 42L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [42]"));
    }

    @Test
    @DisplayName("Партнер, указанный в опциях доставки не является СД")
    void noDeliveryServiceTest() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L, 6L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER)
            ));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> o.getDeliveryOption().setPartnerId(6L)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DELIVERY_SERVICE] with ids [6]"));
    }

    @Test
    @DisplayName("Склад отправления - ПВЗ")
    void fromNotWarehouse() throws Exception {
        LogisticsPointResponse warehouseFrom =
            LmsFactory.createLogisticsPointResponse(3L, 1L, "pick", PointType.PICKUP_POINT);

        mockGetLogisticsPoints(warehouseFrom, WAREHOUSE_TO, PICKUP_POINT);

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [3]"));
    }

    @Test
    @DisplayName("Склад получения - ПВЗ (не через pickupPointId)")
    void toNotWarehouse() throws Exception {
        LogisticsPointResponse warehouseTo =
            LmsFactory.createLogisticsPointResponse(4L, 1L, "pick", PointType.PICKUP_POINT);

        mockGetLogisticsPoints(WAREHOUSE_FROM, warehouseTo, PICKUP_POINT);

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [4]"));
    }

    @Test
    @DisplayName("Склад отправления не принадлежит магазину")
    void invalidMarketIdFrom() throws Exception {
        LogisticsPointResponse warehouseFrom = LmsFactory.createLogisticsPointResponseBuilder(
                3L,
                1L,
                "pick",
                PointType.WAREHOUSE
            )
            .businessId(100L)
            .build();

        mockGetLogisticsPoints(warehouseFrom, WAREHOUSE_TO, PICKUP_POINT);

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [3]"));
    }

    @Test
    @DisplayName("Склад получения не соответствует указанному партнеру")
    void notPartnerWarehouseTo() throws Exception {
        LogisticsPointResponse warehouseTo =
            LmsFactory.createLogisticsPointResponse(4L, 42L, "pick", PointType.WAREHOUSE);

        mockGetLogisticsPoints(WAREHOUSE_FROM, warehouseTo, PICKUP_POINT);

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Указанный склад получателя id=4 "
                + "не принадлежит партнеру id=5"));
    }

    @Test
    @DisplayName("Склад получения принадлежит магазину, а не СД")
    void senderWarehouseTo() throws Exception {
        LogisticsPointResponse warehouseTo =
            LmsFactory.createLogisticsPointResponse(4L, null, "pick", PointType.WAREHOUSE);

        mockGetLogisticsPoints(WAREHOUSE_FROM, warehouseTo, PICKUP_POINT);

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Указанный склад получателя id=4 "
                + "не принадлежит партнеру id=5"));
    }

    @Test
    @DisplayName("Не указан partnerTo в фильтре отгрузки. Не должны валидировать опции доставки")
    void notShipmentPartnerTo() throws Exception {
        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto
            .setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                INITIAL_SHIPMENT_DATE,
                OrderDtoFactory.createLocation(3L),
                null,
                null,
                ShipmentType.WITHDRAW
            ).build()))
            .setCost(createLomOrderCost(null).build());

        createOrder(OrderDtoFactory.defaultOrderDraft()
            .andThen(orderDraft -> orderDraft.getShipment().setPartnerTo(null)))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderDto);
        verifyZeroInteractions(deliveryCalculatorSearchEngineClient);
    }

    @Test
    @DisplayName("Не указан type в фильтре отгрузки. Не должны валидировать опции доставки")
    void notShipmentType() throws Exception {
        List<OrderServiceDto> services = addLomSortService(OrderDtoFactory.defaultLomDeliveryServices("3.86", "0.75"));
        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(
                List.of(
                    OrderDtoFactory.createWaybillSegmentBuilder(
                        INITIAL_SHIPMENT_DATE,
                        OrderDtoFactory.createLocation(3L),
                        null,
                        5L,
                        null
                    ).build()
                )
            )
            .setCost(
                OrderDtoFactory.createLomOrderCost(null)
                    .services(services)
                    .build()
            );

        createOrder(OrderDtoFactory.defaultOrderDraft()
            .andThen(orderDraft -> {
                orderDraft.getDeliveryOption().setServices(
                    addSortService(OrderDtoFactory.defaultDeliveryOptionServices("3.86", "0.75"))
                );
                orderDraft.getShipment().setType(null);
            }))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
        verifyZeroInteractions(deliveryCalculatorSearchEngineClient);
    }

    @Test
    @DisplayName("Создание черновика заказа без НДС")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_without_tax.xml",
        type = DatabaseOperation.REFRESH
    )
    void orderServicesWithoutTax() throws Exception {
        createOrder(orderDraftWithDeliveryService())
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setCost(createLomOrderCost().services(
                    List.of(
                        deliveryLomOrderService(ImmutableSortedSet.of(VatType.NO_VAT)),
                        createLomOrderService(ShipmentOption.INSURANCE, BigDecimal.valueOf(0.75)),
                        createLomOrderService(ShipmentOption.CASH_SERVICE, BigDecimal.valueOf(3.4)),
                        createLomOrderService(ShipmentOption.RETURN, BigDecimal.valueOf(0.75)),
                        createLomOrderService(ShipmentOption.RETURN_SORT, BigDecimal.valueOf(20))
                    )).build())
                .setSenderTaxSystem(TaxSystem.USN_INCOME)
        );
    }

    @Test
    @DisplayName("Создание черновика заказа c НДС")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_with_tax.xml",
        type = DatabaseOperation.REFRESH
    )
    void orderServicesWithTax() throws Exception {
        createOrder(orderDraftWithDeliveryService())
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setCost(createLomOrderCost().services(
                    List.of(
                        deliveryLomOrderService(ImmutableSortedSet.of(VatType.VAT_20)),
                        createLomOrderService(ShipmentOption.INSURANCE, BigDecimal.valueOf(0.75)),
                        createLomOrderService(ShipmentOption.CASH_SERVICE, BigDecimal.valueOf(3.4)),
                        createLomOrderService(ShipmentOption.RETURN, BigDecimal.valueOf(0.75)),
                        createLomOrderService(ShipmentOption.RETURN_SORT, BigDecimal.valueOf(20))
                    )
                ).build())
                .setSenderTaxSystem(TaxSystem.OSN)
        );
    }

    @Test
    @DisplayName("Создание черновика заказа для сендера с другим идентификатором")
    void createSecondSenderOrder() throws Exception {
        long senderId = 25L;

        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);
        mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            defaultDeliverySearchRequestBuilder()
                .deliveryServiceIds(Set.of(5L))
                .senderId(senderId)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft(), senderId)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createLomOrderRequest()
            .setSenderId(25L)
            .setSenderName("test-sender-name25")
            .setSenderUrl("www.test-sender-name25.com")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное создание черновика заказа с модификацией запроса вариантов доставки")
    @MethodSource("createOrderDraftDeliveryOptionFilterSource")
    void createOrderDraftDeliveryOptionFilter(
        @SuppressWarnings("unused") String displayName,
        Consumer<OrderDraft> draftConsumer,
        Consumer<WaybillOrderRequestDto> lomOrderConsumer,
        UnaryOperator<DeliverySearchRequestBuilder> filterConsumer
    ) throws Exception {
        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);

        mockDeliveryOption(filterConsumer.apply(defaultDeliverySearchRequestBuilder()).build());

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(draftConsumer))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        lomOrderConsumer.accept(orderRequestDto);
        verifyLomOrderCreate(orderRequestDto);
    }

    private static Stream<Arguments> createOrderDraftDeliveryOptionFilterSource() {
        return Stream.<Quadruple<
            String,
            Consumer<OrderDraft>,
            Consumer<WaybillOrderRequestDto>,
            UnaryOperator<DeliverySearchRequestBuilder>
            >>of(
            Quadruple.of(
                "deliveryType",
                orderDraft -> orderDraft.setDeliveryType(DeliveryType.PICKUP),
                lomRequest -> lomRequest
                    .setWaybill(List.of(createWithdrawBuilder(false).segmentType(SegmentType.PICKUP).build()))
                    .setDeliveryType(ru.yandex.market.logistics.lom.model.enums.DeliveryType.PICKUP)
                    .setRecipient(
                        OrderDtoFactory.createRecipientBuilder().address(
                            OrderDtoFactory.createAddressBuilder().latitude(null).longitude(null).build()
                        ).build()
                    ),
                filter -> filter.deliveryServiceIds(Set.of(5L)).tariffType(TariffType.PICKUP)
            ),
            Quadruple.of(
                "places.items.price, places.items.assessedValue",
                orderDraft -> {
                    OrderDraftDeliveryOption deliveryOption = defaultDeliveryOption();
                    deliveryOption.setServices(filterCashService(defaultDeliveryOptionServices("0.459", "0.75")));
                    orderDraft
                        .setDeliveryOption(deliveryOption)
                        .setPlaces(List.of(createDefaultPlace(
                            createItem()
                                .setPrice(BigDecimal.ZERO)
                                .setAssessedValue(BigDecimal.ZERO)
                        )));
                },
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .price(OrderDtoFactory.createMonetary(BigDecimal.ZERO))
                            .assessedValue(OrderDtoFactory.createMonetary(BigDecimal.ZERO))
                            .build()
                    ))
                    .setCost(
                        OrderDtoFactory.createLomOrderCost()
                            .assessedValue(BigDecimal.valueOf(125))
                            .services(filterLomCashService(defaultLomDeliveryServices("0.459", "0.75"))).build()
                    ),
                filter -> filter.offerPrice(0L)
            ),
            Quadruple.of(
                "places.items.price",
                orderDraft -> {
                    OrderDraftDeliveryOption deliveryOption = defaultDeliveryOption();
                    deliveryOption.setServices(filterCashService(defaultDeliveryOptionServices("0.459", "0.75")));
                    orderDraft
                        .setDeliveryOption(deliveryOption)
                        .setPlaces(List.of(createDefaultPlace(createItem().setPrice(null))));
                },
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .price(OrderDtoFactory.createMonetary(null))
                            .build()
                    ))
                    .setCost(
                        OrderDtoFactory.createLomOrderCost()
                            .services(filterLomCashService(defaultLomDeliveryServices("0.459", "0.75")))
                            .build()
                    ),
                filter -> filter.offerPrice(null)
            ),
            Quadruple.of(
                "places.items.count",
                orderDraft -> {
                    OrderDraftDeliveryOption deliveryOption = defaultDeliveryOption();
                    deliveryOption.setServices(filterCashService(defaultDeliveryOptionServices("0.459", "0.75")));
                    orderDraft
                        .setDeliveryOption(deliveryOption)
                        .setPlaces(List.of(createDefaultPlace(createItem().setCount(null))));
                },
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .count(null)
                            .build()
                    ))
                    .setCost(
                        OrderDtoFactory.createLomOrderCost()
                            .services(filterLomCashService(defaultLomDeliveryServices("0.459", "0.75")))
                            .build()
                    ),
                filter -> filter.offerPrice(null)
            )
        ).map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth()));
    }

    @Test
    @DisplayName("Успешное создание черновика заказа без указания pickPointId")
    void createOrderDraftTestNoPickupPoint() throws Exception {
        mockDeliveryOptionNoPickupPoint();

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(order -> order.getRecipient().setPickupPointId(null)))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createLomOrderRequest().setPickupPointId(null));
    }

    @Test
    @DisplayName("Успешное создание черновика заказа без даты отгрузки")
    void createOrderDraftShipmentDateNull() throws Exception {
        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                null,
                OrderDtoFactory.createLocation(3L),
                null,
                5L,
                ShipmentType.WITHDRAW
            ).build()))
            .setDeliveryInterval(
                createDeliveryInterval()
                    .deliveryDateMin(LocalDate.of(2019, 2, 5))
                    .deliveryDateMax(LocalDate.of(2019, 2, 12))
                    .build()
            );

        createOrder(OrderDtoFactory.defaultOrderDraft()
            .andThen(d -> d.getShipment().setDate(null))
            .andThen(d -> d.getDeliveryOption()
                .setCalculatedDeliveryDateMin(LocalDate.of(2019, 2, 5))
                .setCalculatedDeliveryDateMax(LocalDate.of(2019, 2, 12))
            ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа для отгрузки самопривозом")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_for_import.xml",
        type = DatabaseOperation.UPDATE
    )
    void importShipment() throws Exception {
        when(lmsClient.getLogisticsPoints(
            LmsFactory.createLogisticsPointsFilter(Set.of(4L), null, PointType.WAREHOUSE, true)
        )).thenReturn(List.of(WAREHOUSE_TO));

        LocalDate shipmentDate = LocalDate.parse("2019-08-05");
        LocalDate deliveryDateMax = LocalDate.parse("2019-08-13");

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                shipmentDate,
                OrderDtoFactory.createLocation(3L),
                OrderDtoFactory.createLocation(4L),
                5L,
                ShipmentType.IMPORT
            ).build()))
            .setDeliveryInterval(createDeliveryInterval().deliveryDateMax(deliveryDateMax).build());

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(
                    d -> d.getShipment()
                        .setType(ru.yandex.market.logistics.nesu.client.enums.ShipmentType.IMPORT)
                        .setDate(shipmentDate)
                )
                .andThen(d -> d.getDeliveryOption().setCalculatedDeliveryDateMax(deliveryDateMax))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа без указания склада получателя")
    void createOrderDraftTestNoLocationTo() throws Exception {
        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 101L), true))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(3L, 41L, null, "warehouse1", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(101L, 5L, "pick", PointType.PICKUP_POINT)
            ));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                INITIAL_SHIPMENT_DATE,
                OrderDtoFactory.createLocation(3L),
                null,
                5L,
                ShipmentType.WITHDRAW
            ).build()));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(d -> d.getShipment().setWarehouseTo(null)))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа только со складом отправителя")
    void createOrderDraftOnlyLocationFrom() throws Exception {
        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(33L, 101L), true))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(33L, 41L, null, "warehouse1", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(101L, 5L, "pick", PointType.PICKUP_POINT)
            ));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                null,
                OrderDtoFactory.createLocation(33L),
                null,
                null,
                null
            ).build()))
            .setDeliveryInterval(DeliveryIntervalDto.builder().build())
            .setCost(
                OrderDtoFactory.createLomOrderCost(null)
                    .delivery(null)
                    .deliveryForCustomer(null)
                    .tariffId(null)
                    .services(null)
                    .build()
            );

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(d -> d.setShipment(new OrderDraftShipment().setWarehouseFrom(33L)))
                .andThen(d -> d.setDeliveryOption(null))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа без указания склада отправителя")
    void createOrderDraftTestNoLocationFrom() throws Exception {
        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(4L, 101L), true))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(4L, 5L, "warehouse2", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(101L, 5L, "pick", PointType.PICKUP_POINT)
            ));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(
                OrderDtoFactory.createWaybillSegmentBuilder(
                    INITIAL_SHIPMENT_DATE,
                    null,
                    null,
                    5L,
                    ShipmentType.WITHDRAW
                ).build()
            ))
            .setReturnSortingCenterId(null);

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(d -> d.getShipment().setWarehouseFrom(null)))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Нет настроек СД для сендера в регионе")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftNoDeliverySettingsSettings() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        mockGetLogisticsPoint(
            LmsFactory.createLogisticsPointResponseBuilder(3L, null, "warehouse1", PointType.WAREHOUSE)
                .address(LmsFactory.createAddressDto(11117))
                .businessId(41L)
                .build()
        );

        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(4L, 101L), true))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(4L, 3L, "warehouse2", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(101L, 3L, "pick", PointType.PICKUP_POINT)
            ));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .senderId(3L)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft(), 3L, 3L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Нет региональных настроек для локации 11117 для сендера 3."));
    }

    @Test
    @DisplayName("Создание черновика заказа для сендера с региональными настройками")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftRegionSettings() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(4L, 101L), true))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(4L, 5L, "warehouse2", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(101L, 5L, "pick", PointType.PICKUP_POINT)
            ));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .deliveryServiceIds(Set.of(5L))
                .senderId(3L)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft(), 3L, 3L)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(
                OrderDtoFactory.createWaybillSegmentBuilder(
                    INITIAL_SHIPMENT_DATE,
                    OrderDtoFactory.createLocation(3L),
                    null,
                    5L,
                    ShipmentType.WITHDRAW
                ).build()
            ))
            .setItems(List.of(
                OrderDtoFactory.createLomItemBuilder()
                    .vendorId(3L)
                    .build()
            ))
            .setSenderId(3L);
        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа с полной информацией в нем с использованием СЦ для отгрузки")
    void createOrderDraftWithSortingCenterTest() throws Exception {
        LocationDto sortingCenterWarehouse = OrderDtoFactory.createLocation(5L);

        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO
            ));

        mockDeliveryOptionNoPickupPoint();

        createOrder(createOrderThroughSortingCenter())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(sortingCenterOrder(sortingCenterWarehouse));
        verify(lmsClient, times(0)).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()));
    }

    @Test
    @DisplayName("Успешное создание черновика заказа с полной информацией в нем с использованием СЦ для отгрузки")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftWithSortingCenterRegionSettings() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO
            ));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .deliveryServiceIds(Set.of(5L))
                .senderId(3L)
                .pickupPoints(null)
                .build()
        );

        createOrder(createOrderThroughSortingCenter().andThen(d -> d.getShipment().setWarehouseTo(5L)), 3L, 3L)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Ошибка при создании черновика заказа: склад СЦ в регионе не совпадает со складов назначения в заказе")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftWithSortingCenterRegionSettingsIncorrectSortingCenterWarehouse() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 6L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO_2
            ));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .senderId(4L)
                .pickupPoints(null)
                .build()
        );

        createOrder(
            createOrderThroughSortingCenter()
                .andThen(d -> d.getShipment().setWarehouseTo(6L)),
            3L,
            3L
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Указанный склад сортировочного центра id=6 не соответствует региональным настройкам сендера id=3."
            ));
    }

    @Test
    @DisplayName("Ошибка при создании черновика заказа: нет настроек для региона склада отправления")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftWithSortingCenterNoSettingsInLocation() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO
            ));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .senderId(4L)
                .pickupPoints(null)
                .build()
        );

        createOrder(
            createOrderThroughSortingCenter()
                .andThen(d -> d.getShipment().setWarehouseTo(5L)),
            4L,
            3L
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(
                "Нет региональных настроек для локации 1 для сендера id=4."
            ));
    }

    @Test
    @DisplayName("Успешное создание черновика заказа через СЦ: не происходит валидация региональных настроек, "
        + "так как нет склада отправления")
    @DatabaseSetup(
        value = "/repository/order/database_order_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderDraftWithSortingCenterNoWarehouseFrom() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(3L));
        SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries(
            lmsClient,
            LmsFactory.createPartner(SORTING_CENTER_ID_2, 203L, PartnerType.SORTING_CENTER),
            List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
                LmsFactory.createPartner(42L, PartnerType.DELIVERY),
                LmsFactory.createPartner(53916L, PartnerType.DELIVERY)
            ),
            List.of(
                SORTING_CENTER_WAREHOUSE_TO,
                SORTING_CENTER_WAREHOUSE_TO_2
            )
        );

        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(6L), true))))
            .thenReturn(List.of(SORTING_CENTER_WAREHOUSE_TO_2));

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .deliveryServiceIds(Set.of(5L))
                .locationFrom(11117)
                .senderId(4L)
                .pickupPoints(null)
                .build()
        );

        createOrder(
            createOrderThroughSortingCenter()
                .andThen(d -> d.getShipment().setWarehouseFrom(null).setWarehouseTo(6L)),
            4L,
            3L
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @DisplayName(
        "Успешное создание черновика заказа через СЦ без указания warehouseTo. Для данного СЦ есть ровно один склад"
    )
    void createOrderDraftThroughSortingCenterWithoutWarehouseTo() throws Exception {
        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L), true))))
            .thenReturn(List.of(WAREHOUSE_FROM));

        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()))).thenReturn(List.of(WAREHOUSE_TO));

        mockDeliveryOptionNoPickupPoint();

        createOrder(
            createOrderThroughSortingCenter()
                .andThen(d -> d.getShipment().setWarehouseTo(null))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(sortingCenterOrder(OrderDtoFactory.createLocation(4L)));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("orderDraftWithoutShipmentWarehouseToArguments")
    @DisplayName(
        "Ошибка создания черновика заказа через СЦ без указания warehouseTo"
    )
    void createOrderDraftThroughSortingCenterWithoutWarehouseTo(
        @SuppressWarnings("unused") String caseName,
        List<LogisticsPointResponse> sortingCenterWarehouses,
        String exceptionMessage
    ) throws Exception {
        mockSearchSortingCenter();

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L), true))))
            .thenReturn(List.of(WAREHOUSE_FROM));

        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()))).thenReturn(sortingCenterWarehouses);

        mockDeliveryOptionNoPickupPoint();

        createOrder(
            createOrderThroughSortingCenter()
                .andThen(d -> d.getShipment().setWarehouseTo(null))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage(exceptionMessage));
    }

    @Nonnull
    private static Stream<Arguments> orderDraftWithoutShipmentWarehouseToArguments() {
        return Stream.of(
            Arguments.of(
                "Для СЦ не найдено активных складов",
                List.of(),
                "Партнер id = 6 не имеет активных складов"
            ),
            Arguments.of(
                "Для СЦ найдено несколько активных складов",
                List.of(WAREHOUSE_FROM, WAREHOUSE_TO),
                "Для партнера id = 6 найдено больше одного активного склада с идентификаторами [3, 4]"
            )
        );
    }

    @Test
    @DisplayName("Валидация опций доставки, не найдена опция")
    void deliveryOptionValidationNotFound() throws Exception {
        when(deliveryCalculatorSearchEngineClient.deliverySearch(any()))
            .thenReturn(DeliverySearchResponse.builder().deliveryOptions(List.of()).build());

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isBadRequest())
            .andExpect(content().json("{\"errors\":[{\"errorCode\":\"OPTION_NOT_FOUND\"}]}"));
    }

    @Test
    @DisplayName("Валидация опций доставки")
    void deliveryOptionValidation() throws Exception {
        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);

        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L, SORTING_CENTER_ID),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartner(5L, PartnerType.DELIVERY),
                LmsFactory.createPartner(SORTING_CENTER_ID, 101L, PartnerType.SORTING_CENTER)
            ));

        Consumer<OrderDraft> invalidOrder = OrderDtoFactory.defaultOrderDraft()
            .andThen(d -> d.getShipment()
                .setPartnerTo(SORTING_CENTER_ID)
                .setWarehouseTo(5L)
                .setDate(LocalDate.of(2019, 8, 3))
            )
            .andThen(d -> d.getDeliveryOption()
                .setCalculatedDeliveryDateMin(LocalDate.of(2019, 8, 8))
                .setCalculatedDeliveryDateMax(LocalDate.of(2019, 8, 8))
                .setServices(List.of(
                    deliveryServiceBuilder().setCode(ServiceType.CASH_SERVICE),
                    deliveryServiceBuilder().setCode(ServiceType.WAIT_20),
                    deliveryServiceBuilder(ServiceType.INSURANCE, new BigDecimal("0.75"), false),
                    deliveryServiceBuilder(ServiceType.RETURN, new BigDecimal("0.76"), false),
                    deliveryServiceBuilder(ServiceType.RETURN_SORT, new BigDecimal("20"), false)
                )));

        createOrder(invalidOrder)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/validation/delivery_option_validation.json"));
    }

    @Test
    @DisplayName("Валидация опций доставки. Расчетный интервал доставки.")
    void deliveryOptionDateIntervalValidation() throws Exception {
        createOrder(OrderDtoFactory.defaultOrderDraft()
            .andThen(d -> d.getDeliveryOption()
                .setCalculatedDeliveryDateMin(LocalDate.of(2019, 8, 8))
                .setCalculatedDeliveryDateMax(LocalDate.of(2019, 9, 9))
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/validation/delivery_option_delivery_interval_validation.json"));
    }

    @Test
    @DisplayName("Валидация опций доставки, найдено более одной опции")
    void deliveryOptionValidationMultipleOption() throws Exception {
        DeliveryOption.DeliveryOptionBuilder optionBuilder = DeliveryOption.builder()
            .deliveryServiceId(5L)
            .tariffType(TariffType.COURIER)
            .services(defaultDeliveryOptionServices(100))
            .maxDays(MAX_DELIVERY_DAYS);
        when(deliveryCalculatorSearchEngineClient.deliverySearch(any()))
            .thenReturn(DeliverySearchResponse.builder().deliveryOptions(
                List.of(optionBuilder.build(), optionBuilder.cost(44L).tariffId(38L).build())
            ).build());

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("message")
                .value(containsString("При валидации опций доставки было получено более одной опции")));
    }

    @Test
    @DisplayName("Валидация опций доставки, с полной предоплатой")
    void deliveryOptionFullyPrepaid() throws Exception {
        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(order -> order.getCost().setFullyPrepaid(true))
                .andThen(order -> order.getDeliveryOption().setServices(
                    filterCashService(order.getDeliveryOption().getServices())
                ))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Стоимость кассового обслуживания из варианта доставки")
    void deliveryOptionCashService() throws Exception {
        doReturn(DeliverySearchResponse.builder().deliveryOptions(List.of(
            DeliveryOption.builder()
                .cost(100)
                .deliveryServiceId(5L)
                .tariffType(TariffType.COURIER)
                .maxDays(MAX_DELIVERY_DAYS)
                .services(defaultDeliveryOptionServices(100, 0.022))
                .build()
        )).build())
            .when(deliveryCalculatorSearchEngineClient)
            .deliverySearch(safeRefEq(defaultDeliverySearchRequestBuilder().build()));

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(order -> order.getDeliveryOption().setServices(
                    OrderDtoFactory.defaultDeliveryOptionServices("4.400", "0.75")
                ))
        )
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setCost(
                    OrderDtoFactory.createLomOrderCost(new BigDecimal("0.022"))
                        .services(OrderDtoFactory.defaultLomDeliveryServices("4.400", "0.75")).build()
                )
        );
    }

    @Test
    @DisplayName("Успешное создание черновика заказа из нескольких коробок")
    void createOrderDraftSeveralBoxesTest() throws Exception {
        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);
        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .length(150)
                .width(170)
                .height(110)
                .weight(BigDecimal.valueOf(50))
                .offerPrice(60_000L)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(orderDraft -> {
            orderDraft.setPlaces(List.of(
                    createDefaultPlace(10, 100, 40, "externalId-1"),
                    createDefaultPlace(80, 10, 50, "externalId-2"),
                    createDefaultPlace(20, 60, 60, "externalId-3")
                ))
                .setDimensions(createDimensions());
            orderDraft.getDeliveryOption().setServices(defaultDeliveryOptionServices("10.200", "0.75"));
        }))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        AbstractOrderRequestDto orderRequestDto = createLomOrderRequest()
            .setCost(createLomOrderCost().services(defaultLomDeliveryServices("10.200", "0.75")).build())
            .setItems(List.of(
                createDefaultLomItem("externalId-1", Set.of("externalId-1"), List.of(0)),
                createDefaultLomItem("externalId-2", Set.of("externalId-2"), List.of(1)),
                createDefaultLomItem("externalId-3", Set.of("externalId-3"), List.of(2))
            ))
            .setUnits(List.of(
                createDefaultLomPlace(10, 100, 40, "externalId-1"),
                createDefaultLomPlace(80, 10, 50, "externalId-2"),
                createDefaultLomPlace(20, 60, 60, "externalId-3"),
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .externalId("generated-0")
                    .dimensions(createKorobyte(110, 170, 150, 50))
                    .build()
            ));
        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Заказ через собственную СД помечается как fake")
    void ownDeliveryFakeOrder() throws Exception {
        mockOwnDelivery();

        createOrder(OrderDtoFactory.defaultOrderDraft()
            .andThen(o -> o.getDeliveryOption().setPartnerId(45L).setCalculatedDeliveryDateMin(INITIAL_SHIPMENT_DATE))
            .andThen(o -> o.getShipment().setPartnerTo(45L))
            .andThen(o -> o.getRecipient().setPickupPointId(null))
        )
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setWaybill(List.of(
                    OrderDtoFactory.createWaybillSegmentBuilder(
                            LocalDate.of(2019, 8, 1),
                            OrderDtoFactory.createLocation(3L),
                            null,
                            45L,
                            ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW
                        )
                        .build()
                ))
                .setPickupPointId(null)
                .setDeliveryInterval(
                    createDeliveryInterval()
                        .deliveryIntervalId(null)
                        .deliveryDateMin(INITIAL_SHIPMENT_DATE)
                        .fromTime(LocalTime.of(9, 0))
                        .toTime(LocalTime.of(22, 0))
                        .build()
                )
                .setFake(true)
        );
    }

    @Test
    @DisplayName("Заказ без даты отгрузки, через собственную СД помечается как fake")
    void ownDeliveryFakeOrderWithoutShipmentDate() throws Exception {
        mockOwnDelivery();

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(d -> d.getShipment().setPartnerTo(45L).setDate(null))
                .andThen(d -> d.getDeliveryOption()
                    .setPartnerId(45L)
                    .setCalculatedDeliveryDateMin(LocalDate.of(2019, 2, 2))
                    .setCalculatedDeliveryDateMax(LocalDate.of(2019, 2, 7))
                )
                .andThen(o -> o.getRecipient().setPickupPointId(null))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto
            .setWaybill(List.of(
                OrderDtoFactory.createWaybillSegmentBuilder(
                        null,
                        OrderDtoFactory.createLocation(3L),
                        null,
                        45L,
                        ShipmentType.WITHDRAW
                    )
                    .build()
            ))
            .setPickupPointId(null)
            .setDeliveryInterval(
                createDeliveryInterval()
                    .deliveryIntervalId(null)
                    .deliveryDateMin(LocalDate.of(2019, 2, 2))
                    .deliveryDateMax(LocalDate.of(2019, 2, 7))
                    .fromTime(null)
                    .toTime(null)
                    .build()
            )
            .setFake(true);

        verifyLomOrderCreate(orderRequestDto);
        verify(lmsClient, never()).getCourierScheduleDays(any());
    }

    @Test
    @DisplayName("Отсутствует локация склада отправления")
    void noWarehouseFromLocation() throws Exception {
        mockGetLogisticsPoints(
            LmsFactory.createLogisticsPointResponseBuilder(3L, null, "warehouse1", PointType.WAREHOUSE)
                .address(null)
                .businessId(41L)
                .build(),
            WAREHOUSE_TO,
            PICKUP_POINT
        );

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("No location id on shipment warehouse from address"));
    }

    @Test
    @DisplayName("Возвратный СЦ из региональных настроек")
    void regionalSortingCenter() throws Exception {
        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createLomOrderRequest().setReturnSortingCenterId(6L));
    }

    @Test
    @DisplayName("Возвратный СЦ из региональных настроек, локация склада отправления ниже по дереву регионов")
    void regionalSortingCenterRegionTree() throws Exception {
        mockGetLogisticsPoints(
            LmsFactory.createLogisticsPointResponseBuilder(3L, null, "warehouse3", PointType.WAREHOUSE)
                .address(LmsFactory.createAddressDto(213))
                .businessId(41L)
                .build(),
            WAREHOUSE_TO,
            PICKUP_POINT
        );

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createLomOrderRequest().setReturnSortingCenterId(6L));
    }

    @Test
    @DisplayName("Возвратный СЦ из региональных настроек, нужен отдельный запрос за возвратным складом")
    @DatabaseSetup(
        value = "/repository/order/sender_regional_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void alternativeRegionalSortingCenter() throws Exception {
        mockGetLogisticsPoints(
            LmsFactory.createLogisticsPointResponseBuilder(3L, null, "warehouse3", PointType.WAREHOUSE)
                .address(LmsFactory.createAddressDto(40))
                .businessId(41L)
                .build(),
            WAREHOUSE_TO,
            PICKUP_POINT
        );

        when(lmsClient.getLogisticsPoint(6L))
            .thenReturn(Optional.of(
                LmsFactory.createLogisticsPointResponse(6L, 7L, "warehouse6", PointType.WAREHOUSE)
            ));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createLomOrderRequest().setReturnSortingCenterId(7L));
    }

    @Test
    @DisplayName("Отсутствуют региональные настройки для локации склада отправления")
    void noMatchingRegionalSettings() throws Exception {
        mockGetLogisticsPoints(
            LmsFactory.createLogisticsPointResponseBuilder(3L, null, "warehouse1", PointType.WAREHOUSE)
                .address(LmsFactory.createAddressDto(3))
                .businessId(41L)
                .build(),
            WAREHOUSE_TO,
            PICKUP_POINT
        );

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Нет региональных настроек для локации 3 для сендера id=1."));
    }

    @Test
    @DisplayName("Получаем адрес с квартирой")
    void recipientAddressEnrichesWithUnify() throws Exception {
        SignalOuterClass.Address.Builder builder = getDefaultAddressBuilder();
        String areaName = "какой-то регион";
        builder.addAdHierarchy(
            SignalOuterClass.Toponym.newBuilder()
                .setKind(SignalOuterClass.Toponym.Kind.AREA)
                .setName(areaName)
        );

        mockUnifierClient(getSimpleUnifierReplyWithAddress(builder.build()));

        mockCourierSchedule(lmsClient, 213, Set.of(5L));
        mockDeliveryOption(defaultDeliverySearchRequestBuilder().locationsTo(Set.of(213)).build());

        createOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(order -> order.getRecipient().setFullAddress("some string"))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setRecipient(
                    OrderDtoFactory.createRecipientBuilder()
                        .address(
                            AddressDto.builder()
                                .geoId(213)
                                .country("Россия")
                                .region("Москва")
                                .locality("Москва")
                                .subRegion(areaName)
                                .street("Новинский бульвар")
                                .house("8")
                                .zipCode("121099")
                                .room("5.11")
                                .latitude(new BigDecimal("55.7513100141919"))
                                .longitude(new BigDecimal("37.5846221554295"))
                                .build()
                        )
                        .build()
                )
        );
        verify(geoClient, never()).find(any());
    }

    @Test
    @DisplayName("Строка разбирается до улицы. Дома и квартиры - нет")
    void recipientAddressEnrichesWithGeoSearchWithoutHouseAndApartments() throws Exception {
        SignalOuterClass.Address.Builder addressBuilder = getDefaultAddressBuilder();
        addressBuilder.removeAdHierarchy(addressBuilder.getAdHierarchyCount() - 1);
        addressBuilder.removeAddInfoItems(0);
        UnifierReply expectedReply = getSimpleUnifierReplyWithAddress(addressBuilder.build());

        mockUnifierClient(expectedReply);

        mockCourierSchedule(lmsClient, 213, Set.of(5L));
        mockDeliveryOption(defaultDeliverySearchRequestBuilder().locationsTo(Set.of(213)).build());

        createOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(order -> order.getRecipient().setFullAddress("some string"))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setRecipient(OrderDtoFactory.createRecipientBuilder()
                    .address(
                        AddressDto.builder()
                            .geoId(213)
                            .country("Россия")
                            .region("Москва")
                            .locality("Москва")
                            .street("Новинский бульвар")
                            .zipCode("121099")
                            .latitude(new BigDecimal("55.7513100141919"))
                            .longitude(new BigDecimal("37.5846221554295"))
                            .build()
                    )
                    .build()
                )
        );
        verify(geoClient, never()).find(any());
    }

    @Test
    @DisplayName("Адрес из строки полного адреса не дает geoId населенного пункта")
    void recipientAddressEnrichesWithUnifyBadGeoId() throws Exception {
        UnifierReply expectedReply = getSimpleUnifierReplyWithAddress(
            getDefaultAddressBuilder().setGeoId(1234567L).build()
        );

        mockUnifierClient(expectedReply);
        String searchString = "address search string";

        createOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(order -> order.getRecipient().setFullAddress(searchString))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Failed to find city for address: " + searchString));
    }

    @Test
    @DisplayName("Вернулся кривой geoId от унификатора")
    void recipientAddressWithBadGeoId() throws Exception {
        UnifierReply expectedReply = getSimpleUnifierReplyWithAddress(
            getDefaultAddressBuilder().setGeoId(Long.MAX_VALUE).build()
        );

        mockUnifierClient(expectedReply);

        createOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(order -> order.getRecipient().setFullAddress("some string"))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(
                errorMessage(String.format("Failed to convert geoId '%s' from unification response", Long.MAX_VALUE))
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("argumentsWithFractionalKopecks")
    @DisplayName("Успешное создание черновика заказа c нецелым значением копеек")
    void createOrderDraftWithFractionalKopecks(
        @SuppressWarnings("unused") String caseName,
        Consumer<OrderDraft> orderDraftConsumer,
        CostDto.CostDtoBuilder costDtoBuilder,
        String cashServiceCost,
        String insuranceCost
    ) throws Exception {
        mockDeliveryOption(defaultDeliverySearchRequestBuilder().build());
        List<OrderServiceDto> services = OrderDtoFactory.defaultLomDeliveryServices(cashServiceCost, insuranceCost);
        createOrder(orderDraftConsumer.andThen(orderDraft -> orderDraft.getDeliveryOption().setServices(
            OrderDtoFactory.defaultDeliveryOptionServices(cashServiceCost, insuranceCost)))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            OrderDtoFactory.createLomOrderRequest()
                .setCost(costDtoBuilder.services(services).build())
                .setTags(Set.of(getTag()))
        );
    }

    private static Stream<Arguments> argumentsWithFractionalKopecks() {
        return Stream.of(
            Arguments.of(
                "Дробные копейки в объявленной стоимости",
                OrderDtoFactory.defaultOrderDraft()
                    .andThen(order -> order.getCost().setAssessedValue(BigDecimal.valueOf(125.1234))),
                OrderDtoFactory.createLomOrderCost().assessedValue(BigDecimal.valueOf(125.12)),
                "3.4",
                "0.75"
            ),
            Arguments.of(
                "Дробные копейки в стоимости доставки для клиента, указанной магазином",
                OrderDtoFactory.defaultOrderDraft()
                    .andThen(order -> order.getCost().setManualDeliveryForCustomer(BigDecimal.valueOf(130.1254))),
                OrderDtoFactory.createLomOrderCost().manualDeliveryForCustomer(BigDecimal.valueOf(130.13)),
                "5.61",
                "0.75"
            )
        );
    }

    @Test
    @DisplayName("Невалидный черновик заказа не для YaGo")
    void invalidOrderDraftNotForYaGo() throws Exception {
        createOrder(OrderDtoFactory.defaultExtendedInvalidOrderDraft()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Валидация длины внешнего идентификатора заказа Yandex.Go проходит (20 символов)")
    void yandexGoOrderExternalIdSizeValidationPassed() throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(1L));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> o.setExternalId("01234567890123456789")), 1L)
            .andExpect(status().isOk());

        verify(lomClient).createOrder(any(WaybillOrderRequestDto.class), eq(false));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {YANDEX_INN, "027204637820"})
    @DisplayName("Успешная валидация ИНН")
    void innSizeValidationPassed(String inn) throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(1L));

        createOrder(
            OrderDtoFactory.defaultOrderDraft().andThen(o -> o.setPlaces(List.of(new Place().setItems(List.of(
                createItem().setSupplierInn(inn)
            ))))),
            1L
        )
            .andExpect(status().isOk());

        verify(lomClient).createOrder(any(WaybillOrderRequestDto.class), eq(false));
    }

    @Test
    @DisplayName("Некорректный PREPAID заменяется на CASH")
    void incorrectPrepaidReplaced() throws Exception {
        mockDeliveryOptionNoPickupPoint();

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(order -> {
            order.getRecipient().setPickupPointId(null);
            order.getCost()
                .setFullyPrepaid(false)
                .setPaymentMethod(
                    ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod.PREPAID
                );
        }))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setPickupPointId(null)
                .setCost(
                    OrderDtoFactory.createLomOrderCost()
                        .isFullyPrepaid(false)
                        .paymentMethod(PaymentMethod.CASH)
                        .build()
                )
        );
    }

    @Test
    @DisplayName("Создание черновика заказа c КИЗами")
    void createOrderDraftWithCis() throws Exception {
        MultiplaceItem item = OrderDtoFactory.createItem();
        item.setInstances(List.of(OrderDtoFactory.createItemInstance()));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(orderDraft -> orderDraft.setItems(List.of(item))))
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setItems(List.of(
                    createLomItemBuilder()
                        .instances(List.of(Map.of("CIS_FULL", "item-instance-cis")))
                        .boxes(List.of(
                            OrderItemBoxDto.builder()
                                .dimensions(OrderDtoFactory.createItemKorobyte())
                                .build()
                        ))
                        .build()
                ))
        );
    }

    @Test
    @DisplayName("Создание черновика заказа c заполнением информации о поставщике данными отправителя для магазина GO")
    void createOrderDraftWithSupplierInfoFromSender() throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(1L));

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(orderDraft -> orderDraft.setItems(List.of(OrderDtoFactory.createItem())))
        )
            .andExpect(status().isOk());

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setItems(List.of(
                    createLomItemBuilder()
                        .supplierPhone("9999999999")
                        .supplierName("test-sender-name")
                        .boxes(List.of(
                            OrderItemBoxDto.builder()
                                .dimensions(OrderDtoFactory.createItemKorobyte())
                                .build()
                        ))
                        .build()
                ))
        );
    }
}
