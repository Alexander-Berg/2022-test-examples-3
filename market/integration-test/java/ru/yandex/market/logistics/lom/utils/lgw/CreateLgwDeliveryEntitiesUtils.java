package ru.yandex.market.logistics.lom.utils.lgw;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.delivery.AttachedDocsData;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderParcelId;
import ru.yandex.market.logistic.gateway.common.model.delivery.PartnerCode;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalLocation;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalPhone;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalRecipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Place;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.Register;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.delivery.ShipmentType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Tax;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaxType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.VatValue;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;

public final class CreateLgwDeliveryEntitiesUtils {

    private CreateLgwDeliveryEntitiesUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Order.OrderBuilder createOrder() {
        return createOrder(createBarcodeResourceIdNew(), "1");
    }

    @Nonnull
    public static Order.OrderBuilder createOrderWithTariff(String tariff) {
        return createOrder(createBarcodeResourceIdNew(), tariff);
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(PaymentMethod paymentMethod) {
        return createOrder(createBarcodeResourceIdNew(), paymentMethod, DeliveryType.PICKUP_POINT);
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(DeliveryType deliveryType) {
        return createOrder(createBarcodeResourceIdNew(), PaymentMethod.CARD, deliveryType);
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(PaymentMethod paymentMethod, DeliveryType deliveryType) {
        return createOrder(createBarcodeResourceIdNew(), paymentMethod, deliveryType);
    }

    @Nonnull
    public static Order.OrderBuilder updateOrder() {
        return createOrder(createBarcodeResourceIdDsExternalId(), "1");
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(DeliveryType deliveryType, Location locationTo) {
        return createOrder(createBarcodeResourceIdNew(), deliveryType, locationTo);
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        DeliveryType deliveryType,
        Location locationTo
    ) {
        return createOrder(barcode, List.of(createItem(1)), deliveryType, locationTo, "1");
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        DeliveryType deliveryType,
        List<Item> items
    ) {
        return createOrder(barcode, items, deliveryType, createPickupPointLocationTo().build(), "1");
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(ResourceId.ResourceIdBuilder barcode, String tariff) {
        return createOrder(
            barcode,
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createPickupPointLocationTo().build(),
            tariff
        );
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        PaymentMethod paymentMethod,
        DeliveryType deliveryType
    ) {
        return createOrder(
            barcode,
            List.of(createItem(1)),
            deliveryType,
            createPickupPointLocationTo().build(),
            paymentMethod,
            "1"
        );
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(PaymentMethod paymentMethod, Long uid) {
        return createOrder(
            createBarcodeResourceIdNew(),
            List.of(createItem(1)),
            DeliveryType.PICKUP_POINT,
            createPickupPointLocationTo().build(),
            paymentMethod,
            "1",
            uid
        );
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(ResourceId.ResourceIdBuilder barcode, List<Item> items) {
        return createOrder(barcode, items, DeliveryType.PICKUP_POINT, createPickupPointLocationTo().build(), "1");
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        PaymentMethod paymentMethod,
        String tariff
    ) {
        return createOrder(
            barcode,
            items,
            DeliveryType.PICKUP_POINT,
            createPickupPointLocationTo().build(),
            paymentMethod,
            tariff
        );
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        DeliveryType deliveryType,
        Location locationTo,
        String tariff
    ) {
        return createOrder(barcode, items, deliveryType, locationTo, PaymentMethod.CARD, tariff);
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        DeliveryType deliveryType,
        Location locationTo,
        PaymentMethod paymentMethod,
        String tariff,
        @Nullable Long uid
    ) {
        return new Order.OrderBuilder(
            barcode.build(),
            locationTo,
            createScLocation().build(),
            createKorobyte(1),
            items,
            tariff,
            BigDecimal.valueOf(1001),
            paymentMethod,
            deliveryType,
            BigDecimal.valueOf(2000),
            createRecipient(uid).build(),
            BigDecimal.valueOf(4000), // доставка 2000р + итемы 200 * 10 = 4000р
            createSender().build()
        )
            .setWarehouseFrom(createScWarehouse().build())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setAmountPrepaid(BigDecimal.ZERO)
            .setShipmentDate(new DateTime("2019-06-11T00:00:00+03:00"))
            .setPickupPointCode("externalId-1")
            .setPickupPointId(createResourceId(1, "externalId-1"))
            .setDeliveryDate(new DateTime("2019-06-06T00:00:00+03:00"))
            .setDeliveryInterval(new TimeInterval("14:00:00/17:00:00"))
            .setComment("test-comment")
            .setServices(List.of(
                new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D).build(),
                new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(0D).build()
            ))
            .setPlaces(List.of(
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1)))
                    .build()
            ))
            .setPersonalRecipient(new PersonalRecipient(
                    "personal-fullname-id",
                    List.of(new PersonalPhone("personal-phone-id", "12345")),
                    "personal-email-id",
                    null,
                    null,
                    null
            ))
            .setPersonalLocationTo(new PersonalLocation("personal-address-id", "personal-gps-id"))
            .setWarehouse(createReturnWarehouse().build());
    }

    @Nonnull
    public static Order.OrderBuilder createOrder(
        ResourceId.ResourceIdBuilder barcode,
        List<Item> items,
        DeliveryType deliveryType,
        Location locationTo,
        PaymentMethod paymentMethod,
        String tariff
    ) {
        return createOrder(barcode, items, deliveryType, locationTo, paymentMethod, tariff, null);
    }

    @Nonnull
    public static Order.OrderBuilder createDsForTwoSegmentWaybillOrder() {
        return new Order.OrderBuilder(
            createBarcodeResourceIdNew().build(),
            createPickupPointLocationTo().build(),
            createScLocation().build(),
            createKorobyte(1),
            List.of(createItem(1)),
            "1",
            BigDecimal.valueOf(1001),
            PaymentMethod.CARD,
            DeliveryType.PICKUP_POINT,
            BigDecimal.valueOf(2000),
            createRecipient().build(),
            BigDecimal.valueOf(4000),
            createSender().build()
        )
            .setWarehouseFrom(createScWarehouse().build())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setAmountPrepaid(BigDecimal.ZERO)
            .setPickupPointCode("externalId-1")
            .setPickupPointId(createResourceId(1, "externalId-1"))
            .setDeliveryDate(new DateTime("2019-06-06T00:00:00"))
            .setDeliveryInterval(new TimeInterval("14:00:00/17:00:00"))
            .setShipmentDate(new DateTime("2019-06-11T00:00:00"))
            .setShipmentPointCode("externalId-1")
            .setComment("test-comment")
            .setServices(List.of(
                new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D).build(),
                new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(0D).build()
            ))
            .setPlaces(List.of(
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1)))
                    .build()
            ))
            .setWarehouse(createScReturnWarehouse().build());
    }

    @Nonnull
    public static Order.OrderBuilder createDsOrderWithAllServices() {
        return createDsOrder(List.of(
            new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.COMPLECT).setCost(1D).build(),
            new Service.ServiceBuilder(false)
                .setCode(ServiceType.DELIVERY)
                .setCost(1D)
                .setTaxes(List.of(new Tax(TaxType.VAT, VatValue.TWENTY)))
                .build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.INSURANCE).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.OTHER).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.PACK).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.RETURN).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.RETURN_SORT).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.STORAGE).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.TRYING).setCost(1D).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.WAIT_20).setCost(1D).build()
        ));
    }

    @Nonnull
    public static Order.OrderBuilder createDsOrder(List<Service> services) {
        return new Order.OrderBuilder(
            createBarcodeResourceIdNew().build(),
            createPickupPointLocationTo().build(),
            createScLocation().build(),
            createKorobyte(1),
            List.of(createItem(1)),
            "1",
            BigDecimal.valueOf(1001),
            PaymentMethod.CARD,
            DeliveryType.PICKUP_POINT,
            BigDecimal.valueOf(2000),
            createRecipient().build(),
            BigDecimal.valueOf(4000),
            createSender().build()
        )
            .setWarehouseFrom(createScWarehouse().build())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setShipmentDate(new DateTime("2019-06-11T00:00:00"))
            .setShipmentPointCode("externalId-1")
            .setAmountPrepaid(BigDecimal.ZERO)
            .setPickupPointCode("externalId-1")
            .setPickupPointId(createResourceId(1, "externalId-1"))
            .setDeliveryDate(new DateTime("2019-06-06T00:00:00"))
            .setDeliveryInterval(new TimeInterval("14:00:00/17:00:00"))
            .setComment("test-comment")
            .setServices(services)
            .setPlaces(List.of(
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1)))
                    .build()
            ))
            .setWarehouse(createScReturnWarehouse().build());
    }

    @Nonnull
    public static Order.OrderBuilder createOrderDeliveryTax() {
        return createOrder().setServices(List.of(
            new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D)
                .setTaxes(List.of(new Tax(TaxType.VAT, VatValue.TWENTY))).build(),
            new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(0D).build()
        ));
    }

    @Nonnull
    public static Order.OrderBuilder createOrderFullyPrepaid() {
        return new Order.OrderBuilder(
            createBarcodeResourceIdNew().build(),
            createPickupPointLocationTo().build(),
            createScLocation().build(),
            createKorobyte(1),
            List.of(createItem(1)),
            "1",
            BigDecimal.valueOf(100),
            PaymentMethod.CARD,
            DeliveryType.PICKUP_POINT,
            BigDecimal.ZERO,
            createRecipient().build(),
            BigDecimal.ZERO,
            createSender().build()
        )
            .setWarehouseFrom(createScWarehouse().build())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setShipmentDate(new DateTime("2019-06-11T00:00:00"))
            .setAmountPrepaid(BigDecimal.valueOf(2000))
            .setPickupPointCode("externalId-1")
            .setPickupPointId(createResourceId(1, "externalId-1"))
            .setDeliveryDate(new DateTime("2019-06-06T00:00:00"))
            .setDeliveryInterval(new TimeInterval("14:00:00/17:00:00"))
            .setComment("test-comment")
            .setServices(List.of(
                new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D).build(),
                new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(0D).build()
            ))
            .setPlaces(List.of(
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1)))
                    .build()
            ))
            .setWarehouse(createReturnWarehouse().build())
            .setAssessedCost(BigDecimal.valueOf(1001));
    }

    @Nonnull
    public static Order.OrderBuilder createOrderGoPlatform() {
        return new Order.OrderBuilder(
            createBarcodeResourceIdNew().build(),
            createCourierLocationTo().build(),
            createPickupPointLocationTo().build(),
            createKorobyte(1),
            List.of(createItem(1)),
            "1",
            BigDecimal.valueOf(100),
            PaymentMethod.CARD,
            DeliveryType.COURIER,
            BigDecimal.ZERO,
            createRecipient().build(),
            BigDecimal.ZERO,
            createSender().build()
        )
            .setWarehouseFrom(createPickupPointWarehouse().build())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setShipmentDate(new DateTime("2019-06-11T00:00:00+03:00"))
            .setAmountPrepaid(BigDecimal.valueOf(2000))
            .setPickupPointCode("externalId-1")
            .setPickupPointId(createResourceId(1, "externalId-1"))
            .setDeliveryDate(new DateTime("2019-06-06T00:00:00+03:00"))
            .setDeliveryInterval(new TimeInterval("14:00:00/17:00:00"))
            .setComment("test-comment")
            .setServices(List.of(
                new Service.ServiceBuilder(false).setCode(ServiceType.CASH_SERVICE).setCost(1D).build(),
                new Service.ServiceBuilder(false).setCode(ServiceType.CHECK).setCost(0D).build()
            ))
            .setPlaces(List.of(
                createPlace(1001, 2)
                    .setItemPlaces(List.of(new ItemPlace(createUnitId(1), 1)))
                    .build()
            ))
            .setWarehouse(createReturnWarehouse().build())
            .setAssessedCost(BigDecimal.valueOf(1001));
    }

    @Nonnull
    public static Register createRegisterDs() {
        return createRegisterDs(createSender().build());
    }

    @Nonnull
    public static Register createRegisterDs(Sender sender) {
        return createRegisterDs(
            sender,
            List.of(createResourceId("1001-LOinttest-1", "test-external-id").build())
        );
    }

    @Nonnull
    public static Register createRegisterDs(Sender sender, List<ResourceId> ordersId) {
        return new Register(
            createIdResourceId().build(),
            ordersId,
            new DateTime("2019-06-11T00:00:00"),
            sender,
            createIdResourceId().build(),
            ShipmentType.ACCEPTANCE
        );
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createResourceId(String yandexId, String partnerId) {
        return new ResourceId.ResourceIdBuilder().setYandexId(yandexId).setPartnerId(partnerId);
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("2", null).build(),
            createWarehouseLocationFrom().build(),
            List.of(
                new WorkTime(
                    1,
                    List.of(new TimeInterval("10:00:00+03:00/18:00:00+03:00"))
                )
            )
        )
            .setContact(createWarehousePerson())
            .setPhones(List.of(createWarehousePhone()));
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createScWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("3", "sc-external-id").build(),
            createScLocation().build(),
            List.of(
                new WorkTime(
                    1,
                    List.of(new TimeInterval("10:00:00+03:00/18:00:00+03:00"))
                )
            )
        )
            .setContact(createWarehousePerson())
            .setPhones(List.of(createScWarehousePhone()));
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createWarehouseSelfExport() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("2", "externalId").build(),
            createWarehouseLocationFrom().build(),
            List.of(
                new WorkTime(
                    1,
                    List.of(new TimeInterval("10:00:00+03:00/18:00:00+03:00"))
                )
            )
        )
            .setContact(createWarehousePerson())
            .setPhones(List.of(createWarehousePhone()));
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createScReturnWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("1", "return-external-id").build(),
            createReturnFfLocation().build(),
            List.of(
                new WorkTime(
                    1,
                    List.of(new TimeInterval("10:00:00+03:00/18:00:00+03:00"))
                )
            )
        )
            .setContact(new Person.PersonBuilder("Иван", "Иванов").setPatronymic("Иванович").build())
            .setPhones(List.of(createScReturnWarehousePhone()));
    }

    @Nonnull
    private static Phone createScReturnWarehousePhone() {
        return new Phone("+79991111111", "111");
    }

    @Nonnull
    private static Phone createScWarehousePhone() {
        return new Phone("+79993333333", "333");
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createReturnWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("1", "return-external-id").build(),
            createReturnFfLocation().build(),
            List.of(new WorkTime(1, List.of(new TimeInterval("10:00:00/18:00:00"))))
        )
            .setPhones(List.of(createReturnWarehousePhone()))
            .setContact(createWarehousePerson());
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createExpressReturnWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("1", "express-return-external-id").build(),
            createExpressReturnFfLocation().build(),
            List.of(new WorkTime(1, List.of(new TimeInterval("10:00:00/18:00:00"))))
        )
            .setPhones(List.of(createReturnWarehousePhone()))
            .setContact(createWarehousePerson());
    }

    @Nonnull
    public static Warehouse.WarehouseBuilder createPickupPointWarehouse() {
        return new Warehouse.WarehouseBuilder(
            createResourceId("null", "null").build(),
            createPickupPointLocationTo().build(),
            null
        );
    }

    @Nonnull
    public static OrderParcelId createOrderParcelId() {
        return new OrderParcelId(createBarcodeResourceId().build(), null);
    }

    @Nonnull
    public static AttachedDocsData.AttachedDocsDataBuilder createAttachedDocsData() {
        return new AttachedDocsData.AttachedDocsDataBuilder()
            .setOrdersId(List.of(createOrderParcelId()))
            .setRegisterId(createIdResourceId().build())
            .setShipmentDate(new DateTime("2019-06-11T00:00:00"))
            .setShipmentType(ShipmentType.ACCEPTANCE)
            .setSender(createSender().build())
            .setWarehouse(createWarehouse().build());
    }

    @Nonnull
    private static ResourceId.ResourceIdBuilder createIdResourceId() {
        return createResourceId("1", null);
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createBarcodeResourceIdNew() {
        return createResourceId("2-LOinttest-1", null);
    }

    @Nonnull
    public static ResourceId.ResourceIdBuilder createBarcodeResourceIdDsExternalId() {
        return createResourceId("2-LOinttest-1", "ds-external-id");
    }

    @Nonnull
    private static ResourceId.ResourceIdBuilder createBarcodeResourceId() {
        return createResourceId("2-LOinttest-1", "test-external-id");
    }

    @Nonnull
    private static Location.LocationBuilder createPickupPointLocationTo() {
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
    public static Location.LocationBuilder createCourierLocationTo() {
        return new Location.LocationBuilder("Россия", "recipient-settlement", "Московская область")
            .setSettlement("recipient-settlement")
            .setStreet("recipient-street")
            .setHouse("recipient-house")
            .setHousing("recipient-housing")
            .setBuilding("recipient-building")
            .setRoom("recipient-apartment")
            .setLat(BigDecimal.valueOf(6))
            .setLng(BigDecimal.valueOf(7))
            .setLocationId(6);
    }

    @Nonnull
    private static Location.LocationBuilder createScLocation() {
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
    private static Location.LocationBuilder createReturnFfLocation() {
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
    private static Location.LocationBuilder createExpressReturnFfLocation() {
        return new Location.LocationBuilder("Россия", "express-return-settlement", "Московская область")
            .setSettlement("express-return-settlement")
            .setStreet("express-return-street")
            .setHouse("express-return-house")
            .setHousing("express-return-housing")
            .setBuilding("express-return-building")
            .setRoom("express-return-apartment")
            .setLat(BigDecimal.valueOf(1))
            .setLng(BigDecimal.valueOf(1))
            .setLocationId(1);
    }

    @Nonnull
    public static Sender.SenderBuilder createSender() {
        return new Sender.SenderBuilder("credentials-incorporation", "credentials-ogrn")
            .setId(new ResourceId.ResourceIdBuilder().setYandexId("1").build())
            .setUrl("www.sender-url.com")
            .setInn("credentials-inn")
            .setLegalForm(LegalForm.IP)
            .setName("sender-name")
            .setTaxation(Taxation.OSN)
            .setPhones(List.of(new Phone("+74959999999", null)))
            .setEmail("credentials-email@test-domain.com");
    }

    @Nonnull
    private static Phone createPhone() {
        return new Phone.PhoneBuilder("+74959999999")
            .setAdditional("12345")
            .build();
    }

    @Nonnull
    private static Recipient.RecipientBuilder createRecipient() {
        return createRecipient(null);
    }

    @Nonnull
    private static Recipient.RecipientBuilder createRecipient(@Nullable Long uid) {
        return new Recipient.RecipientBuilder(
            new Person("test-first-name", "test-last-name", "test-middle-name"),
            List.of(createPhone())
        )
            .setEmail("test-email@test-domain.com")
            .setUid(uid);
    }

    @Nonnull
    public static Korobyte createKorobyte(int scale) {
        return new Korobyte.KorobyteBuilder()
            .setHeight(2 * scale)
            .setWidth(3 * scale)
            .setLength(scale)
            .setWeightGross(BigDecimal.valueOf(4 * scale))
            .build();
    }

    @Nonnull
    public static Item createItem(int id) {
        return createItemBuilder(id).build();
    }

    @Nonnull
    public static Item createItem(int id, String name) {
        return createItemBuilder(id, name).build();
    }

    @Nonnull
    public static Item.ItemBuilder createItemBuilder(int id) {
        return createItemBuilder(id, "item " + id);
    }

    @Nonnull
    public static Item.ItemBuilder createItemBuilder(int id, String name) {
        return new Item.ItemBuilder(name, id, BigDecimal.valueOf(10L * id))
            .setTaxes(List.of(new Tax(TaxType.VAT, VatValue.NO_NDS)))
            .setArticle("item article " + id)
            .setKorobyte(createKorobyte(id))
            .setInstances(createInstances(id))
            .setUnitId(createUnitId(id))
            .setCategoryName("Телефоны")
            .setCargoTypes(List.of(CargoType.TECH_AND_ELECTRONICS))
            .setSupplier(
                Supplier.builder()
                    .setName("Имя поставщика")
                    .setPhone(new Phone.PhoneBuilder("+79876543210").build())
                    .setInn("1231231234")
                    .build()
            );
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
    public static UnitId createUnitId(int id) {
        return new UnitId.UnitIdBuilder(100L * id, "item article " + id).build();
    }

    @Nonnull
    public static Place.PlaceBuilder createPlace(int id) {
        return createPlace(id, id);
    }

    @Nonnull
    public static Place.PlaceBuilder createPlace(int id, int dimensionsScale) {
        return new Place.PlaceBuilder(
            createResourceId(id, "place external id " + id),
            createKorobyte(dimensionsScale)
        )
            .setPartnerCodes(List.of(new PartnerCode(String.valueOf(100 + id), "place external id " + id)))
            .setItemPlaces(List.of());
    }

    @Nonnull
    public static Place.PlaceBuilder createPlaceWithoutExternalId(int id) {
        return new Place.PlaceBuilder(
            createResourceId(id, null),
            createKorobyte(id)
        )
            .setItemPlaces(List.of());
    }

    @Nonnull
    public static ResourceId createResourceId(int id, String partnerId) {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(String.valueOf(id))
            .setPartnerId(partnerId)
            .build();
    }

    @Nonnull
    public static OrderItems createOrderItem(int assessedCost) {
        return createOrderItemBuilder(assessedCost, BigDecimal.valueOf(4000)).build();
    }

    @Nonnull
    public static OrderItems.OrderItemsBuilder createOrderItemBuilder(int assessedCost, BigDecimal total) {
        return new OrderItems.OrderItemsBuilder(
            createResourceId("barcode-1", "external-id-1").build()
        )
            .setAssessedCost(BigDecimal.valueOf(assessedCost))
            .setDeliveryCost(BigDecimal.valueOf(2000))
            .setTotal(total)
            .setLength(1)
            .setHeight(2)
            .setWidth(3)
            .setWeight(BigDecimal.valueOf(4L))
            .setItems(List.of(createItem(1)));
    }

    @Nonnull
    public static OrderDeliveryDate createOrderDeliveryDate(
        LocalDateTime localDateTime,
        LocalTime from,
        LocalTime to
    ) {
        return new OrderDeliveryDate(
            createResourceId("1", "1").build(),
            DateTime.fromLocalDateTime(localDateTime),
            TimeInterval.of(from, to),
            ""
        );
    }

    @Nonnull
    private static Location.LocationBuilder createWarehouseLocationFrom() {
        return new Location.LocationBuilder("Россия", "Новосибирск", "Регион")
            .setLocationId(1)
            .setStreet("Николаева")
            .setHouse("11")
            .setHousing("11")
            .setBuilding("")
            .setRoom("")
            .setZipCode("649220")
            .setSubRegion("Округ");
    }

    @Nonnull
    public static Location.LocationBuilder createRecipientLocation() {
        return new Location.LocationBuilder("test-country", "recipient-locality", "recipient-region")
            .setFederalDistrict("test-federal-district")
            .setSubRegion("recipient-sub-region")
            .setSettlement("recipient-settlement")
            .setStreet("recipient-street")
            .setHouse("recipient-house")
            .setBuilding("recipient-building")
            .setHousing("recipient-housing")
            .setRoom("recipient-room")
            .setZipCode("recipient-zip-code")
            .setPorch("recipient-porch")
            .setFloor(1)
            .setMetro("recipient-metro")
            .setLat(new BigDecimal("55.018803"))
            .setLng(new BigDecimal("82.933952"))
            .setLocationId(10000)
            .setIntercom("recipient-intercom");
    }

    @Nonnull
    public static CreateOrderRestrictedData createDsRestrictedData() {
        return CreateOrderRestrictedData.builder().setTransferCodes(createTransferCodes()).build();
    }

    @Nonnull
    public static ru.yandex.market.logistic.gateway.common.model.fulfillment.request.entities.restricted
        .CreateOrderRestrictedData createFfRestrictedData() {

        return ru.yandex.market.logistic.gateway.common.model.fulfillment.request.entities.restricted
            .CreateOrderRestrictedData.builder()
            .setTransferCodes(createTransferCodes())
            .build();
    }

    @Nonnull
    private static OrderTransferCodes createTransferCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("12345").build())
            .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("54321").build())
            .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("66666").build())
            .build();
    }

    @Nonnull
    private static Person createWarehousePerson() {
        return new Person("Иван", "Иванов", "Иванович");
    }

    @Nonnull
    private static Phone createWarehousePhone() {
        return new Phone("+79232435555", "777");
    }

    @Nonnull
    private static Phone createReturnWarehousePhone() {
        return new Phone("+79991111111", "111");
    }

}

