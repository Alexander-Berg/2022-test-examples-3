package steps.orderSteps;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Delivery;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocData;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplate;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocTemplateType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Location;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Param;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Place;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Recipient;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Sender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Taxation;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.WorkTime;

public class OrderLgwFulfillmentSteps {

    private OrderLgwFulfillmentSteps() {
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
            .setTariff("Доставка")
            .setWaybill(createParams())
            .setPlaces(createPlaces());
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
            .setTariff("Доставка")
            .setWaybill(createParams())
            .build();
    }

    public static ResourceId createResourceId() {
        return new ResourceId.ResourceIdBuilder().setYandexId("111").setPartnerId("Zakaz").build();
    }

    public static Partner createPartner() {
        return new Partner(123L);
    }

    public static Item createItem() {
        return new Item.ItemBuilder(
            "Свят! 1шт",
            1,
            new BigDecimal(100),
            CargoType.UNKNOWN,
            Collections.singletonList(CargoType.UNKNOWN)
        )
            .setUnitId(new UnitId(null, 436353L, "a123"))
            .setArticle("a123")
            .setBarcodes(Collections.singletonList(new Barcode(
                "4603934000793",
                "barcode_type",
                BarcodeSource.SUPPLIER
            )))
            .setDescription("описание")
            .setUntaxedPrice(new BigDecimal(100))
            .setBoxCapacity(15)
            .setComment("comment N1")
            .setLifeTime(15)
            .setHasLifeTime(true)
            .build();
    }

    public static Warehouse createWarehouse() {
        return new Warehouse.WarehouseBuilder(
            ResourceId.builder().setYandexId("1").setPartnerId("ff1").build(),
            createLocation(),
            createWorkTimes(),
            "ООО Ромашка"
        )
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
                .setPeriods(Collections.singletonList(
                    new TimeInterval("10:03:48/21:00")
                )).build()
        );
    }

    public static Person createPerson() {
        return new Person.PersonBuilder("Василий").setSurname("Пупкин").build();
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
            Collections.singletonList(createDocTemplate())
        )
            .setDeliveryId(createResourceId())
            .setIntakeTime(createWorkTimes())
            .setPriority(1)
            .build();
    }

    public static DocTemplate createDocTemplate() {
        return new DocTemplate.DocTemplateBuilder("Doc #1", DocTemplateType.DOCUMENT, "Template #1")
            .setVersion(1).build();
    }

    public static List<Phone> createPhones() {
        return Collections.singletonList(new Phone.PhoneBuilder("79099999999").build());
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
}
