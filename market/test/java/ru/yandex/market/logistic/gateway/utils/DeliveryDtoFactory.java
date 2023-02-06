package ru.yandex.market.logistic.gateway.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.AdditionalServiceCode;
import ru.yandex.market.logistic.gateway.common.model.delivery.AttachedDocsData;
import ru.yandex.market.logistic.gateway.common.model.delivery.Car;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Courier;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.DocData;
import ru.yandex.market.logistic.gateway.common.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.ItemInstances;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderParcelId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Param;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Place;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.RecipientData;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.ShipmentType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Transaction;
import ru.yandex.market.logistic.gateway.common.model.delivery.TransactionType;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderYandexGoData;

public class DeliveryDtoFactory {

    public static Order.OrderBuilder createOrderBuilder() {
        return createOrderBuilder(Collections.singletonList(createItem()));
    }

    public static Order.OrderBuilder createOrderBuilder(List<Item> items) {
        return new Order.OrderBuilder(
            createResourceId(),
            createLocation(),
            createLocation(),
            new Korobyte.KorobyteBuilder()
                .setHeight(1)
                .setWidth(1)
                .setLength(1)
                .setWeightGross(BigDecimal.ONE).build(),
            items,
            "Безлимит",
            BigDecimal.valueOf(2250),
            PaymentMethod.CARD,
            DeliveryType.PICKUP_POINT,
            BigDecimal.ONE,
            createRecipient(),
            BigDecimal.ONE,
            createSender()

        )
            .setWarehouseFrom(createWarehouse())
            .setShipmentDate(new DateTime("2018-04-20T17:00:00+03:00"))
            .setShipmentDateTime(new DateTime("2018-04-20T18:30:00+03:00"))
            .setAmountPrepaid(BigDecimal.valueOf(1000))
            .setCargoType(CargoType.ANIMALS)
            .setCargoCost(BigDecimal.valueOf(1000));
    }

    public static Order createOrder() {
        return createOrderBuilder()
            .setParcelId(ResourceId.builder().setYandexId("111").build())
            .setDocumentData(createDocData())
            .setPlaces(Collections.singletonList(createPlace()))
            .build();
    }

    public static Order createInvalidOrder() {
        return new Order.OrderBuilder(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
            .build();
    }

    private static CreateOrderYandexGoData createValidOrderYandexGoData() {
        return new CreateOrderYandexGoData(
            "test",
            1,
            1,
            createDateTime()
        );
    }

    public static OrderTransferCodes createValidOrderTransferCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("123456").build())
            .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("654321").build())
            .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("333333").build())
            .build();
    }

    public static CreateOrderRestrictedData createOrderRestrictedData() {
        return CreateOrderRestrictedData.builder()
            .setYandexGoData(createValidOrderYandexGoData())
            .setTransferCodes(createValidOrderTransferCodes())
            .build();
    }

    public static CreateOrderRestrictedData createOrderEmptyRestrictedData() {
        return CreateOrderRestrictedData.builder()
            .setYandexGoData(null)
            .setTransferCodes(null)
            .build();
    }

    public static Collection<Triple<CreateOrderRestrictedData, String, String>> createInvalidRestrictedYandexGoDataCollection() {
        return ImmutableList.of(
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "",
                        1,
                        1,
                        createDateTime()
                    )
                ).build(),
                "restrictedData.yandexGoData.routeId",
                "must not be blank"
            ),
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "test",
                        -1,
                        1,
                        createDateTime()
                    )
                ).build(),
                "restrictedData.yandexGoData.totalSegmentCount",
                "must be greater than or equal to 1"
            ),
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "test",
                        null,
                        1,
                        createDateTime()
                    )
                ).build(),
                "restrictedData.yandexGoData.totalSegmentCount",
                "must not be null"
            ),
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "test",
                        1,
                        -1,
                        createDateTime()
                    )
                ).build(),
                "restrictedData.yandexGoData.segmentNumber",
                "must be greater than or equal to 0"
            ),
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "test",
                        1,
                        null,
                        createDateTime()
                    )
                ).build(),
                "restrictedData.yandexGoData.segmentNumber",
                "must not be null"
            ),
            new Triple<>(
                CreateOrderRestrictedData.builder().setYandexGoData(
                    new CreateOrderYandexGoData(
                        "test",
                        1,
                        1,
                        null
                    )
                ).build(),
                "restrictedData.yandexGoData.pickupTime",
                "must not be null"
            )
        );
    }

    public static Partner createPartner() {
        return new Partner(123L);
    }

    public static OrderParcelId createOrderParcelId() {
        return new OrderParcelId(createResourceId(), ResourceId.builder().setYandexId("222").build());
    }

    public static ResourceId createResourceId() {
        return ResourceId.builder().setYandexId("111").build();
    }

    public static ExternalResourceId createExternalResourceId() {
        return new ExternalResourceId("111", "DS12", "123");
    }

    public static Location createLocation() {
        return new Location.LocationBuilder("Russia", "Moscow", "The federal city of Moscow").build();
    }

    private static DocData createDocData() {
        return new DocData.DocDataBuilder("Big doc data").setVersion(100500).build();
    }

    private static Sender createSender() {
        return new Sender.SenderBuilder("ООО \"Java-господа\"", "777")
            .setId(ResourceId.builder().setYandexId("123").build())
            .setUrl("http://localhost/")
            .setLegalForm(LegalForm.OOO)
            .setInn("666")
            .setAddress(createLocation())
            .setTaxation(Taxation.OSN)
            .setPhones(Collections.singletonList(DeliveryDtoFactory.createPhone()))
            .build();
    }

    public static Phone createPhone() {
        return new Phone.PhoneBuilder("8-800-555-35-35").build();
    }

    private static Place createPlace() {
        return new Place.PlaceBuilder(
            ResourceId.builder().build(),
            new Korobyte.KorobyteBuilder().build()
        )
            .build();
    }

    public static Recipient createRecipient() {
        return new Recipient.RecipientBuilder(
            new Person("Афанасий", "Иванов", "Эдуардович"),
            Collections.singletonList(createPhone())
        )
            .setRecipientData(new RecipientData(createResourceId()))
            .setPersonalDataStatus(PersonalDataStatus.GATHERED)
            .build();
    }

    private static Item createItem() {
        return new Item.ItemBuilder("Бутылка", 3, BigDecimal.valueOf(50.0)).build();
    }

    public static List<Map<String, String>> createItemInstances() {
        return List.of(
            Map.of("cis", "123abc"),
            Map.of("cis", "cba321")
        );
    }

    public static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2019-02-14T00:00:00+03:00/2019-02-21T00:00:00+03:00");
    }

    public static WorkTime createWorkTime() {
        return new WorkTime(1, Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00")));
    }

    public static Warehouse createWarehouse() {
        return new Warehouse(
            createResourceId(),
            createLocation(),
            "Для проезда через шлагбаум позвоните охраннику",
            Collections.singletonList(
                new WorkTime(
                    1,
                    Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00"))
                )),
            createPerson(),
            Collections.singletonList(createPhone())
        );
    }

    public static Person createPerson() {
        return new Person("Афинодор", "Великопольский", "Андреевич");
    }

    public static OrderDeliveryDate createOrderDeliveryDate() {
        return new OrderDeliveryDate(
            createResourceId(),
            createDateTime(),
            createTimeInterval(),
            "anyMessage"
        );
    }

    public static OrderDeliveryDate createInvalidOrderDeliveryDate() {
        return new OrderDeliveryDate(
            null,
            createDateTime(),
            createTimeInterval(),
            "anyMessage"
        );
    }

    public static TimeInterval createTimeInterval() {
        return TimeInterval.of(LocalTime.of(12, 0), LocalTime.of(17, 0));
    }

    public static DateTime createDateTime() {
        return DateTime.fromLocalDateTime(LocalDateTime.of(2019, Month.FEBRUARY, 14, 11, 0, 0));
    }

    public static SelfExport createSelfExport() {
        return new SelfExport.SelfExportBuilder()
            .setCourier(createCourier())
            .setSelfExportId(createResourceId())
            .setTime(createDateTimeInterval())
            .setVolume(10.0f)
            .setWarehouse(createWarehouse())
            .setWeight(5.0f)
            .build();
    }

    private static Car createCar() {
        return new Car.CarBuilder("А019МР199").setDescription("Моя красная тачка").build();
    }

    private static Courier createCourier() {
        return new Courier.CourierBuilder(Collections.singletonList(
            new Person.PersonBuilder("Иван", "Доставляев")
                .setPatronymic("Васильевич")
                .build()))
            .setCar(createCar())
            .build();
    }

    public static SelfExport createInvalidSelfExport() {
        // warehouse must not be null
        return new SelfExport.SelfExportBuilder()
            .setCourier(createCourier())
            .setSelfExportId(createResourceId())
            .setTime(createDateTimeInterval())
            .setVolume(10.0f)
            .setWarehouse(null)
            .setWeight(5.0f)
            .build();
    }

    private static ResourceId createRegisterId() {
        return ResourceId.builder().setYandexId("LOdkdkd2").build();
    }

    public static AttachedDocsData createAttachedDocsData() {
        return new AttachedDocsData.AttachedDocsDataBuilder()
            .setOrdersId(List.of(createOrderParcelId()))
            .setSender(createSender())
            .setShipmentDate(createDateTime())
            .setShipmentType(ShipmentType.ACCEPTANCE)
            .setRegisterId(createRegisterId())
            .setWarehouse(createWarehouse())
            .build();
    }

    public static AttachedDocsData createInvalidAttachedDocsData() {
        return new AttachedDocsData.AttachedDocsDataBuilder()
            .setOrdersId(null)
            .setSender(null)
            .setShipmentDate(null)
            .setShipmentType(null)
            .setWarehouse(null)
            .build();
    }

    public static Transaction createTransaction() {
        return new Transaction(
            createResourceId(),
            createDateTime(),
            "test_hash",
            TransactionType.SERVICE,
            Collections.singletonList(new Param("weight", "15", null)),
            new BigDecimal(12345),
            "test_native_name",
            AdditionalServiceCode.INSURANCE
        );
    }

    public static OrderItems.OrderItemsBuilder createOrderItemsBuilder() {
        return new OrderItems.OrderItemsBuilder(DeliveryDtoFactory.createResourceId())
            .setTotal(BigDecimal.valueOf(1100))
            .setAssessedCost(BigDecimal.valueOf(1000))
            .setDeliveryCost(BigDecimal.valueOf(100))
            .setWeight(BigDecimal.valueOf(1))
            .setLength(120)
            .setWidth(70)
            .setHeight(30)
            .setItems(Collections.singletonList(DeliveryDtoFactory.createItem()));
    }

    public static List<ItemInstances> createItemsInstances() {
        return Collections.singletonList(
            new ItemInstances.ItemInstancesBuilder()
                .setUnitId(new UnitId.UnitIdBuilder(48905L, "article").build())
                .setInstances(createItemInstances())
                .build()
        );
    }
}
