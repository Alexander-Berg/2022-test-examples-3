package ru.yandex.market.logistics.lom.utils.lgw;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Car;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Delivery;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocData;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplate;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplateType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Intake;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Location;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PersonalLocation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PersonalPhone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PersonalRecipient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Place;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Recipient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Register;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnRegister;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Sender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShipmentType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Tax;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TaxType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Taxation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.VatValue;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.WorkTime;

@ParametersAreNonnullByDefault
public class CreateLgwFulfillmentEntitiesUtils {
    private CreateLgwFulfillmentEntitiesUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Order.OrderBuilder updateScOrder() {
        return createOrder(
            createBarcodeResourceIdScExternalId(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            null,
            createScWarehouseResourceId(),
            createScShipmentDateTime()
        );
    }

    @Nonnull
    public static Order.OrderBuilder createFfOrder() {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createPickupPointLocation().build(),
            createShopLocation().build(),
            null,
            createScWarehouseResourceId(),
            createScShipmentDateTime()
        );
    }

    @Nonnull
    public static Order.OrderBuilder createYadoMidScOrder() {
        return createYadoMidScOrder(createDelivery("Название СД не указано", "20"));
    }

    @Nonnull
    public static Order.OrderBuilder createYadoMidScOrder(Delivery delivery) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createShopWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            null,
            delivery,
            createScWarehouseResourceId(),
            createScShipmentDateTime(),
            null
        );
    }

    @Nonnull
    public static Order.OrderBuilder createScOrder() {
        return createScOrderWithSenderPartnerId(null);
    }

    @Nonnull
    public static Order.OrderBuilder createScOrderWithSenderPartnerId(@Nullable String senderPartnerId) {
        return createScOrder(senderPartnerId, createDelivery("Название СД не указано", "20"));
    }

    @Nonnull
    public static Order.OrderBuilder createScOrder(@Nullable String senderPartnerId, Delivery delivery) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            null,
            delivery,
            createScWarehouseResourceId(),
            createScShipmentDateTime(),
            senderPartnerId
        )
            .setReturnInfo(
                new ReturnInfo(
                    new PartnerInfo("48", "sc-credentials-incorporation"),
                    null,
                    ReturnType.WAREHOUSE
                )
            );
    }

    @Nonnull
    public static Order.OrderBuilder createScOrderWithoutPartnerLogisticPoint() {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            null,
            null,
            createScShipmentDateTime()
        );
    }

    @Nonnull
    public static Order.OrderBuilder createScOrder(DeliveryType deliveryType) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            deliveryType,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            null,
            createScWarehouseResourceId(),
            createScShipmentDateTime()
        );
    }

    @Nonnull
    public static Order.OrderBuilder createScOrderWithServices(@Nullable List<Service> services) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createScLocation().build(),
            createShopLocation().build(),
            services,
            null,
            createScShipmentDateTime()
        );
    }

    @Nonnull
    public static Order.OrderBuilder createDropshipOrder(@Nullable String senderPartnerId) {
        return createFfOrder(senderPartnerId, null);
    }

    @Nonnull
    public static Order.OrderBuilder createFfOrder(@Nullable String senderPartnerId) {
        return createFfOrder(senderPartnerId, "1001");
    }

    @Nonnull
    public static Order.OrderBuilder createFfOrder(
        @Nullable String senderPartnerId,
        @Nullable String externalId
    ) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createPickupPointLocation().build(),
            createShopLocation().build(),
            null,
            createDelivery("сортировочный центр твоего парсела", "21"),
            createShopWarehouseResourceId(),
            createDropshipShipmentDateTime(),
            senderPartnerId
        )
            .setExternalId(createResourceId("2-LOinttest-1", externalId).build())
            .setReturnInfo(
                new ReturnInfo(
                    new PartnerInfo("48", "sc-credentials-incorporation"),
                    null,
                    ReturnType.WAREHOUSE
                )
            );
    }

    @Nonnull
    public static ResourceId createResourceId(String yandexId) {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(yandexId)
            .build();
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createBarcodeResourceIdNew() {
        return createResourceId("2-LOinttest-1", null);
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createBarcodeResourceIdScExternalId() {
        return createResourceId("2-LOinttest-1", "sc-external-id");
    }

    @Nonnull
    public static ResourceId createReturnWarehouseResourceId() {
        return createResourceId("1", "return-external-id").build();
    }

    @Nonnull
    public static ResourceId createShopWarehouseResourceId() {
        return createResourceId("4", "shop-external-id").build();
    }

    @Nonnull
    public static ResourceId createScWarehouseResourceId() {
        return createResourceId("3", "sc-external-id").build();
    }

    @Nonnull
    public static Order createReturnWarehouseOrder(@Nullable String senderPartnerId) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createReturnWarehouse().build(),
            createShopWarehouse().build(),
            createReturnFfLocation().build(),
            createShopLocation().build(),
            null,
            createDelivery("Название СД не указано", "20"),
            createReturnWarehouseResourceId(),
            null,
            senderPartnerId
        )
            .setExternalId(CreateLgwFulfillmentEntitiesUtils.createBarcodeResourceIdNew().build())
            .setShipmentDate(new DateTime("2019-06-11T00:00:00+03:00"))
            .setShipmentDateTime(new DateTime("2019-06-11T09:21:00+03:00"))
            .build();
    }

    @Nonnull
    @SuppressWarnings("ParameterNumber")
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        DeliveryType deliveryType,
        Warehouse warehouse,
        Warehouse warehouseFrom,
        Location locationTo,
        Location locationFrom,
        @Nullable List<Service> services,
        @Nullable ResourceId partnerLogisticPoint,
        @Nullable DateTime shipmentDateTime
    ) {
        return createOrder(
            barcode,
            items,
            deliveryType,
            warehouse,
            warehouseFrom,
            locationTo,
            locationFrom,
            services,
            createDelivery("Название СД не указано", "20"),
            partnerLogisticPoint,
            shipmentDateTime,
            null
        );
    }

    @Nonnull
    @SuppressWarnings("ParameterNumber")
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        DeliveryType deliveryType,
        Warehouse warehouse,
        Warehouse warehouseFrom,
        Location locationTo,
        Location locationFrom,
        @Nullable List<Service> services,
        Delivery delivery,
        @Nullable ResourceId partnerLogisticPoint,
        @Nullable DateTime shipmentDateTime,
        @Nullable String senderPartnerId
    ) {
        return new Order.OrderBuilder(
            barcode.build(),
            locationTo,
            items,
            BigDecimal.valueOf(2000),
            BigDecimal.valueOf(1001),
            PaymentType.CARD,
            delivery,
            deliveryType,
            BigDecimal.valueOf(2000),
            createDocData(),
            services,
            warehouse,
            warehouseFrom,
            createRecipient().build(),
            BigDecimal.valueOf(4000),
            BigDecimal.ZERO
        )
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.parse("2019-06-06T14:00:00")))
            .setExternalId(createResourceId("2-LOinttest-1", "ds-external-id").build())
            .setPartnerLogisticPoint(partnerLogisticPoint)
            .setLocationFrom(locationFrom)
            .setSender(createSender(
                "www.sender-url.com",
                "sender-name",
                "1",
                senderPartnerId,
                List.of(new Phone("+74959999999", null)),
                Taxation.OSN
            ))
            .setPlaces(List.of(createPlace(1001, 2).setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1))).build()))
            .setKorobyte(createKorobyte(1))
            .setCargoType(CargoType.UNKNOWN)
            .setShipmentDate(createShipmentDate())
            .setShipmentDateTime(shipmentDateTime)
            .setPickupPointCode("externalId-1")
            .setComment("test-comment")
            .setMaxAbsentItemsPricePercent(new BigDecimal("12"))
            .setPersonalRecipient(
                new PersonalRecipient(
                    "personal-fullname-id",
                    List.of(new PersonalPhone("personal-phone-id", "12345")),
                    "personal-email-id"
                )
            )
            .setPersonalLocationTo(new PersonalLocation("personal-address-id", "personal-gps-id"))
            .setTariff("1");
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createResourceId(String yandexId, @Nullable String partnerId) {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(yandexId)
            .setPartnerId(partnerId);
    }

    @Nonnull
    public static Register createRegister() {
        return createRegister(List.of(createResourceId("1001-LOinttest-1", "test-external-id").build()));
    }

    @Nonnull
    public static Register createRegister(List<ResourceId> orderIds) {
        return new Register(
            createResourceId("1", null).build(),
            orderIds,
            new DateTime("2019-06-11T00:00:00"),
            createSender("credentials-url", "credentials-name", "1", null, null, null),
            createResourceId("1", null).build(),
            ShipmentType.ACCEPTANCE
        );
    }

    @Nonnull
    public static Location.LocationBuilder createScLocation() {
        return new Location.LocationBuilder("Россия", "sc-settlement", "Московская область")
            .setSettlement("sc-settlement")
            .setStreet("sc-street")
            .setHouse("sc-house")
            .setHousing("sc-housing")
            .setBuilding("sc-building")
            .setRoom("sc-apartment")
            .setLat(BigDecimal.valueOf(3))
            .setLng(BigDecimal.valueOf(3))
            .setLocationId(3);
    }

    @Nonnull
    private static Location.LocationBuilder createPickupPointLocation() {
        return new Location.LocationBuilder("Россия", "pickup-settlement", "Московская область")
            .setSettlement("pickup-settlement")
            .setStreet("pickup-street")
            .setHouse("pickup-house")
            .setHousing("pickup-housing")
            .setBuilding("pickup-building")
            .setRoom("pickup-apartment")
            .setLat(BigDecimal.valueOf(5))
            .setLng(BigDecimal.valueOf(5))
            .setLocationId(5);
    }

    @Nonnull
    public static Location.LocationBuilder createShopLocation() {
        return new Location.LocationBuilder("Россия", "shop-settlement", "Московская область")
            .setSettlement("shop-settlement")
            .setStreet("shop-street")
            .setHouse("shop-house")
            .setHousing("shop-housing")
            .setBuilding("shop-building")
            .setRoom("shop-apartment")
            .setLat(BigDecimal.valueOf(4))
            .setLng(BigDecimal.valueOf(4))
            .setLocationId(4);
    }

    @Nonnull
    public static Location.LocationBuilder createReturnFfLocation() {
        return new Location.LocationBuilder("Россия", "return-settlement", "Московская область")
            .setSettlement("return-settlement")
            .setStreet("return-street")
            .setHouse("return-house")
            .setHousing("return-housing")
            .setBuilding("return-building")
            .setRoom("return-apartment")
            .setLat(BigDecimal.valueOf(1))
            .setLng(BigDecimal.valueOf(1))
            .setLocationId(1);
    }

    @Nonnull
    private static Recipient.RecipientBuilder createRecipient() {
        return new Recipient.RecipientBuilder(
            new Person("test-first-name", "test-last-name", "test-middle-name", null),
            List.of(createPhone())
        )
            .setEmail("test-email@test-domain.com");
    }

    @Nonnull
    public static Korobyte createKorobyte(int scale) {
        return new Korobyte.KorobyteBuilder()
            .setHeight(2 * scale)
            .setWidth(3 * scale)
            .setLength(scale)
            .setWeightGross(BigDecimal.valueOf(4L * scale))
            .build();
    }

    @Nonnull
    private static Person createWarehousePerson() {
        return new Person("Иван", "Иванов", "Иванович", null);
    }

    @Nonnull
    private static Person createShopPerson() {
        return new Person("Магазин", "Магазинов", "Магазинович", null);
    }

    @Nonnull
    private static List<Phone> createShopPhones() {
        return List.of(new Phone("+79994444444", "444"));
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createShopWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("4", "shop-external-id").build(),
            createShopLocation().build(),
            createWarehouseSchedule(),
            null
        )
            .setContact(createShopPerson())
            .setPhones(createShopPhones());
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createReturnWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createReturnWarehouseResourceId(),
            createReturnFfLocation().build(),
            createWarehouseSchedule(),
            null
        )
            .setPhones(createReturnWarehousePhones())
            .setContact(createWarehousePerson());
    }

    @Nonnull
    private static List<Phone> createReturnWarehousePhones() {
        return List.of(new Phone("+79991111111", "111"));
    }

    @Nonnull
    private static Item createItem(int id) {
        return createItemBuilder(id, 1, null, createUnitId(id), null).build();
    }

    @Nonnull
    public static Item createItem(int id, @Nullable Integer boxCount) {
        return createItemBuilder(id, boxCount, null, createUnitId(id), null).build();
    }

    @Nonnull
    public static Item.ItemBuilder createItemBuilder(
        int id,
        @Nullable Integer boxCount,
        @Nullable Boolean removableIfAbsent,
        UnitId unitId,
        @Nullable Integer undefinedCount
    ) {
        return new Item.ItemBuilder(
            "item " + id,
            id,
            BigDecimal.valueOf(10L * id),
            CargoType.UNKNOWN,
            List.of(CargoType.UNKNOWN)
        )
            .setArticle("item article " + id)
            .setKorobyte(createKorobyte(id))
            .setUnitId(unitId)
            .setBoxCount(boxCount)
            .setTax(new Tax(TaxType.VAT, VatValue.NO_NDS))
            .setRemovableIfAbsent(removableIfAbsent)
            .setInstances(createInstances(id))
            .setUndefinedCount(undefinedCount)
            .setUnitOperationType(UnitOperationType.FULFILLMENT);
    }

    @Nonnull
    public static List<Map<String, String>> createInstances(int id) {
        return Stream
            .iterate(1, n -> n + 1)
            .limit(id)
            .map(integer -> Map.of("cis", "123abc"))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static UnitId createUnitId(long id) {
        return new UnitId.UnitIdBuilder(id * 100, "item article " + id).build();
    }

    @Nonnull
    public static Place.PlaceBuilder createPlace(int id) {
        return createPlace(id, id, "place external id " + id);
    }

    @Nonnull
    public static Place.PlaceBuilder createPlace(int id, int dimensionsScale) {
        return createPlace(id, dimensionsScale, "place external id " + id);
    }

    @Nonnull
    public static Place.PlaceBuilder createPlaceWithoutExternalId(int id) {
        return createPlace(id, id, null);
    }

    @Nonnull
    public static Place.PlaceBuilder createPlaceWithoutExternalId(int id, int dimensionsScale) {
        return createPlace(id, dimensionsScale, null);
    }

    @Nonnull
    private static Place.PlaceBuilder createPlace(int id, int dimensionsScale, @Nullable String partnerId) {
        return new Place.PlaceBuilder(createResourceId(String.valueOf(id), partnerId).build())
            .setPartnerCodes(List.of(new PartnerCode(String.valueOf(100L + id), partnerId)))
            .setKorobyte(createKorobyte(dimensionsScale))
            .setItemPlaces(List.of());
    }

    @Nonnull
    public static Order createScOrderWithAllServices() {
        return createScOrderWithServices(List.of(
            new Service(ServiceType.REPACK, null, null, false),
            new Service(ServiceType.SORT, null, null, false)
        ))
            .build();
    }

    @Nonnull
    public static Order.OrderBuilder createScOrderWithTariff(@Nullable String tariff) {
        return createScOrderWithServices(null)
            .setTariff(tariff);
    }

    @Nonnull
    public static Delivery createDelivery(String name, String id) {
        return new Delivery.DeliveryBuilder(
            name,
            List.of(new Phone("+79099999999", "")),
            "contract #1",
            createDocTemplates()
        )
            .setPriority(1)
            .setDeliveryId(ResourceId.builder().setYandexId(id).build())
            .build();
    }

    @Nonnull
    private static Sender createSender(
        String url,
        String name,
        String yandexId,
        @Nullable String partnerId,
        @Nullable List<Phone> phones,
        @Nullable Taxation taxation
    ) {
        return new Sender.SenderBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId(yandexId).build(),
            "credentials-incorporation",
            url,
            LegalForm.IP
        )
            .setOgrn("credentials-ogrn")
            .setInn("credentials-inn")
            .setName(name)
            .setPartnerId(partnerId)
            .setPhones(phones)
            .setTaxation(taxation)
            .setEmail("credentials-email@test-domain.com")
            .build();

    }

    @Nonnull
    private static DateTime createShipmentDate() {
        return DateTime.fromLocalDateTime(LocalDateTime.of(LocalDate.of(2019, 6, 11), LocalTime.MIDNIGHT));
    }

    @Nonnull
    private static DateTime createScShipmentDateTime() {
        return DateTime.fromOffsetDateTime(
            OffsetDateTime.of(LocalDateTime.of(2019, 6, 11, 9, 21, 0), ZoneOffset.ofHours(3))
        );
    }

    @Nonnull
    private static DateTime createDropshipShipmentDateTime() {
        return DateTime.fromOffsetDateTime(
            OffsetDateTime.of(LocalDateTime.of(2019, 6, 11, 13, 54, 0), ZoneOffset.ofHours(3))
        );
    }

    @Nonnull
    private static DocData createDocData() {
        return new DocData.DocDataBuilder("test").setVersion(1).build();
    }

    @Nonnull
    private static List<DocTemplate> createDocTemplates() {
        return List.of(
            new DocTemplate("Doc #1", 1, DocTemplateType.DOCUMENT, "Template #1"),
            new DocTemplate("Doc #2", 1, DocTemplateType.LABEL, "Template #2")
        );
    }

    @Nonnull
    private static TimeInterval createTimeInterval() {
        return new TimeInterval("10:00:00/18:00:00");
    }

    @Nonnull
    private static Phone createPhone() {
        return new Phone.PhoneBuilder("+74959999999")
            .setAdditional("12345")
            .build();
    }

    @Nonnull
    public static Intake createLgwIntake() {
        return new Intake(
            ResourceId.builder().setYandexId("1").build(),
            new Warehouse.WarehouseBuilder(
                ResourceId.builder().setYandexId("6").setPartnerId("external-id").build(),
                createShopLocation().build(),
                createLogisticPointSchedule(),
                "ooo magaz"
            )
                .setContact(createWarehousePerson())
                .setInstruction(null)
                .setPhones(List.of(createPhone()))
                .build(),
            createDateTimeInterval(),
            createVolume(),
            createWeight()
        );
    }

    @Nonnull
    public static SelfExport createLgwSelfExport() {
        return new SelfExport(
            ResourceId.builder().setYandexId("1").build(),
            new Warehouse.WarehouseBuilder(
                ResourceId.builder().setYandexId("7").setPartnerId("external-id").build(),
                createScLocation().build(),
                createLogisticPointSchedule(),
                "ooo sc"
            )
                .setContact(createWarehousePerson())
                .setInstruction(null)
                .setPhones(List.of(createPhone()))
                .build(),
            createDateTimeInterval(),
            createCourier(),
            createVolume(),
            createWeight()
        );
    }

    @Nonnull
    public static ReturnRegister createReturnRegister(String externalId, String yandexId) {
        return new ReturnRegister(
            List.of(createResourceId("1001-LOinttest-1", externalId).build()),
            createSender(
                "www.sender-url.com",
                "sender-name",
                yandexId,
                null,
                List.of(new Phone("+74959999999", null)),
                Taxation.OSN
            ),
            createResourceId("1", null).build()
        );
    }

    @Nonnull
    private static List<WorkTime> createLogisticPointSchedule() {
        return List.of(new WorkTime(DayOfWeek.MONDAY, List.of(createTimeInterval())));
    }

    @Nonnull
    private static BigDecimal createVolume() {
        return BigDecimal.valueOf(0.1);
    }

    @Nonnull
    private static BigDecimal createWeight() {
        return BigDecimal.valueOf(4.0);
    }

    @Nonnull
    private static List<WorkTime> createWarehouseSchedule() {
        return List.of(new WorkTime(DayOfWeek.MONDAY, List.of(createTimeInterval())));
    }

    @Nonnull
    private static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2019-06-11T10:00:00/2019-06-11T18:00:00");
    }

    @Nonnull
    private static Courier createCourier() {
        return new Courier.CourierBuilder(
            List.of(
                new Person.PersonBuilder("Иван")
                    .setSurname("Иванов")
                    .setPatronymic("Иванович")
                    .build()
            )
        ).setPhone(createPhone()).build();
    }

    @Nonnull
    public static Intake.IntakeBuilder createIntakeBuilder() {
        return createIntakeBuilder(createWarehouseBuilder("3"));
    }

    @Nonnull
    public static Intake.IntakeBuilder createIntakeBuilder(Warehouse.WarehouseBuilder warehouseBuilder) {
        return new Intake.IntakeBuilder(
            createResourceId("1"),
            warehouseBuilder.build(),
            DateTimeInterval.fromFormattedValue("2019-06-11T12:00:00/2019-06-11T14:00:00")
        )
            .setVolume(BigDecimal.valueOf(0.1))
            .setWeight(BigDecimal.valueOf(0.5));
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createWarehouseBuilder(String id) {
        return new Warehouse.WarehouseBuilder(
            createResourceId(id, "externalId").build(),
            new Location.LocationBuilder("Россия", "Новосибирск", "Регион")
                .setSettlement("Новосибирск")
                .setStreet("Николаева")
                .setHouse("11")
                .setHousing("11")
                .setBuilding("")
                .setRoom("")
                .setZipCode("649220")
                .setLocationId(1)
                .build(),
            List.of(
                new WorkTime.WorkTimeBuilder()
                    .setDay(DayOfWeek.MONDAY)
                    .setPeriods(List.of(
                        TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(18, 0))
                    ))
                    .build()
            ),
            "ООО Рога и копыта"
        )
            .setContact(
                new Person.PersonBuilder("Иван")
                    .setSurname("Иванов")
                    .setPatronymic("Иванович")
                    .build()
            )
            .setPhones(List.of(
                new Phone.PhoneBuilder("+79232435555")
                    .setAdditional("777")
                    .build()
            ));
    }

    @Nonnull
    public static SelfExport createSelfExport() {
        return new SelfExport(
            createResourceId("1"),
            createWarehouseBuilder("4").build(),
            DateTimeInterval.fromFormattedValue("2019-06-11T12:00:00/2019-06-11T14:00:00"),
            new Courier.CourierBuilder(
                List.of(new Person.PersonBuilder("test-first-name-1")
                    .setSurname("test-last-name-1")
                    .build()
                )
            )
                .setCar(new Car.CarBuilder("A001BC23").setDescription("Renault").build())
                .setPhone(new Phone.PhoneBuilder("+79994567890").build())
                .build(),
            BigDecimal.valueOf(0.1),
            BigDecimal.valueOf(0.5)
        );
    }

    @Nonnull
    public static OrderItems createOrderItems() {
        return createOrderItems(BigDecimal.valueOf(4000));
    }

    @Nonnull
    public static OrderItems createOrderItems(BigDecimal total) {
        return new OrderItems.OrderItemsBuilder()
            .setOrderId(createResourceId("barcode-1", "external-id-1").build())
            .setKorobyte(
                new Korobyte.KorobyteBuilder()
                    .setLength(1)
                    .setHeight(2)
                    .setWidth(3)
                    .setWeightGross(BigDecimal.valueOf(4L))
                    .build()
            )
            .setAssessedCost(BigDecimal.valueOf(100))
            .setDeliveryCost(BigDecimal.valueOf(2000))
            .setTotal(total)
            .setItems(List.of(createItem(1, null)))
            .build();
    }
}
