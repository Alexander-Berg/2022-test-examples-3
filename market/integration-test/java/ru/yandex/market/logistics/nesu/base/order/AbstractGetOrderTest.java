package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractGetOrderTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    protected long orderId = 1000;
    private Supplier<OrderDto> result;

    @BeforeEach
    void setup() {
        result = this::defaultOrder;

        when(lomClient.getOrder(
            orderId,
            Set.of(OptionalOrderPart.CHANGE_REQUESTS, OptionalOrderPart.UPDATE_RECIPIENT_ENABLED)
        ))
            .thenAnswer(i -> Optional.ofNullable(result.get()));
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        result = () -> null;

        getOrder()
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Неизвестный сендер")
    void invalidSender() throws Exception {
        result = () -> defaultOrder().setSenderId(-1L);

        getOrder()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [-1]"));
    }

    @Test
    @DisplayName("Неизвестный склад")
    void invalidWarehouse() throws Exception {
        result = () -> defaultOrder()
            .setWaybill(List.of(WaybillSegmentDto.builder()
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(110L).build())
                        .build()
                )
                .build()
            ));

        getOrder()
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [LOGISTICS_POINT] with ids [110]"));
    }

    @Test
    @DisplayName("Неизвестный партнер")
    void invalidPartner() throws Exception {
        result = () -> defaultOrder().setWaybill(List.of(WaybillSegmentDto.builder().partnerId(100L).build()));

        getOrder()
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [100]"));
    }

    @Test
    @DisplayName("Конвертация реальной службы доставки в виртуальную")
    void convertVirtualDeliveryService() throws Exception {
        mockGetLogisticsPoints(Set.of(110L, 111L));
        mockGetLogisticsPoints(Set.of(110L));

        mockSearchPartners(
            Set.of(100L),
            List.of(
                LmsFactory.createPartnerResponseBuilder(100L, PartnerType.DELIVERY, 100L)
                    .readableName("delivery_service")
                    .subtype(PartnerSubtypeResponse.newBuilder().id(2).build())
                    .build()
            )
        );

        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder()
                .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                .setIds(Set.of(53916L))
                .build()
        ))
            .thenReturn(List.of(
                LmsFactory.createPartnerResponseBuilder(53916L, PartnerType.DELIVERY, 100L)
                    .readableName("virtual_delivery_service")
                    .build()
            ));

        result = () -> defaultOrder().setWaybill(List.of(
            OrderDtoFactory.waybillSegmentWithStatusDto(1)
                .partnerId(100L)
                .partnerType(ru.yandex.market.logistics.lom.model.enums.PartnerType.DELIVERY)
                .externalId("test-external-id")
                .build()
        ));

        getOrder()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/get/virtual_delivery_service.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({"orderParsingProvider", "orderParsingProviderMultiplace"})
    @DisplayName("Получение полей заказа")
    void orderParsing(String responsePath, UnaryOperator<OrderDto> orderBuilder) throws Exception {
        mockGetLogisticsPoints(Set.of(110L, 111L));
        mockGetLogisticsPoints(Set.of(110L));

        mockSearchPartners(
            Set.of(100L, 200L),
            List.of(
                LmsFactory.createPartner(100L, PartnerType.DELIVERY, "delivery_service", 100L),
                LmsFactory.createPartner(200L, PartnerType.DELIVERY, "delivery_service_2", 100L)
            )
        );

        result = () -> orderBuilder.apply(defaultOrder());

        getOrder()
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    private void mockSearchPartners(Set<Long> ids, List<PartnerResponse> partners) {
        when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(ids).build())).thenReturn(partners);
    }

    private void mockGetLogisticsPoints(Set<Long> ids) {
        when(lmsClient
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(ids, null))))
            .thenReturn(ids.stream()
                .map(id -> LmsFactory.createLogisticsPointResponse(id, 100L, "warehouse_" + id, PointType.WAREHOUSE))
                .collect(Collectors.toList())
            );
    }

    private static Stream<Arguments> orderParsingProviderMultiplace() {
        return Stream.<Pair<String, UnaryOperator<OrderDto>>>of(
            Pair.of(
                "controller/order/get/items_multiplace.json",
                orderDto -> unitsAndItems(orderDto).setItems(
                    defaultItems(Set.of("place_external_id_1", "place_external_id_2"))
                )
            ),
            Pair.of(
                "controller/order/get/items_multiplace_without_box.json",
                orderDto -> {
                    List<ItemDto> itemDtos =
                        new ArrayList<>(defaultItems(Set.of("place_external_id_1", "place_external_id_2")));

                    itemDtos.add(ItemDto.builder().build());
                    return unitsAndItems(orderDto).setItems(itemDtos);
                }
            )
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    private static Stream<Arguments> orderParsingProvider() {
        return Stream.<Pair<String, UnaryOperator<OrderDto>>>of(
            Pair.of("controller/order/get/minimal.json", UnaryOperator.identity()),
            Pair.of(
                "controller/order/get/plain_fields.json",
                builder -> builder
                    .setExternalId("external_order_id")
                    .setComment("order_comment")
            ),
            Pair.of(
                "controller/order/get/cost.json",
                orderDto -> orderDto.setCost(CostDto.builder()
                    .paymentMethod(PaymentMethod.CASH)
                    .assessedValue(BigDecimal.TEN)
                    .isFullyPrepaid(true)
                    .build())
            ),
            Pair.of(
                "controller/order/get/places.json",
                AbstractGetOrderTest::unitsAndItems
            ),
            Pair.of(
                "controller/order/get/contacts.json",
                orderDto -> orderDto.setContacts(List.of(
                    OrderContactDto.builder().build(),
                    OrderDtoFactory.createLomContact()
                        .comment("contact_comment")
                        .build()
                ))
            ),
            Pair.of(
                "controller/order/get/recipient.json",
                orderDto -> orderDto.setRecipient(OrderDtoFactory.createRecipientBuilder().build())
            ),
            Pair.of(
                "controller/order/get/shipment.json",
                orderDto -> orderDto.setWaybill(List.of(
                    OrderDtoFactory.createWaybillSegmentBuilder(
                        LocalDate.of(2020, 2, 2),
                        LocationDto.builder().warehouseId(110L).build(),
                        LocationDto.builder().warehouseId(111L).build(),
                        100L,
                        ShipmentType.IMPORT
                    )
                        .externalId("test-external-id")
                        .segmentStatus(SegmentStatus.PENDING)
                        .waybillSegmentStatusHistory(List.of(new WaybillSegmentStatusHistoryDto(
                            1L,
                            SegmentStatus.PENDING,
                            LocalDate.of(2019, 10, 1)
                                .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                                .toInstant(),
                            LocalDate.of(2019, 10, 1)
                                .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                                .toInstant(),
                            null,
                            null
                        )))
                        .build(),
                    OrderDtoFactory.waybillSegmentWithStatusDto(2).partnerId(200L).partnerType(null).build()
                ))
            ),
            Pair.of(
                "controller/order/get/delivery_option.json",
                orderDto -> orderDto
                    .setDeliveryType(DeliveryType.COURIER)
                    .setCost(CostDto.builder()
                        .tariffId(200L)
                        .delivery(BigDecimal.valueOf(500))
                        .deliveryForCustomer(new BigDecimal(1000))
                        .cashServicePercent(BigDecimal.valueOf(5))
                        .manualDeliveryForCustomer(BigDecimal.valueOf(142L))
                        .services(List.of(
                            OrderDtoFactory.defaultLomOrderService().code(ShipmentOption.CASH_SERVICE).build(),
                            OrderServiceDto.builder()
                                .code(ShipmentOption.DELIVERY)
                                .cost(BigDecimal.valueOf(500L))
                                .customerPay(false)
                                .build()
                        ))
                        .build())
                    .setWaybill(List.of(
                        OrderDtoFactory.waybillSegmentWithStatusDto(1)
                            .partnerId(100L)
                            .externalId("test-external-id")
                            .build(),
                        OrderDtoFactory.waybillSegmentWithStatusDto(2)
                            .partnerId(200L)
                            .externalId("test-external-id-2")
                            .build()
                    ))
            ),
            Pair.of(
                "controller/order/get/delivery_interval.json",
                orderDto -> orderDto.setDeliveryInterval(
                    DeliveryIntervalDto.builder()
                        .deliveryIntervalId(1L)
                        .fromTime(LocalTime.of(10, 0))
                        .toTime(LocalTime.of(15, 0))
                        .build()
                )
            ),
            Pair.of(
                "controller/order/get/delivery_interval_min_max.json",
                orderDto -> orderDto.setDeliveryInterval(
                    DeliveryIntervalDto.builder()
                        .deliveryDateMin(LocalDate.of(2019, 7, 8))
                        .deliveryDateMax(LocalDate.of(2019, 7, 10))
                        .build()
                )
            ),
            Pair.of(
                "controller/order/get/cargo_status.json",
                orderDto -> orderDto
                    .setStatus(OrderStatus.PROCESSING)
                    .setWaybill(List.of(
                        OrderDtoFactory.waybillSegmentWithStatusDto(2, SegmentStatus.OUT).build(),
                        OrderDtoFactory.waybillSegmentWithStatusDto(1, SegmentStatus.IN).build()
                    ))
            ),
            Pair.of(
                "controller/order/get/warehouse_from.json",
                orderDto -> orderDto.setWaybill(List.of(OrderDtoFactory.createWaybillSegmentBuilder(
                    null,
                    OrderDtoFactory.createLocation(110L),
                    null,
                    null,
                    null
                ).build()))
            ),
            Pair.of("controller/order/get/has_labels.json", orderDto -> orderDto.setHasLabels(true)),
            Pair.of(
                "controller/order/get/available_actions.json",
                orderDto -> orderDto.setAvailableActions(
                    OrderActionsDto.builder()
                        .untieFromShipment(true)
                        .generateLabel(true)
                        .cancel(true)
                        .build()
                )
            ),
            Pair.of(
                "controller/order/get/items_multiplace.json",
                orderDto -> unitsAndItems(orderDto).setItems(
                    defaultItems(Set.of("place_external_id_1", "place_external_id_2"))
                )
            )
        ).map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static List<ItemDto> defaultItems(Set<String> placesIds) {
        return List.of(
            ItemDto.builder()
                .boxes(List.of(
                    OrderItemBoxDto.builder()
                        .storageUnitExternalIds(placesIds)
                        .build()
                ))
                .build(),
            ItemDto.builder()
                .name("item_name_2")
                .vendorId(100500L)
                .article("item_article")
                .count(4)
                .price(MonetaryDto.builder().value(BigDecimal.valueOf(100)).build())
                .assessedValue(MonetaryDto.builder().value(BigDecimal.valueOf(1000)).build())
                .vatType(VatType.VAT_10)
                .dimensions(
                    KorobyteDto.builder()
                        .length(10)
                        .height(20)
                        .width(30)
                        .weightGross(BigDecimal.TEN)
                        .build()
                )
                .boxes(List.of(
                    OrderItemBoxDto.builder()
                        .storageUnitExternalIds(Set.of("place_external_id_2"))
                        .build()
                ))
                .supplierInn("inn")
                .build()
        );
    }

    @Nonnull
    private static OrderDto unitsAndItems(OrderDto orderDto) {
        return orderDto
            .setUnits(List.of(
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .dimensions(
                        KorobyteDto.builder()
                            .length(100)
                            .height(200)
                            .width(300)
                            .weightGross(BigDecimal.ONE)
                            .build()
                    )
                    .externalId("root-unit")
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.PLACE)
                    .dimensions(
                        KorobyteDto.builder()
                            .length(100)
                            .height(200)
                            .width(300)
                            .weightGross(BigDecimal.ONE)
                            .build()
                    )
                    .externalId("place_external_id_1")
                    .parentExternalId("root-unit")
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.PLACE)
                    .externalId("place_external_id_2")
                    .parentExternalId("root-unit")
                    .build()
            ))
            .setItems(defaultItems(Set.of("place_external_id_2")));
    }

    private OrderDto defaultOrder() {
        return new OrderDto()
            .setId(orderId)
            .setSenderId(11L)
            .setCreated(Instant.parse("2019-07-03T17:00:00Z"))
            .setStatus(OrderStatus.DRAFT)
            .setPlatformClientId(3L);
    }

    protected abstract ResultActions getOrder() throws Exception;

}
