package ru.yandex.market.logistics.lom.converter;

import java.math.BigDecimal;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PersonalPhone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PersonalPhysicalPersonSender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PhysicalPersonSender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.converter.lgw.fulfillment.CreateOrderLgwFulfillmentConverter;
import ru.yandex.market.logistics.lom.entity.Contact;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderContact;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Fio;
import ru.yandex.market.logistics.lom.entity.embedded.Monetary;
import ru.yandex.market.logistics.lom.entity.enums.ContactType;
import ru.yandex.market.logistics.lom.entity.enums.ItemUnitOperationType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.entity.items.OrderItemBox;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory;

import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegments;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegmentsMultiSc;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegmentsWithDropoff;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrder;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrderItem;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrderItemBoxStorageUnit;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createStorageUnit;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createItem;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createItemBuilder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createKorobyte;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createPlace;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createPlaceWithoutExternalId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createScOrderWithAllServices;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createScOrderWithServices;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createScOrderWithTariff;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createUnitId;

@DisplayName("Конвертация запроса создания заказа для склада в LGW")
class CreateOrderLgwFulfillmentConverterTest extends AbstractContextualTest {

    private static final Set<String> SEGMENT_TAGS = Set.of(
        "EXPRESS",
        "ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED",
        "YANDEX_GO",
        "C2C"
    );

    @Autowired
    private CreateOrderLgwFulfillmentConverter ffLgwConverter;
    @Autowired
    private FeatureProperties featureProperties;

    private List<WaybillSegment> waybill;
    private List<WaybillSegment> waybillNoSc;
    private List<WaybillSegment> waybillMultiSc;
    private List<WaybillSegment> waybillWithDropoff;
    private WaybillSegment sortingCenterSegment;
    private WaybillSegment deliverySegment;
    private StorageUnit place;

    @BeforeEach
    void setupSegments() {
        place = createStorageUnit(StorageUnitType.PLACE, 1001, 2);
        StorageUnit root = createStorageUnit(StorageUnitType.ROOT, 1000, 1).setChildren(Set.of(place));

        waybill = createDsFfWaybillSegments(root);
        sortingCenterSegment = waybill.get(0);
        deliverySegment = waybill.get(1);
        waybillNoSc = List.of(deliverySegment);
        waybillMultiSc = createDsFfWaybillSegmentsMultiSc(root);
        waybillWithDropoff = createDsFfWaybillSegmentsWithDropoff(root);
    }

    @Test
    @DisplayName("Все услуги")
    void convertWithAllServices() {
        Order testOrder = createOrder("2-LOinttest-1", place);
        testOrder.getCost().setServices(ConverterTestEntitiesFactory.allServices());
        ConverterTestEntitiesFactory.addPersonal(testOrder);
        deliverySegment.setExternalId("ds-external-id");

        softly.assertThat(convertOrder(testOrder))
            .isEqualTo(createScOrderWithAllServices());
    }

    @Test
    @DisplayName("Без услуг")
    void convertWithoutServices() {
        Order testOrder = createOrder("2-LOinttest-1", place);
        testOrder.getCost().setServices(ImmutableSortedSet.of());
        ConverterTestEntitiesFactory.addPersonal(testOrder);

        deliverySegment.setExternalId("ds-external-id");

        softly.assertThat(convertOrder(testOrder))
            .isEqualTo(createScOrderWithServices(null).build());
    }

    @Test
    @DisplayName("Без сц сегмента")
    void convertNoSc() {
        Order testOrder = createOrder("2-LOinttest-1", place);

        testOrder.getCost().setServices(ImmutableSortedSet.of());
        ConverterTestEntitiesFactory.addPersonal(testOrder);
        deliverySegment.setExternalId("ds-external-id");

        softly.assertThat(convertOrder(testOrder, waybillNoSc))
            .isEqualTo(createScOrderWithServices(null).build());
    }

    @Test
    @DisplayName("С кодом тарифа")
    void convertWithTariffCode() {
        Order testOrder = createOrder("2-LOinttest-1", place);
        testOrder.getCost().setTariffCode("custom-tariff-code");
        ConverterTestEntitiesFactory.addPersonal(testOrder);
        deliverySegment.setExternalId("ds-external-id");

        softly.assertThat(convertOrder(testOrder))
            .isEqualTo(createScOrderWithTariff("custom-tariff-code").build());
    }

    @Test
    @DisplayName("Размеры заказа из корневой единицы хранения")
    void dimensionsFromRootStorageUnit() {
        softly.assertThat(convertOrder(createOrder("", place)))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getKorobyte)
            .isEqualToComparingFieldByFieldRecursively(createKorobyte(1));
    }

    @Test
    @DisplayName("Размеры заказа из товаров заказа")
    void dimensionsFromOrderItems() {
        Order order = createOrder("", place)
            .setItems(Set.of(
                createOrderItem(2),
                createOrderItem(1)
            ));
        sortingCenterSegment.setStorageUnit(null);
        deliverySegment.setStorageUnit(null);

        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getKorobyte)
            .isEqualToComparingFieldByFieldRecursively(createKorobyte(3));
    }

    @Test
    @DisplayName("Товары из товаров заказа")
    void itemsFromOrderItems() {
        Order order = createOrder("", place)
            .setItems(Set.of(
                createOrderItem(2),
                createOrderItem(1)
            ));
        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createItem(1, null),
                createItem(2, null)
            ));
    }

    @Test
    @DisplayName("Товары без признака НДС")
    void itemsWithoutVat() {
        Order order = createOrder("", place)
            .setItems(Set.of(createOrderItem(1).setVatType(null)));
        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createItemBuilder(1, null, null, createUnitId(1), null)
                    .setTax(null)
                    .build()
            ));
    }

    @Test
    @DisplayName("Места из единиц хранения")
    void placesFromStorageUnits() {
        sortingCenterSegment.setStorageUnit(
            createStorageUnit(StorageUnitType.ROOT, 3)
                .setChildren(Set.of(
                    createStorageUnit(StorageUnitType.PLACE, 2),
                    createStorageUnit(StorageUnitType.PLACE, 3).setExternalId(null),
                    createStorageUnit(StorageUnitType.PLACE, 4).setPartnerId(null),
                    createStorageUnit(StorageUnitType.PLACE, 1).setExternalId(null).setPartnerId(null)
                ))
        );

        softly.assertThat(convertOrder(createOrder("", place).setItems(List.of())))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getPlaces)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createPlace(2).build(),
                createPlace(4).setPartnerCodes(null).build(),
                createPlaceWithoutExternalId(1).setPartnerCodes(null).build(),
                createPlaceWithoutExternalId(3).setPartnerCodes(null).build()
            ));
    }

    @Test
    @DisplayName("Товары разложены по местам")
    void itemsLinkedToPlaces() {
        StorageUnit firstPlace = createStorageUnit(StorageUnitType.PLACE, 1000, 1);
        StorageUnit secondPlace = createStorageUnit(StorageUnitType.PLACE, 1001, 2);
        sortingCenterSegment.setStorageUnit(
            createStorageUnit(StorageUnitType.ROOT, 1002, 3)
                .setChildren(Set.of(
                    secondPlace,
                    firstPlace
                ))
        );

        Order order = createOrder("", place)
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

        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getPlaces)
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
    @DisplayName("Разметка возможности удаления товаров")
    void itemsAutoRemoveFields() {
        Order order = createOrder("", place)
            .setItems(Set.of(
                createOrderItem(2, true),
                createOrderItem(1, false)
            ));
        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createItemBuilder(1, null, false, createUnitId(1), null).build(),
                createItemBuilder(2, null, true, createUnitId(2), null).build()
            ));
    }

    @Test
    @DisplayName("Разметка признака кроссдочности")
    void withCrossDockItem() {
        Order order = createOrder("", place)
            .setItems(Set.of(
                createOrderItem(1),
                createOrderItem(2).setItemUnitOperationType(ItemUnitOperationType.FULFILLMENT),
                createOrderItem(3).setItemUnitOperationType(ItemUnitOperationType.CROSSDOCK)
            ));
        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getItems)
            .isEqualToComparingFieldByFieldRecursively(List.of(
                createItemBuilder(1, null, null, createUnitId(1), null)
                    .setUnitOperationType(UnitOperationType.FULFILLMENT)
                    .build(),
                createItemBuilder(2, null, null, createUnitId(2), null)
                    .setUnitOperationType(UnitOperationType.FULFILLMENT)
                    .build(),
                createItemBuilder(3, null, null, createUnitId(3), null)
                    .setUnitOperationType(UnitOperationType.CROSSDOCK)
                    .build()
            ));
    }

    @Test
    @DisplayName("Возвратный сегмент совпадает с прямым СЦ сегментом")
    void returnSegmentSameAsForward() {
        Order order = createOrder("", place).setReturnSortingCenterId(2L);
        softly.assertThat(convertOrder(order))
            .extracting(o -> o.getWarehouse().getWarehouseId())
            .isEqualToComparingFieldByFieldRecursively(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId("4")
                    .setPartnerId("shop-external-id")
                    .build()
            );
    }

    @Test
    @DisplayName("Возвратный сегмент отличается от прямого СЦ сегмента")
    void returnSegmentDifferentFromForward() {
        Order order = createOrder("", place);
        softly.assertThat(convertOrder(order))
            .extracting(o -> o.getWarehouse().getWarehouseId())
            .isEqualToComparingFieldByFieldRecursively(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId("1")
                    .setPartnerId("return-external-id")
                    .build()
            );
    }

    @Test
    @DisplayName("Теги сегмента")
    void convertTags() {
        sortingCenterSegment.addTags(Set.of(WaybillSegmentTag.values()));
        Order testOrder = createOrder("2-LOinttest-1", place);

        softly.assertThat(convertOrder(testOrder).getTags()).containsExactlyInAnyOrderElementsOf(SEGMENT_TAGS);
    }

    @Test
    @DisplayName("Услуги сегмента")
    void convertOptions() {
        sortingCenterSegment.setOptions(ImmutableSortedSet.of(ShipmentOption.PARTIAL_RETURN));
        Order testOrder = createOrder("2-LOinttest-1", place);

        softly.assertThat(convertOrder(testOrder).getServices())
            .extracting(Service::getCode)
            .containsExactly(ServiceType.PARTIAL_RETURN);
    }

    @Test
    @DisplayName("Название товара для взрослых скрывается при конвертации")
    void convertWithAdultItem() {
        Order order = createOrder("2-LOinttest-1", place)
            .setItems(Set.of(
                new OrderItem()
                    .setCargoTypes(Set.of(CargoType.ADULT))
                    .setMsku(69L)
                    .setName("test")
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(order).getItems())
            .extracting(Item::getName)
            .containsExactly("Аксессуар, 69");
    }

    @Test
    @DisplayName("Название товара не из категории интим не скрывается при конвертации")
    void convertWithoutAdultItem() {
        Order order = createOrder("2-LOinttest-1", place)
            .setItems(Set.of(
                new OrderItem()
                    .setCargoTypes(Set.of(CargoType.TECH_AND_ELECTRONICS))
                    .setMsku(69L)
                    .setName("test")
                    .setPrice(createMonetary())
            ));

        softly.assertThat(convertOrder(order).getItems())
            .extracting(Item::getName)
            .containsExactly("test");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("returnWarehouseChainArgs")
    @DisplayName("Выставляются корректные возвратные склады в цепочке из 3+ СЦ")
    void returnWarehouseLongChain(int idx, String expectedReturnYaId) {

        Order order = createOrder("2-LOinttest-1", place)
            .setWaybill(waybillMultiSc)
            .setReturnSortingCenterId(3L);

        order.getReturnSortingCenterWarehouse().setWarehouseId(204L);

        softly.assertThat(
            ffLgwConverter
                .toExternal(order, waybillMultiSc.get(idx), waybillMultiSc.get(4))
                .getWarehouse()
                .getWarehouseId()
                .getYandexId()
            )
            .isEqualTo(expectedReturnYaId);
    }

    @Test
    @DisplayName("Значение boxCount = null")
    void boxCountIsNull() {
        Order order = createOrder("2-LOinttest-1", place).setItems(List.of(createOrderItem(1).setBoxes(Set.of())));

        softly.assertThat(convertOrder(order))
            .extracting(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order::getItems)
            .usingRecursiveComparison()
            .isEqualTo(List.of(createItem(1, null)));
    }

    @Test
    @DisplayName("Конвертация отправителя-физлица на сегмента дропоффа")
    void convertPhysicalPersonSenderOnDropoff() {
        Order order = createOrder("2-LOinttest-1", place);
        order.setOrderContacts(createPhysicalSenderContact());

        ru.yandex.market.logistic.gateway.common.model.fulfillment.Order lgwOrder = ffLgwConverter.toExternal(
            order.setWaybill(waybillWithDropoff),
            waybillWithDropoff.get(0),
            waybillWithDropoff.get(1)
        );

        softly.assertThat(lgwOrder.getPhysicalPersonSender()).isEqualTo(createLgwPhysicalSender());
        softly.assertThat(lgwOrder.getPersonalPhysicalPersonSender()).isEqualTo(createLgwPersonalPhysicalSender());
    }

    @Test
    @DisplayName("Отправитель-физлицо не конвертируется на сегменте СЦ")
    void ignorePhysicalPersonSenderOnSc() {
        Order order = createOrder("2-LOinttest-1", place);
        order.setOrderContacts(createPhysicalSenderContact());
        softly.assertThat(convertOrder(order).getPhysicalPersonSender()).isNull();
        softly.assertThat(convertOrder(order).getPersonalPhysicalPersonSender()).isNull();
    }

    @Nonnull
    private static Stream<Arguments> returnWarehouseChainArgs() {
        return Stream.of(
            Arguments.of(0, "204"),
            Arguments.of(1, "201"),
            Arguments.of(2, "202"),
            Arguments.of(3, "203")
        );
    }

    @Nonnull
    private ru.yandex.market.logistic.gateway.common.model.fulfillment.Order convertOrder(Order testOrder) {
        return convertOrder(testOrder, waybill);
    }

    @Nonnull
    private ru.yandex.market.logistic.gateway.common.model.fulfillment.Order convertOrder(
        Order testOrder,
        List<WaybillSegment> waybill
    ) {
        return ffLgwConverter.toExternal(
            testOrder.setWaybill(waybill),
            sortingCenterSegment,
            deliverySegment
        );
    }

    @Nonnull
    private static Monetary createMonetary() {
        return new Monetary()
            .setCurrency("RUB")
            .setExchangeRate(BigDecimal.ONE)
            .setValue(BigDecimal.TEN.multiply(BigDecimal.valueOf(1)));
    }

    @Nonnull
    private static List<OrderContact> createPhysicalSenderContact() {
        return List.of(
            new OrderContact()
                .setContactType(ContactType.PHYSICAL_PERSON_SENDER)
                .setContact(
                    new Contact()
                        .setFio(new Fio().setLastName("Пупкин").setFirstName("Василий").setMiddleName("Иванович"))
                        .setPhone("8987653210")
                        .setExtension("12345")
                        .setPersonalFullnameId("sender-fullname-id")
                        .setPersonalPhoneId("sender-phone-id")
                )
        );
    }

    @Nonnull
    private static PhysicalPersonSender createLgwPhysicalSender() {
        return new PhysicalPersonSender(
            new Person("Василий", "Пупкин", "Иванович", null),
            new Phone("8987653210", "12345")
        );
    }

    @Nonnull
    private static PersonalPhysicalPersonSender createLgwPersonalPhysicalSender() {
        return new PersonalPhysicalPersonSender(
            "sender-fullname-id",
            new PersonalPhone("sender-phone-id", "12345")
        );
    }
}
