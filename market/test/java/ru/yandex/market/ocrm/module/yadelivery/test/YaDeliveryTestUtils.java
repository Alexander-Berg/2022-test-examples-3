package ru.yandex.market.ocrm.module.yadelivery.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.catalog.items.CatalogItemService;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusesDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusesDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.ocrm.module.yadelivery.domain.CargoStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryCargoStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryItem;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrderStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrderStatusHistory;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliverySender;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryService;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryShipmentOption;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryVatType;

@Component
public class YaDeliveryTestUtils {
    @Inject
    private LomClient lomClient;
    @Inject
    private BcpService bcpService;
    @Inject
    private CatalogItemService catalogItemService;
    @Inject
    private AttributeTypeService attributeTypeService;

    public YaDeliverySender createYaDeliverySender() {
        return bcpService.create(YaDeliverySender.FQN, Maps.of(YaDeliverySender.LOM_SENDER_ID,
                Randoms.positiveLongValue()));
    }

    public YaDeliveryOrder createYaDeliveryOrder() {
        return createYaDeliveryOrder(Map.of());
    }

    public YaDeliveryOrder createYaDeliveryOrder(@Nonnull Map<String, Object> properties) {
        var map = requiredProperties();
        map.putAll(properties);
        return bcpService.create(YaDeliveryOrder.FQN, map);
    }

    public byte[] testData(String path) {
        return new String(ResourceHelpers.getResource(path)).replaceAll("\n", "").getBytes();
    }

    public List<Map<String, Object>> createYaDeliveryServicesMock(
            YaDeliveryOrder order,
            @Nonnull List<Map<String, Object>> servicesProperties) {
        List<Map<String, Object>> resultProperties = new ArrayList<>();
        List<OrderServiceDto> servicesDto = new ArrayList<>();
        for (Map<String, Object> itemProperties : servicesProperties) {
            Map<String, Object> defProperties = generateYaDeliveryServiceMockProperties(order);
            defProperties.putAll(itemProperties);

            OrderServiceDto serviceDto = createOrderServiceDtoByProperties(defProperties);
            servicesDto.add(serviceDto);
            resultProperties.add(defProperties);
        }

        OrderDto orderDto = new OrderDto();
        orderDto.setId(Randoms.longValue());
        orderDto.setCost(CostDto.builder()
                .services(servicesDto)
                .build());

        Mockito.when(lomClient.getOrder(Mockito.anyLong()))
                .thenReturn(Optional.of(orderDto));

        return resultProperties;
    }

    private Map<String, Object> generateYaDeliveryServiceMockProperties(YaDeliveryOrder order) {
        String shipmentOptionName = Randoms.enumValue(ShipmentOption.class).name();
        YaDeliveryShipmentOption shipmentOption =
                catalogItemService.get(YaDeliveryShipmentOption.FQN, shipmentOptionName);

        return Maps.of(
                YaDeliveryService.PARENT, order,
                YaDeliveryService.CODE, shipmentOption,
                YaDeliveryService.COST, Randoms.bigDecimal(),
                YaDeliveryService.CUSTOMER_PAY, Randoms.booleanValue()
        );
    }

    private OrderServiceDto createOrderServiceDtoByProperties(Map<String, Object> defProperties) {
        YaDeliveryShipmentOption shipmentOption = Maps.unsafeGet(YaDeliveryService.CODE, defProperties);
        ShipmentOption shipmentOptionEnum = ShipmentOption.valueOf(shipmentOption.getCode());

        return OrderServiceDto.builder()
                .code(shipmentOptionEnum)
                .cost(Maps.unsafeGet(YaDeliveryService.COST, defProperties))
                .customerPay(Maps.unsafeGet(YaDeliveryService.CUSTOMER_PAY, defProperties))
                .build();
    }

    public List<Map<String, Object>> createYaDeliveryItemsMock(YaDeliveryOrder order,
                                                               @Nonnull List<Map<String, Object>> itemsProperties) {

        List<Map<String, Object>> resultProperties = new ArrayList<>();
        List<ItemDto> itemsDto = new ArrayList<>();
        for (Map<String, Object> itemProperties : itemsProperties) {
            Map<String, Object> defProperties = generateYaDeliveryItemMockProperties(order);
            defProperties.putAll(itemProperties);

            ItemDto itemDto = createItemDtoByProperties(defProperties);
            itemsDto.add(itemDto);
            resultProperties.add(defProperties);
        }

        OrderDto orderDto = new OrderDto();
        orderDto.setId(Randoms.longValue());
        orderDto.setItems(itemsDto);

        Mockito.when(lomClient.getOrder(Mockito.anyLong()))
                .thenReturn(Optional.of(orderDto));

        return resultProperties;
    }

    private Map<String, Object> generateYaDeliveryItemMockProperties(YaDeliveryOrder order) {
        String vatTypeName = Randoms.enumValue(VatType.class).name();
        YaDeliveryVatType vatType = catalogItemService.get(YaDeliveryVatType.FQN, vatTypeName);

        return Maps.of(
                YaDeliveryItem.PARENT, order,
                YaDeliveryItem.NAME, Randoms.string(),
                YaDeliveryItem.VENDOR_ID, Randoms.positiveLongValue(),
                YaDeliveryItem.ARTICLE, Randoms.string(),
                YaDeliveryItem.COUNT, Randoms.positiveIntValue(),
                YaDeliveryItem.DIMENSIONS_LENGTH, Randoms.positiveIntValue(),
                YaDeliveryItem.DIMENSIONS_HEIGHT, Randoms.positiveIntValue(),
                YaDeliveryItem.DIMENSIONS_WIDTH, Randoms.positiveIntValue(),
                YaDeliveryItem.DIMENSIONS_WEIGHT_GROSS, Randoms.bigDecimal(),
                YaDeliveryItem.PRICE_CURRENCY, "RUB",
                YaDeliveryItem.PRICE_VALUE, Randoms.bigDecimal(),
                YaDeliveryItem.PRICE_EXCHANGE_RATE, Randoms.bigDecimal(),
                YaDeliveryItem.ASSESSED_VALUE_CURRENCY, "RUB",
                YaDeliveryItem.ASSESSED_VALUE_VALUE, Randoms.bigDecimal(),
                YaDeliveryItem.ASSESSED_VALUE_EXCHANGE_RATE, Randoms.bigDecimal(),
                YaDeliveryItem.VAT_TYPE, vatType
        );
    }

    private ItemDto createItemDtoByProperties(Map<String, Object> defProperties) {
        YaDeliveryVatType vatType = Maps.unsafeGet(YaDeliveryItem.VAT_TYPE, defProperties);
        VatType vatTypeEnum = VatType.valueOf(vatType.getCode());
        return ItemDto.builder()
                .name(Maps.unsafeGet(YaDeliveryItem.NAME, defProperties))
                .vendorId(Maps.unsafeGet(YaDeliveryItem.VENDOR_ID, defProperties))
                .article(Maps.unsafeGet(YaDeliveryItem.ARTICLE, defProperties))
                .count(Maps.unsafeGet(YaDeliveryItem.COUNT, defProperties))
                .dimensions(KorobyteDto.builder()
                        .length(Maps.unsafeGet(YaDeliveryItem.DIMENSIONS_LENGTH, defProperties))
                        .height(Maps.unsafeGet(YaDeliveryItem.DIMENSIONS_HEIGHT, defProperties))
                        .width(Maps.unsafeGet(YaDeliveryItem.DIMENSIONS_WIDTH, defProperties))
                        .weightGross(Maps.unsafeGet(YaDeliveryItem.DIMENSIONS_WEIGHT_GROSS, defProperties))
                        .build())
                .price(MonetaryDto.builder()
                        .currency(Maps.unsafeGet(YaDeliveryItem.PRICE_CURRENCY, defProperties))
                        .value(Maps.unsafeGet(YaDeliveryItem.PRICE_VALUE, defProperties))
                        .exchangeRate(Maps.unsafeGet(YaDeliveryItem.PRICE_EXCHANGE_RATE, defProperties))
                        .build())
                .assessedValue(MonetaryDto.builder()
                        .currency(Maps.unsafeGet(YaDeliveryItem.ASSESSED_VALUE_CURRENCY, defProperties))
                        .value(Maps.unsafeGet(YaDeliveryItem.ASSESSED_VALUE_VALUE, defProperties))
                        .exchangeRate(Maps.unsafeGet(YaDeliveryItem.ASSESSED_VALUE_EXCHANGE_RATE, defProperties))
                        .build())
                .vatType(vatTypeEnum)
                .build();

    }

    public Map<String, Object> createYaDeliveryOrderStatusHistoryMock(YaDeliveryOrder order,
                                                                      @Nonnull Map<String, Object> properties) {
        Map<String, Object> defaultProperies = createYaDeliveryOrderStatusHistoryMockProperties(order);
        defaultProperies.putAll(properties);

        OrderStatusesDto orderStatusesDto = createOrderStatusesDtoByMock(defaultProperies);
        Mockito.when(lomClient.getOrdersStatuses(Mockito.any()))
                .thenReturn(Collections.singletonList(orderStatusesDto));

        return defaultProperies;
    }

    private OrderStatusesDto createOrderStatusesDtoByMock(Map<String, Object> defaultProperies) {
        OffsetDateTime dateTime =
                (OffsetDateTime) defaultProperies.get(YaDeliveryOrderStatusHistory.DATETIME);
        YaDeliveryOrderStatus orderStatus =
                (YaDeliveryOrderStatus) defaultProperies.get(YaDeliveryOrderStatusHistory.ORDER_STATUS);
        YaDeliveryCargoStatus cargoStatus =
                (YaDeliveryCargoStatus) defaultProperies.get(YaDeliveryOrderStatusHistory.CARGO_STATUS);

        OrderStatusesDto.OrderStatusesDtoBuilder builder = OrderStatusesDto.builder();
        if (orderStatus != null) {
            OrderStatus orderStatusEnum = OrderStatus.valueOf(orderStatus.getCode());
            builder.globalStatusesHistory(
                    List.of(OrderStatusHistoryDto.builder()
                            .datetime(dateTime.toInstant())
                            .status(orderStatusEnum)
                            .build()));
        } else if (cargoStatus != null) {
            CargoStatus cargoStatusEnum = CargoStatus.valueOf(cargoStatus.getCode());
            Pair<PartnerType, SegmentStatus> pair = calculatePartnerTypeAndSegmentStatus(cargoStatusEnum);
            PartnerType partnerType = pair.getFirst();
            SegmentStatus segmentStatus = pair.getSecond();
            builder.waybillSegmentsHistories(
                    List.of(WaybillSegmentStatusesDto.builder()
                            .partnerType(partnerType)
                            .statusHistory(
                                    List.of(WaybillSegmentStatusHistoryDto.builder()
                                            .created(dateTime.toInstant())
                                            .status(segmentStatus)
                                            .build()))
                            .build())
            );
        }

        return builder.build();
    }

    private Pair<PartnerType, SegmentStatus> calculatePartnerTypeAndSegmentStatus(CargoStatus cargoStatus) {
        for (PartnerType partnerType : PartnerType.values()) {
            for (SegmentStatus segmentStatus : SegmentStatus.values()) {
                CargoStatus currentCargoStatus = CargoStatus.createBasedOn(segmentStatus.name(), partnerType.name());
                if (cargoStatus.equals(currentCargoStatus)) {
                    return Pair.of(partnerType, segmentStatus);
                }
            }
        }

        throw new IllegalStateException("Not found right PartnerType and SegmentStatus pair for: " + cargoStatus);
    }

    @Nonnull
    private Map<String, Object> createYaDeliveryOrderStatusHistoryMockProperties(YaDeliveryOrder order) {
        String orderStatusName = Randoms.enumValue(OrderStatus.class).name();
        var orderStatus = catalogItemService.get(YaDeliveryOrderStatus.FQN, orderStatusName);
        String cargoStatusName = Randoms.enumValue(CargoStatus.class).name();
        var cargoStatus = catalogItemService.get(YaDeliveryCargoStatus.FQN, cargoStatusName);
        return Maps.of(
                YaDeliveryOrderStatusHistory.PARENT, order,
                YaDeliveryOrderStatusHistory.DATETIME, Randoms.dateTime(Dates.MOSCOW_ZONE),
                YaDeliveryOrderStatusHistory.ORDER_STATUS, orderStatus,
                YaDeliveryOrderStatusHistory.CARGO_STATUS, cargoStatus
        );
    }

    @Nonnull
    private Map<String, Object> requiredProperties() {
        var created = Randoms.dateTime();
        var lomOrderId = Randoms.positiveLongValue();
        return Maps.of(HasId.ID, YaDeliveryOrder.constructId(created, lomOrderId),
                YaDeliveryOrder.CREATED, created,
                YaDeliveryOrder.LOM_ORDER_ID, lomOrderId
        );
    }

    public void equalsEntity(Map<String, Object> expectedMap, Entity actualEntity) {
        Assertions.assertNotNull(actualEntity);
        Assertions.assertTrue(expectedMap.size() > 0, "Проверять нечего");

        for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
            String attributeCode = entry.getKey();
            Object expectedValue = entry.getValue();

            Attribute attribute = actualEntity.getMetaclass().getAttribute(attributeCode);
            Object expectedValueWrapped = attributeTypeService.wrap(attribute, expectedValue);

            Assertions.assertEquals(expectedValueWrapped, actualEntity.getAttribute(attributeCode),
                    "Атрибут отличается от ожидаемого: " + attribute.getTitle() + " : " + attribute.getCode());
        }
    }
}
