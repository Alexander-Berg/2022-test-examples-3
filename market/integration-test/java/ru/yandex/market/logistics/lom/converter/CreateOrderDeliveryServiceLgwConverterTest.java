package ru.yandex.market.logistics.lom.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.converter.lgw.CreateOrderLgwConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.entity.embedded.Monetary;
import ru.yandex.market.logistics.lom.entity.embedded.OffsetTimeInterval;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.entity.items.OrderItemBox;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.allServices;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDropshipMovementExpressWaybill;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDropshipMovementPickupCourierWaybill;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDropshipMovementPickupWaybill;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsDeliveryInterval;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegments;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegmentsWithWithdrawShipment;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegmentsWithoutShipmentType;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsWaybillSegment;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsWaybillSegments;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createExpressScReturnWarehouse;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOffsetDateTime;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrderItem;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrderItemBoxStorageUnit;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createStorageUnit;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createDsForTwoSegmentWaybillOrder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createDsOrder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createDsOrderWithAllServices;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createExpressReturnWarehouse;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createItem;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createKorobyte;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createPlace;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createPlaceWithoutExternalId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createReturnWarehouse;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createUnitId;

@DisplayName("Конвертация запроса создания заказа для службы доставки в LGW")
class CreateOrderDeliveryServiceLgwConverterTest extends AbstractContextualTest {

    protected static final long PARTNER_WITH_LINK = 100L;
    protected static final long NOT_PARTNER_WITH_LINK = 90L;

    private static final String BARCODE_GENERATOR_LINK =
        "[Покажите штрих-код заказа у стойки]" +
            "(https://barcode.edadeal.ru/barcode/v3/generate/code128/2-LOinttest-1.png)\n";
    private static final String BARCODE_LINK_HINT_TEMPLATE =
        "В описании к точке есть ссылка на штрих-код заказа. Нажмите на нее и покажите на стойке выдачи заказа";
    private static final Set<String> SEGMENT_TAGS = Set.of(
        "ON_DEMAND",
        "ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED",
        "DEFERRED_COURIER",
        "EXPRESS",
        "DBS",
        "B2B_CUSTOMER",
        "COURIER_PRO",
        "COURIER_CAR",
        "COURIER_PEDESTRIAN",
        "WIDE_INTERVAL",
        "YANDEX_GO",
        "EXPRESS_BATCH",
        "EXPRESS_WAREHOUSE_CLOSING",
        "C2C"
    );

    @Autowired
    private CreateOrderLgwConverter dsLgwConverter;
    @Autowired
    private FeatureProperties featureProperties;

    private StorageUnit rootUnit;
    private StorageUnit place;

    @BeforeEach
    void setupPlaces() {
        place = createStorageUnit(StorageUnitType.PLACE, 1001, 2);
        rootUnit = createStorageUnit(StorageUnitType.ROOT, 1000, 1).setChildren(Set.of(place));
        featureProperties.setBarcodeLinkRequiredPartnerIds(Set.of(PARTNER_WITH_LINK));
        featureProperties.setWithOrderCommentExpressPickup(false)
            .setUseNewFlowForExpressCancellation(false);
    }

    @Test
    @DisplayName("Все услуги")
    void convertOrderAllServices() {
        List<WaybillSegment> waybill = createDsWaybillSegments(rootUnit);
        Order order = createOrder(waybill);
        order.getCost().setServices(allServices());

        softly.assertThat(convertOrder(waybill.get(0)))
            .isEqualTo(createDsOrderWithAllServices().build());
    }

    @Test
    @DisplayName("Без услуг")
    void convertOrderWithoutServices() {
        List<WaybillSegment> waybill = createDsWaybillSegments(rootUnit);
        Order order = createOrder(waybill);
        order.getCost().setServices(ImmutableSortedSet.of());
        softly.assertThat(convertOrder(waybill.get(0))).isEqualTo(createDsOrder(null).build());
    }

    @Test
    @DisplayName("Два сегмента вейбилла")
    void conversionOkWithTwoSegments() {
        List<WaybillSegment> waybill = createDsFfWaybillSegments(rootUnit);
        createOrder(waybill);
        softly.assertThat(convertOrder(waybill.get(1))).isEqualTo(createDsForTwoSegmentWaybillOrder().build());
    }

    @Test
    @DisplayName("Два сегмента вейбилла с отгрузкой типа забор")
    void conversionOkWithTwoSegmentsWithWithdrawShipment() {
        List<WaybillSegment> waybill = createDsFfWaybillSegmentsWithWithdrawShipment(rootUnit);
        createOrder(waybill);
        softly.assertThat(convertOrder(waybill.get(1)))
            .isEqualTo(createDsForTwoSegmentWaybillOrder().setShipmentPointCode("sc-external-id").build());
    }

    @Test
    @DisplayName("Два сегмента вейбилла без типа отгрузки")
    void conversionOkWithTwoSegmentsWithoutShipmentType() {
        List<WaybillSegment> waybill = createDsFfWaybillSegmentsWithoutShipmentType(rootUnit);
        createOrder(waybill);
        softly.assertThat(convertOrder(waybill.get(1)))
            .isEqualTo(createDsForTwoSegmentWaybillOrder().setShipmentPointCode(null).build());
    }

    @Test
    @DisplayName("Дата доставки из интервала")
    void deliveryDateFromIntervalOnly() {
        List<WaybillSegment> waybill = createDsFfWaybillSegments(rootUnit);

        WaybillSegment deliverySegment = waybill.get(1);
        createOrder(waybill).setDeliveryInterval(createDsDeliveryInterval().setDateMin(LocalDate.of(2019, 6, 6)));

        softly.assertThat(dsLgwConverter.toExternal(deliverySegment))
            .isEqualTo(createDsForTwoSegmentWaybillOrder().build());
    }

    @Test
    @DisplayName("Дата доставки для средней мили из даты отгрузки сегмента последней мили")
    void deliveryDateForMiddleMileFromLastMileShipmentDate() {
        List<WaybillSegment> waybill = createDropshipMovementPickupWaybill(rootUnit, "");
        waybill.get(2).getWaybillShipment().setDate(LocalDate.of(2019, 6, 6));

        WaybillSegment deliverySegment = waybill.get(1);
        Order order = createOrder(waybill).setDeliveryInterval(
            createDsDeliveryInterval().setDateMin(LocalDate.of(2019, 6, 13))
        );
        ConverterTestEntitiesFactory.addPersonal(order);
        softly.assertThat(dsLgwConverter.toExternal(deliverySegment))
            .as("Asserting that the DS.createOrder LGW request is valid")
            .isEqualTo(
                CreateLgwDeliveryEntitiesUtils.createOrder(DeliveryType.MOVEMENT)
                    .setShipmentPointCode("externalId-1")
                    .build()
            );
    }

    @Test
    @DisplayName("Дата доставки для средней мили из даты отгрузки сегмента последней мили, когда две последние мили")
    void deliveryDateForMiddleMileFromLastMileShipmentDateWhenTwoLastMiles() {
        List<WaybillSegment> waybill = createDropshipMovementPickupCourierWaybill(rootUnit);
        waybill.get(2).getWaybillShipment().setDate(LocalDate.of(2019, 6, 6));
        waybill.get(3).getWaybillShipment().setDate(LocalDate.of(2019, 6, 7));

        WaybillSegment deliverySegment = waybill.get(1);
        Order order = createOrder(waybill).setDeliveryInterval(
            createDsDeliveryInterval().setDateMin(LocalDate.of(2019, 6, 13))
        );
        ConverterTestEntitiesFactory.addPersonal(order);
        softly.assertThat(dsLgwConverter.toExternal(deliverySegment))
            .as("Asserting that the DS.createOrder LGW request is valid")
            .isEqualTo(
                CreateLgwDeliveryEntitiesUtils.createOrder(DeliveryType.MOVEMENT)
                    .setShipmentPointCode("externalId-1")
                    .build()
            );
    }

    @Test
    @DisplayName("Размеры заказа из корневой единицы хранения")
    void dimensionsFromRootStorageUnit() {
        List<WaybillSegment> waybill = createDsWaybillSegments(createStorageUnit(StorageUnitType.ROOT, 1));
        ConverterTestEntitiesFactory.createOrder("", place).setWaybill(waybill);

        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getKorobyte)
            .isEqualToComparingFieldByFieldRecursively(createKorobyte(1));
    }

    @Test
    @DisplayName("Размеры заказа из товаров заказа")
    void dimensionsFromOrderItems() {
        List<WaybillSegment> waybill = createDsWaybillSegments(null);

        ConverterTestEntitiesFactory.createOrder("", place)
            .setItems(Set.of(
                createOrderItem(2),
                createOrderItem(1)
            ))
            .setWaybill(waybill);

        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getKorobyte)
            .isEqualToComparingFieldByFieldRecursively(createKorobyte(3));
    }

    @Test
    @DisplayName("Товары из товаров заказа")
    void itemsFromOrderItems() {
        List<WaybillSegment> waybill = createDsWaybillSegments(rootUnit);

        ConverterTestEntitiesFactory.createOrder("", place)
            .setWaybill(waybill)
            .setItems(Set.of(
                createOrderItem(2),
                createOrderItem(1)
            ));
        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createItem(1),
                createItem(2)
            ));
    }

    @Test
    @DisplayName("Товары без признака НДС")
    void itemsWithoutVat() {
        List<WaybillSegment> waybill = createDsWaybillSegments(rootUnit);
        ConverterTestEntitiesFactory.createOrder("", place)
            .setWaybill(waybill)
            .setItems(Set.of(createOrderItem(1).setVatType(null)));
        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                CreateLgwDeliveryEntitiesUtils.createItemBuilder(1)
                    .setTaxes(null)
                    .build()
            ));
    }

    @Test
    @DisplayName("Места из единиц хранения")
    void placesFromStorageUnits() {
        List<WaybillSegment> waybill = createDsWaybillSegments(createStorageUnit(StorageUnitType.ROOT, 3, 3)
            .setChildren(Set.of(
                createStorageUnit(StorageUnitType.PLACE, 2),
                createStorageUnit(StorageUnitType.PLACE, 3).setExternalId(null),
                createStorageUnit(StorageUnitType.PLACE, 4).setPartnerId(null),
                createStorageUnit(StorageUnitType.PLACE, 1).setExternalId(null).setPartnerId(null)
            )));

        ConverterTestEntitiesFactory.createOrder("", place)
            .setItems(List.of())
            .setWaybill(waybill);
        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getPlaces)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createPlace(2).build(),
                createPlace(4).setPartnerCodes(null).build(),
                createPlaceWithoutExternalId(1).build(),
                createPlaceWithoutExternalId(3).build()
            ));
    }

    @Test
    @DisplayName("Товары разложены по местам")
    void itemsLinkedToPlaces() {
        StorageUnit firstPlace = createStorageUnit(StorageUnitType.PLACE, 1000, 1);
        StorageUnit secondPlace = createStorageUnit(StorageUnitType.PLACE, 1001, 2);
        List<WaybillSegment> waybill = createDsWaybillSegments(createStorageUnit(StorageUnitType.ROOT, 1002, 3)
            .setChildren(Set.of(
                secondPlace,
                firstPlace
            )));

        ConverterTestEntitiesFactory.createOrder("", place)
            .setWaybill(waybill)
            .setItems(Set.of(
                createOrderItem(2)
                    .setBoxes(Set.of(new OrderItemBox().setUnits(
                        Set.of(createOrderItemBoxStorageUnit(firstPlace, 2))
                    ))),
                createOrderItem(3)
                    .setBoxes(Set.of(
                        new OrderItemBox().setUnits(
                            Set.of(createOrderItemBoxStorageUnit(secondPlace, 1))
                        ),
                        new OrderItemBox().setUnits(
                            Set.of(createOrderItemBoxStorageUnit(firstPlace, 2))
                        )
                    ))
            ));

        softly.assertThat(convertOrder(waybill.get(0)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getPlaces)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createPlace(1000, 1)
                    .setItemPlaces(List.of(
                        new ItemPlace(createUnitId(2), 2),
                        new ItemPlace(createUnitId(3), 2)
                    ))
                    .build(),
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(3), 1)))
                    .build()
            ));
    }

    @Test
    @DisplayName("Все услуги")
    void convertLocationToInboundInterval() {
        // В заказе deliveryInterval с 14:00 до 17:00
        WaybillSegment waybillSegment = createDsWaybillSegments(rootUnit).get(0);
        createOrder(List.of(waybillSegment));

        // Меняем в сегменте интервал приемки в локации получения на 10:00 - 14:00
        waybillSegment.getWaybillShipment().getLocationTo().setInboundInterval(
            new OffsetTimeInterval().setFrom(LocalTime.of(10, 0)).setTo(LocalTime.of(14, 0))
        );

        // По ds-api передаются параметры из сегмента, а не из заказа
        softly.assertThat(convertOrder(waybillSegment))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getDeliveryInterval)
            .isEqualToComparingFieldByFieldRecursively(
                ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval.of(
                    LocalTime.of(10, 0),
                    LocalTime.of(14, 0)
                )
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("addressArguments")
    @DisplayName("Проставляется корректный адрес в зависимости от типа сегмента")
    void convertLocationToInboundCheckAddress(SegmentType segmentType, String locationPrefix) {
        WaybillSegment waybillSegment = createDsWaybillSegments(rootUnit).get(0);
        waybillSegment.setSegmentType(segmentType).getWaybillShipment()
            .setLocationTo(new ru.yandex.market.logistics.lom.entity.Location().setType(LocationType.WAREHOUSE));

        createOrder(List.of(waybillSegment));
        softly.assertThat(convertOrder(waybillSegment))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getLocationTo)
            .isEqualToComparingFieldByFieldRecursively(
                new Location.LocationBuilder("Россия", locationPrefix + "-settlement", "Московская область")
                    .setBuilding(locationPrefix + "-building")
                    .setHouse(locationPrefix + "-house")
                    .setHousing(locationPrefix + "-housing")
                    .setRoom(locationPrefix + "-apartment")
                    .setSettlement(locationPrefix + "-settlement")
                    .setStreet(locationPrefix + "-street")
                    .setLocationId(5)
                    .setLng(BigDecimal.valueOf(5))
                    .setLat(BigDecimal.valueOf(5))
                    .build()
            );

    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = SegmentType.class,
        names = {"POST", "PICKUP", "COURIER", "GO_PLATFORM"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Исключение, если для типа сегмента недоступна отгрузка самопривозом напрямую в службу доставки")
    void convertLocationToInboundCheckAddressInvalid(SegmentType segmentType) {
        WaybillSegment waybillSegment = createDsWaybillSegments(rootUnit).get(0);
        waybillSegment.setSegmentType(segmentType).getWaybillShipment()
            .setLocationTo(new ru.yandex.market.logistics.lom.entity.Location().setType(LocationType.WAREHOUSE));

        createOrder(List.of(waybillSegment));

        assertThatThrownBy(() -> convertOrder(waybillSegment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Unsupported segment type " + segmentType.name());
        assertThrows(IllegalStateException.class, () -> convertOrder(waybillSegment));
    }

    @Test
    @DisplayName("Теги сегмента")
    void convertTags() {
        WaybillSegment waybillSegment = createDsWaybillSegment(rootUnit).addTags(Set.of(WaybillSegmentTag.values()));
        Order order = createOrder(List.of(waybillSegment));
        order.getCost().setServices(allServices());

        softly.assertThat(convertOrder(waybillSegment).getTags())
            .containsExactlyInAnyOrderElementsOf(SEGMENT_TAGS);
    }

    @Test
    @DisplayName("Интервал доставки со сдвигом из локации")
    void convertDeliveryIntervalWithOffsetFromLocation() {
        LocalTime fromLocation = LocalTime.of(10, 30);
        LocalTime toLocation = LocalTime.of(12, 30);

        LocalTime fromOrder = LocalTime.of(14, 30);
        LocalTime toOrder = LocalTime.of(16, 30);

        WaybillSegment waybillSegment = createDsWaybillSegment(rootUnit);
        waybillSegment.getWaybillShipment().getLocationTo().setInboundInterval(
            new OffsetTimeInterval(fromLocation, toLocation, 10800)
        );

        Order order = createOrder(List.of(waybillSegment))
            .setDeliveryInterval(new DeliveryInterval().setStartTime(fromOrder).setEndTime(toOrder).setTzOffset(14400));
        order.getRecipient().getAddress().setGeoId(65);

        ZoneOffset zoneOffset = ZoneOffset.ofHours(3);
        softly.assertThat(convertOrder(waybillSegment).getDeliveryInterval())
            .usingRecursiveComparison()
            .isEqualTo(ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval.of(
                OffsetTime.of(fromLocation, zoneOffset),
                OffsetTime.of(toLocation, zoneOffset)
            ));
    }

    @Test
    @DisplayName("Интервал доставки со сдвигом из интервала в заказе")
    void convertDeliveryIntervalWithOffsetFromDeliveryInterval() {
        LocalTime fromOrder = LocalTime.of(14, 30);
        LocalTime toOrder = LocalTime.of(16, 30);

        WaybillSegment waybillSegment = createDsWaybillSegment(rootUnit);

        Order order = createOrder(List.of(waybillSegment))
            .setDeliveryInterval(new DeliveryInterval().setStartTime(fromOrder).setEndTime(toOrder).setTzOffset(14400));
        order.getRecipient().getAddress().setGeoId(65);

        ZoneOffset zoneOffset = ZoneOffset.ofHours(4);
        softly.assertThat(convertOrder(waybillSegment).getDeliveryInterval())
            .usingRecursiveComparison()
            .isEqualTo(ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval.of(
                OffsetTime.of(fromOrder, zoneOffset),
                OffsetTime.of(toOrder, zoneOffset)
            ));
    }

    @Test
    @DisplayName("Конвертация комментария - нет ff-сегмента")
    void convertCommentWithoutFf() {
        WaybillSegment segment = createDsWaybillSegment(rootUnit).setPartnerSubtype(PartnerSubtype.TAXI_EXPRESS);

        createOrder(List.of(segment));
        softly.assertThat(convertOrder(segment))
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getComment)
            .isEqualTo("test-comment");
    }

    @Test
    @DisplayName("Название товара для взрослых скрывается при конвертации")
    void convertWithAdultItem() {
        WaybillSegment segment = createDsWaybillSegment(rootUnit);

        createOrder(List.of(segment))
            .setItems(Set.of(
                new OrderItem()
                    .setCargoTypes(Set.of(CargoType.ADULT))
                    .setMsku(69L)
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(segment).getItems())
            .extracting(Item::getName)
            .containsExactly("Аксессуар, 69");
    }

    @Test
    @DisplayName("Название товара не из категории интим не скрывается при конвертации")
    void convertWithoutAdultItem() {
        WaybillSegment segment = createDsWaybillSegment(rootUnit);

        createOrder(List.of(segment))
            .setItems(Set.of(
                new OrderItem()
                    .setName("Item_96")
                    .setCargoTypes(Set.of(CargoType.TECH_AND_ELECTRONICS))
                    .setMsku(96L)
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(segment).getItems())
            .extracting(Item::getName)
            .containsExactly("Item_96");
    }

    @Test
    @DisplayName("Название товара скрывается для экспресс доставки")
    void convertWithExpressTag() {
        WaybillSegment segment = createDsWaybillSegment(rootUnit).addTag(WaybillSegmentTag.EXPRESS);

        createOrder(List.of(segment))
            .setItems(Set.of(
                new OrderItem()
                    .setName("Item_96")
                    .setCargoTypes(Set.of(CargoType.TECH_AND_ELECTRONICS))
                    .setMsku(96L)
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(segment).getItems())
            .extracting(Item::getName)
            .containsExactly("Аксессуар, 96");
    }

    @Test
    @DisplayName("Название товара не скрывается с отключенной property")
    void convertWithExpressHidingOff() {
        featureProperties.setHideItemNameForExpress(false);

        WaybillSegment segment = createDsWaybillSegment(rootUnit).addTag(WaybillSegmentTag.EXPRESS);

        createOrder(List.of(segment))
            .setItems(Set.of(
                new OrderItem()
                    .setName("Item_96")
                    .setCargoTypes(Set.of(CargoType.TECH_AND_ELECTRONICS))
                    .setMsku(96L)
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(segment).getItems())
            .extracting(Item::getName)
            .containsExactly("Item_96");

        featureProperties.setHideItemNameForExpress(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Конвертация возвратного склада для экспресс сегмента")
    void convertReturnScForExpress(boolean useNewFlow) {
        featureProperties.setUseNewFlowForExpressCancellation(useNewFlow);
        List<WaybillSegment> waybill = createDropshipMovementExpressWaybill(
            rootUnit,
            "ext-id",
            SegmentType.COURIER
        );
        createOrder(waybill);

        waybill.get(1).setReturnWarehouseLocation(createExpressScReturnWarehouse());

        var actual = convertOrder(waybill.get(1));

        softly.assertThat(actual)
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getWarehouse)
            .isEqualTo(
                useNewFlow
                    ? createExpressReturnWarehouse().build()
                    : createReturnWarehouse().build()
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация комментария")
    void convertComment(
        @SuppressWarnings("unused") String name,
        String instruction,
        String comment,
        Long dropshipId,
        PartnerSubtype subtype,
        String expectedInstruction,
        String expectedComment
    ) {
        List<WaybillSegment> waybill = createDsFfWaybillSegments(rootUnit, dropshipId, subtype, instruction);
        createOrder(waybill).setComment(comment);

        ru.yandex.market.logistic.gateway.common.model.delivery.Order actual = convertOrder(waybill.get(1));
        softly.assertThat(actual.getWarehouseFrom().getInstruction())
            .isEqualTo(expectedInstruction);

        softly.assertThat(actual)
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getComment)
            .isEqualTo(expectedComment);
    }

    @Nonnull
    private static Stream<Arguments> convertComment() {
        return Stream.of(
            Arguments.of(
                "Есть комментарий, нет сегмента YaGo и дропшипу не нужна ссылка",
                "test-comment",
                "test-order-comment",
                NOT_PARTNER_WITH_LINK,
                null,
                "test-comment",
                "test-order-comment"
            ),
            Arguments.of(
                "Есть комментарий, нет сегмента YaGo и дропшипу нужна ссылка",
                "test-comment",
                "test-order-comment",
                PARTNER_WITH_LINK,
                null,
                "test-comment",
                "test-order-comment"
            ),
            Arguments.of(
                "Есть комментарий, есть сегмент YaGo и дропшипу не нужна ссылка",
                "test-comment",
                "test-order-comment",
                NOT_PARTNER_WITH_LINK,
                PartnerSubtype.TAXI_EXPRESS,
                "test-comment",
                "test-order-comment"
            ),
            Arguments.of(
                "Есть комментарий, есть сегмент YaGo и дропшипу нужна ссылка",
                "test-comment",
                "test-order-comment",
                PARTNER_WITH_LINK,
                PartnerSubtype.TAXI_EXPRESS,
                BARCODE_GENERATOR_LINK + "test-comment",
                BARCODE_LINK_HINT_TEMPLATE + "\ntest-order-comment"
            ),
            Arguments.of(
                "Есть комментарий, есть сегмент с другим подтипом и дропшипу нужна ссылка",
                "test-comment",
                "test-order-comment",
                PARTNER_WITH_LINK,
                PartnerSubtype.MARKET_LOCKER,
                "test-comment",
                "test-order-comment"
            ),
            Arguments.of(
                "Нет комментария, нет сегмента YaGo и дропшипу не нужна ссылка",
                null,
                null,
                NOT_PARTNER_WITH_LINK,
                null,
                null,
                null
            ),
            Arguments.of(
                "Нет комментария, нет сегмента YaGo и дропшипу нужна ссылка",
                null,
                null,
                PARTNER_WITH_LINK,
                null,
                null,
                null
            ),
            Arguments.of(
                "Нет комментария, есть сегмент YaGo и дропшипу не нужна ссылка",
                null,
                null,
                NOT_PARTNER_WITH_LINK,
                PartnerSubtype.TAXI_EXPRESS,
                null,
                null
            ),
            Arguments.of(
                "Нет комментария, есть сегмент YaGo и дропшипу нужна ссылка",
                null,
                null,
                PARTNER_WITH_LINK,
                PartnerSubtype.TAXI_EXPRESS,
                BARCODE_GENERATOR_LINK,
                BARCODE_LINK_HINT_TEMPLATE
            ),
            Arguments.of(
                "Нет комментария, есть сегмент другого подтипа и дропшипу нужна ссылка",
                null,
                null,
                PARTNER_WITH_LINK,
                PartnerSubtype.DARKSTORE,
                null,
                null
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация экспресс заказа с проставлением shipmentDateTime")
    void convertExpressWithShipmentDateTime(String name, OffsetDateTime shipmentDateTime) {
        List<WaybillSegment> waybill = createDropshipMovementExpressWaybill(rootUnit, "ext-id", SegmentType.COURIER);
        Order order = createOrder(waybill);
        waybill.get(1).getWaybillShipment().setDateTime(shipmentDateTime);
        ConverterTestEntitiesFactory.addPersonal(order);

        var expected = CreateLgwDeliveryEntitiesUtils.createOrder(
                createResourceId("2-LOinttest-1", "ext-id"),
                DeliveryType.COURIER,
                List.of(createItem(1, "Аксессуар, null"))
            )
            .setTags(Set.of("EXPRESS"))
            .setShipmentDateTime(
                shipmentDateTime != null ? DateTime.fromOffsetDateTime(shipmentDateTime) : null
            )
            .setShipmentPointCode("externalId-1")
            .build();

        softly.assertThat(convertOrder(waybill.get(1)))
            .as("Asserting that the DS.createOrder LGW request is valid")
            .isEqualTo(expected);
    }

    @Nonnull
    private static Stream<Arguments> convertExpressWithShipmentDateTime() {
        return Stream.of(
            Arguments.of(
                "Есть shipmentDateTime на сегменте, проставляем в заказ dsApi",
                createOffsetDateTime()
            ),
            Arguments.of("Нет shipmentDateTime на сегменте, не проставляем в заказ dsApi", null)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация комментария для экспресс пвз")
    void convertExpressPickupComment(
        @SuppressWarnings("unused") String name,
        boolean replaceDisabled,
        long partnerId,
        Set<WaybillSegmentTag> tags,
        String comment,
        String instruction,
        String expectedComment
    ) {
        featureProperties.setWithOrderCommentExpressPickup(replaceDisabled);
        List<WaybillSegment> waybill = createDropshipMovementPickupWaybill(rootUnit, instruction);
        waybill.get(0).setPartnerId(partnerId);
        createOrder(waybill).setComment(comment);

        var actual = convertOrder(waybill.get(1).addTags(tags));

        softly.assertThat(actual)
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getComment)
            .isEqualTo(expectedComment);
    }

    @Nonnull
    private static Stream<Arguments> convertExpressPickupComment() {
        return Stream.of(
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, не затираем коммент, не нужна ссылка",
                true,
                NOT_PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                "order comment",
                "pickup instruction",
                "order comment\npickup instruction"
            ),
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, не затираем коммент, нужна ссылка",
                true,
                PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                "order comment",
                "pickup instruction",
                BARCODE_LINK_HINT_TEMPLATE + "\norder comment\npickup instruction"
            ),
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, затираем коммент, не нужна ссылка",
                false,
                NOT_PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                "order comment",
                "pickup instruction",
                "pickup instruction"
            ),
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, затираем коммент, нужна ссылка",
                false,
                PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                "order comment",
                "pickup instruction",
                BARCODE_LINK_HINT_TEMPLATE + "\npickup instruction"
            ),
            Arguments.of(
                "Не сегмент средней мили экспресса в пвз (нет тэга), не затираем коммент, не нужна ссылка",
                true,
                NOT_PARTNER_WITH_LINK,
                Set.of(),
                "order comment",
                "pickup instruction",
                "order comment"
            ),
            Arguments.of(
                "Не сегмент средней мили экспресса в пвз (нет тэга), не затираем коммент, нужна ссылка",
                true,
                PARTNER_WITH_LINK,
                Set.of(),
                "order comment",
                "pickup instruction",
                BARCODE_LINK_HINT_TEMPLATE + "\norder comment"
            ),
            Arguments.of(
                "Не сегмент средней мили экспресса в пвз (нет тэга), затираем коммент, не нужна ссылка",
                false,
                NOT_PARTNER_WITH_LINK,
                Set.of(),
                "order comment",
                "pickup instruction",
                "order comment"
            ),
            Arguments.of(
                "Не сегмент средней мили экспресса в пвз (нет тэга), затираем коммент, нужна ссылка",
                false,
                PARTNER_WITH_LINK,
                Set.of(),
                "order comment",
                "pickup instruction",
                BARCODE_LINK_HINT_TEMPLATE + "\norder comment"
            ),
            //проверяем null
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, не затираем коммент, нужна ссылка, нет инструкции",
                true,
                PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                "order comment",
                null,
                BARCODE_LINK_HINT_TEMPLATE + "\norder comment"
            ),
            Arguments.of(
                "Сегмент средней мили экспресса в пвз, не затираем коммент, нужна ссылка, нет комментария",
                true,
                PARTNER_WITH_LINK,
                Set.of(WaybillSegmentTag.CALL_COURIER),
                null,
                "pickup instruction",
                BARCODE_LINK_HINT_TEMPLATE + "\npickup instruction"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация комментария для экспресс сегмента")
    void convertCourierExpressComment(
        @SuppressWarnings("unused") String name,
        boolean replaceDisabled,
        SegmentType expressSegmentType,
        String comment,
        String expressSegmentExternalId,
        String dropshipExternalId,
        String expectedComment
    ) {
        featureProperties.setWithOrderCommentExpress(replaceDisabled);
        List<WaybillSegment> waybill = createDropshipMovementExpressWaybill(
            rootUnit,
            expressSegmentExternalId,
            expressSegmentType
        );
        createOrder(waybill).setComment(comment).setExternalId(dropshipExternalId);

        var actual = convertOrder(waybill.get(1));

        softly.assertThat(actual)
            .extracting(ru.yandex.market.logistic.gateway.common.model.delivery.Order::getComment)
            .isEqualTo(expectedComment);
    }

    @Nonnull
    private static Stream<Arguments> convertCourierExpressComment() {
        return Stream.of(
            Arguments.of(
                "Сегмент экспресса, с вызовом курьера, с различными externalId у сегмента dropship и express, " +
                    "не затираем коммент",
                true,
                SegmentType.COURIER,
                "order comment",
                "expressSegmentExternalId123412341234",
                "dropshipExternalid123412341234",
                "order comment\nShopOrderId: dropshipExternalid123412341234"
            ),
            Arguments.of(
                "Сегмент экспресса, по платформе GO, с различными externalId у сегмента dropship и express, " +
                    "не затираем коммент",
                true,
                SegmentType.GO_PLATFORM,
                "order comment",
                "expressSegmentExternalId123412341234",
                "dropshipExternalid123412341234",
                "order comment"
            ),
            Arguments.of(
                "Сегмент экспресса, с вызовом курьера без externalId у сегмента dropship, не затираем коммент",
                true,
                SegmentType.COURIER,
                "order comment",
                "expressSegmentExternalId123412341234",
                null,
                "order comment"
            ),
            Arguments.of(
                "Сегмент экспресса, с вызовом курьера, с одиноковыми externalId у сегмента dropship и express, " +
                    "не затираем коммент",
                true,
                SegmentType.COURIER,
                "order comment",
                "123412341234",
                "123412341234",
                "order comment"
            ),
            Arguments.of(
                "Сегмент экспресса, с вызовом курьера, с различными externalId у сегмента dropship и express, " +
                    "затираем коммент",
                false,
                SegmentType.COURIER,
                "order comment",
                "expressSegmentExternalId123412341234",
                "dropshipExternalid123412341234",
                "order comment"
            )
        );
    }

    @Test
    @DisplayName("Услуги на вейбилле")
    void waybillSegmentOptions() {
        WaybillSegment waybillSegment = createDsWaybillSegment(rootUnit)
            .setOptions(ImmutableSortedSet.of(ShipmentOption.TRYING));
        Order order = createOrder(List.of(waybillSegment));
        order.getCost().setServices(ImmutableSortedSet.of(
            new Cost.ServiceCost()
                .setCost(new BigDecimal("200"))
                .setCode(ShipmentOption.WAIT_20)
        ));

        softly.assertThat(convertOrder(waybillSegment).getServices())
            .containsExactly(
                new Service.ServiceBuilder(false)
                    .setCode(ServiceType.WAIT_20)
                    .setCost(200.0)
                    .build(),
                new Service.ServiceBuilder(true)
                    .setCode(ServiceType.TRYING)
                    .build()
            );
    }

    @Nonnull
    private Order createOrder(List<WaybillSegment> waybill) {
        Order order = ConverterTestEntitiesFactory.createOrder("2-LOinttest-1", place).setWaybill(waybill);
        order.getPickupPoint().setAddress(ConverterTestEntitiesFactory.createPickupAddress());
        order.getRecipient().setAddress(ConverterTestEntitiesFactory.createRecipientAddress());
        return order;
    }

    @Nonnull
    private ru.yandex.market.logistic.gateway.common.model.delivery.Order convertOrder(WaybillSegment segment) {
        return dsLgwConverter.toExternal(segment);
    }

    @Nonnull
    private static Stream<Arguments> addressArguments() {
        return Stream.of(
            Arguments.of(SegmentType.POST, "recipient"),
            Arguments.of(SegmentType.COURIER, "recipient"),
            Arguments.of(SegmentType.GO_PLATFORM, "recipient"),
            Arguments.of(SegmentType.PICKUP, "pickup")
        );
    }

    @Nonnull
    private static Monetary createMonetary() {
        return new Monetary()
            .setCurrency("RUB")
            .setExchangeRate(BigDecimal.ONE)
            .setValue(BigDecimal.TEN.multiply(BigDecimal.valueOf(1)));
    }
}
