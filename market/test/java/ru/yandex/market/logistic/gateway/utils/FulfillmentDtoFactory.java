package ru.yandex.market.logistic.gateway.utils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Address;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Car;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignment;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Delivery;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocData;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplate;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplateType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Location;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Outbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Param;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PhysicalPersonSender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Place;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Recipient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterUnit;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterUnitType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Sender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Taxation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransportationRegister;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.WorkTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.entities.restricted.CreateOrderRestrictedData;

public class FulfillmentDtoFactory {

    public static Inbound createInbound() {
        return new Inbound.InboundBuilder(ResourceId.builder().setYandexId("111").build(),
            InboundType.DEFAULT,
            Collections.singletonList(createConsignment()),
            createDateTimeInterval())
            .setWarehouse(createWarehouse())
            .setCourier(createCourier())
            .setComment("Коммент к поставке")
            .build();
    }

    public static Inbound createInvalidInbound() {
        return new Inbound.InboundBuilder(null, null, null, null).build();
    }

    public static Transfer createTransfer() {
        List<TransferItem> transferItems = new ArrayList<>();
        TransferItem transferItem = new TransferItem(createUnitId(), 23);
        transferItem.setInstances(ImmutableList.of(
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
        ));
        transferItems.add(transferItem);

        return new Transfer(ResourceId.builder().setYandexId("111").build(),
            createResourceId(),
            StockType.FIT,
            StockType.SURPLUS,
            transferItems
        );
    }

    public static Outbound createOutbound() {
        return new Outbound.OutboundBuilder(
            createResourceId(),
            StockType.FIT,
            Collections.singletonList(createConsignment()),
            createCourier(),
            createSmallLegalEntity(),
            createDateTimeInterval()
        ).setWarehouse(createWarehouse())
            .setComment("Коммент к изъятию")
            .build();
    }

    public static Outbound createInvalidOutbound() {
        return new Outbound.OutboundBuilder(null, null, null, null, null, null).build();
    }

    public static Order.OrderBuilder createOrderBuilder() {
        return new Order.OrderBuilder(
            createResourceId(),
            createLocation(),
            Collections.singletonList(createItem()),
            BigDecimal.valueOf(1659),
            BigDecimal.valueOf(1908),
            PaymentType.PREPAID,
            createDelivery(),
            DeliveryType.COURIER,
            BigDecimal.valueOf(249),
            createDocData(),
            createServices(),
            createWarehouse(),
            createWarehouse(),
            createRecipient(),
            BigDecimal.valueOf(0),
            BigDecimal.valueOf(1908)
        )
            .setPartnerLogisticPoint(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId("10000010736")
                    .setPartnerId("172")
                    .build()
            )
            .setCargoType(CargoType.UNKNOWN)
            .setComment("Комментарий")
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 3, 0, 0, 0)))
            .setDeliveryInterval(new TimeInterval("03:00:00+03:00/02:59:00+03:00"))
            .setExternalId(createResourceId())
            .setHeight(10)
            .setLength(20)
            .setWidth(10)
            .setWeight(BigDecimal.valueOf(5))
            .setKorobyte(createKorobyte())
            .setLocationFrom(createLocation())
            .setPickupPointCode("pupcode")
            .setSender(createSender())
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 0, 0, 0)))
            .setShipmentDateTime(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 11, 35, 0)))
            .setTariff("Доставка")
            .setPlaces(createPlaces())
            .setWaybill(createParams())
            .setMaxAbsentItemsPricePercent(new BigDecimal("20.5"))
            .setReturnInfo(createReturnInfo())
            .setPhysicalPersonSender(createPhysicalPersonSender());
    }

    /**
     * Данные о заказе.
     */
    public static Order createOrder() {
        return createOrderBuilder()
            .setCargoType(CargoType.UNKNOWN)
            .setComment("Комментарий")
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 3, 0, 0, 0)))
            .setDeliveryInterval(new TimeInterval("03:00:00+03:00/02:59:00+03:00"))
            .setExternalId(createResourceId())
            .setHeight(10)
            .setLength(20)
            .setWidth(10)
            .setWeight(BigDecimal.valueOf(5))
            .setKorobyte(createKorobyte())
            .setLocationFrom(createLocation())
            .setPickupPointCode("pupcode")
            .setSender(createSender())
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 0, 0, 0)))
            .setShipmentDateTime(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 11, 35, 0)))
            .setTariff("Доставка")
            .setWaybill(createParams())
            .build();
    }

    @Nonnull
    private static ReturnInfo createReturnInfo() {
        return new ReturnInfo(
            createPartnerInfo("partnerToId", "partnerTo incorporation"),
            createPartnerInfo("partnerTransporterId", "partnerTransporter incorporation"),
            ReturnType.DROPOFF
        );
    }

    @Nonnull
    private static PartnerInfo createPartnerInfo(String partnerId, String incorporation) {
        return new PartnerInfo(partnerId, incorporation);
    }

    public static Order createInvalidOrder() {
        return new Order.OrderBuilder(null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null).build();
    }

    public static ResourceId createResourceId() {
        return ResourceId.builder().setYandexId("111").setPartnerId("Zakaz").build();
    }

    public static ru.yandex.market.logistic.gateway.common.model.common.ResourceId createCommonResourceId() {
        return ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
            .setYandexId("111")
            .setPartnerId("Zakaz")
            .build();
    }

    public static Partner createPartner() {
        return new Partner(123L);
    }

    public static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2018-04-20T17:00:00+03:00/2018-04-20T17:00:00+03:00");
    }

    public static Consignment createConsignment() {
        return new Consignment.ConsignmentBuilder(ResourceId.builder().setYandexId("111").build(),
            createItem())
            .build();
    }

    public static Item createItem() {
        return new Item.ItemBuilder("Свят! 1шт",
            1,
            new BigDecimal(100),
            CargoType.UNKNOWN,
            List.of()
        )
            .setUnitId(new UnitId(null, 436353L, "a123"))
            .setArticle("a123")
            .setBarcodes(Collections.singletonList(new Barcode(
                "4603934000793",
                "barcode_type",
                BarcodeSource.SUPPLIER)))
            .setCargoTypes(List.of(CargoType.UNKNOWN))
            .setDescription("описание")
            .setUntaxedPrice(new BigDecimal(100))
            .setBoxCapacity(15)
            .setComment("comment N1")
            .setLifeTime(15)
            .setHasLifeTime(true)
            .setRemovableIfAbsent(true)
            .setUnitOperationType(UnitOperationType.CROSSDOCK)
            .setInstances(createItemInstances())
            .build();
    }

    public static List<Map<String, String>> createItemInstances() {
        return List.of(
            Map.of("cis", "123abc"),
            Map.of("cis", "cba321")
        );
    }

    public static Warehouse createWarehouse() {
        return new Warehouse.WarehouseBuilder(
            ResourceId.builder().setYandexId("1").setPartnerId("ff1").build(),
            createLocation(),
            createWorkTimes(),
            "ООО Ромашка")
            .setResourceId(ResourceId.builder().setYandexId("1").setPartnerId("ff1").build())
            .setPhones(Collections.singletonList(new Phone("+7(909)9090909", null)))
            .build();
    }

    public static Location createLocation() {
        return new Location.LocationBuilder("Russia", "Moscow", "The federal city of Moscow").build();
    }

    public static List<WorkTime> createWorkTimes() {
        return Arrays.asList(
            new WorkTime.WorkTimeBuilder()
                .setDay(DayOfWeek.MONDAY)
                .setPeriods(Arrays.asList(
                    new TimeInterval("13:00:00/16:00:00"),
                    new TimeInterval("17:00:00+03:00/18:00:00+03:00")
                )).build(),
            new WorkTime.WorkTimeBuilder()
                .setDay(DayOfWeek.WEDNESDAY)
                .setPeriods(Arrays.asList(
                    new TimeInterval("10:03:48/21:00")
                )).build());
    }

    public static Courier createCourier() {
        return new Courier.CourierBuilder(Collections.singletonList(createPerson()))
            .setCar(new Car.CarBuilder("A123AA99").build())
            .setPhone(new Phone.PhoneBuilder("+7(909)9090909").build())
            .setLegalEntity(createSmallLegalEntity())
            .build();
    }

    public static Person createPerson() {
        return new Person.PersonBuilder("Василий").setSurname("Пупкин").build();
    }

    public static LegalEntity createSmallLegalEntity() {
        return new LegalEntity.LegalEntityBuilder("Рамашка2")
            .setLegalName("ООО Всея Рамашка2")
            .setLegalForm(LegalForm.OOO)
            .setAddress(new Address("Москва", null))
            .build();
    }

    /**
     * Список коробок.
     */
    public static List<Place> createPlaces() {
        return Collections.singletonList(
            new Place.PlaceBuilder(createResourceId())
                .setKorobyte(createKorobyte())
                .setItemPlaces(Collections.singletonList(
                    new ItemPlace.ItemPlaceBuilder(createUnitId(), 1).build()
                ))
                .build());
    }

    /**
     * Вес и габариты объекта.
     */
    public static Korobyte createKorobyte() {
        return new Korobyte.KorobyteBuilder()
            .setWidth(45)
            .setHeight(16)
            .setLength(21)
            .setWeightGross(BigDecimal.valueOf(3.2))
            .setWeightNet(BigDecimal.valueOf(2))
            .setWeightTare(BigDecimal.valueOf(1.2))
            .build();
    }

    public static UnitId createUnitId() {
        return new UnitId.UnitIdBuilder(0L, "75690200345480.Checkouter-test-20")
            .setId("123id").build();
    }

    /**
     * Данные о службе доставки.
     */
    public static Delivery createDelivery() {
        return new Delivery.DeliveryBuilder(
            "maschrout",
            createPhones(),
            "contract #1",
            Collections.singletonList(createDocTemplate()))
            .setDeliveryId(createResourceId())
            .setIntakeTime(createWorkTimes())
            .setPriority(1)
            .build();
    }

    public static DocTemplate createDocTemplate() {
        return new DocTemplate.DocTemplateBuilder("Doc #1", DocTemplateType.DOCUMENT, "Template #1")
            .setVersion(1).build();
    }

    public static Phone createPhone() {
        return new Phone.PhoneBuilder("79099999999").build();
    }

    public static List<Phone> createPhones() {
        return Collections.singletonList(createPhone());
    }

    public static DocData createDocData() {
        return new DocData.DocDataBuilder("test").setVersion(1).build();
    }

    /**
     * Данные об услуге.
     */
    public static List<Service> createServices() {
        return Arrays.asList(
            new Service.ServiceBuilder(ServiceType.SORT)
                .setName("Название услуги для сортировки")
                .setDescription("Описание услуги для сортировки")
                .setIsOptional(false).build(),
            new Service.ServiceBuilder(ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY)
                .setName("Название услуги для брака")
                .setDescription("Описание услуги для брака")
                .setIsOptional(true).build()
        );
    }

    /**
     * Получатель заказа.
     */
    public static Recipient createRecipient() {
        return new Recipient.RecipientBuilder(createPerson(), createPhones())
            .setEmail("test@yandex.ru").build();
    }

    /**
     * Данные о магазине.
     */
    public static Sender createSender() {
        return new Sender.SenderBuilder(createResourceId(), "ООО «Яндекс Маркет»", "test.ru", LegalForm.OOO)
            .setPartnerId("senderPartnerId")
            .setPhones(createPhones())
            .setName("БЕРУ")
            .setEmail("test@test.ru")
            .setContact(createPerson())
            .setOgrn("1167746491395")
            .setInn("2342342342356")
            .setAddress(createLocation())
            .setType("ip")
            .setTaxation(Taxation.OSN).build();
    }

    public static List<Param> createParams() {
        return Arrays.asList(
            new Param.ParamBuilder("key1").setValue("value1").setComment("comment1").build(),
            new Param.ParamBuilder("key2").setValue("value2").setComment("comment2").build()
        );
    }

    public static TransportationRegister createTransportationRegister() {
        return new TransportationRegister(
            createResourceId(),
            RegisterType.YANDEX,
            null,
            List.of(createRegisterUnit(List.of(createRegisterUnit(null))))
        );
    }

    public static RegisterUnit createRegisterUnit(List<RegisterUnit> childRegisterUnits) {
        return new RegisterUnit(
            RegisterUnitType.BOX,
            childRegisterUnits,
            createResourceId(),
            "article",
            1L,
            1
        );
    }

    public static OrderItems.OrderItemsBuilder createOrderItemsBuilder() {
        return new OrderItems.OrderItemsBuilder()
            .setOrderId(createResourceId())
            .setTotal(BigDecimal.valueOf(1100))
            .setAssessedCost(BigDecimal.valueOf(1000))
            .setDeliveryCost(BigDecimal.valueOf(100))
            .setKorobyte(createKorobyte())
            .setItems(List.of(createItem()));
    }

    public static OrderTransferCodes orderTransferCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("123456").build())
            .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("654321").build())
            .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("333333").build())
            .build();
    }

    public static CreateOrderRestrictedData createOrderRestrictedData() {
        return CreateOrderRestrictedData.builder()
            .setTransferCodes(orderTransferCodes())
            .build();
    }

    public static PhysicalPersonSender createPhysicalPersonSender() {
        return new PhysicalPersonSender(
            createPerson(),
            createPhone()
        );
    }
}
