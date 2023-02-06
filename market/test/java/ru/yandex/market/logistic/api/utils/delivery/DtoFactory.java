package ru.yandex.market.logistic.api.utils.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.yandex.market.logistic.api.model.delivery.Car;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.logistic.api.model.delivery.CargoTypes;
import ru.yandex.market.logistic.api.model.delivery.Courier;
import ru.yandex.market.logistic.api.model.delivery.CustomsTranslation;
import ru.yandex.market.logistic.api.model.delivery.DeliveryType;
import ru.yandex.market.logistic.api.model.delivery.Item;
import ru.yandex.market.logistic.api.model.delivery.ItemPlace;
import ru.yandex.market.logistic.api.model.delivery.Korobyte;
import ru.yandex.market.logistic.api.model.delivery.LegalForm;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.Order;
import ru.yandex.market.logistic.api.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.api.model.delivery.Person;
import ru.yandex.market.logistic.api.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.api.model.delivery.PersonalLocation;
import ru.yandex.market.logistic.api.model.delivery.PersonalPhone;
import ru.yandex.market.logistic.api.model.delivery.PersonalRecipient;
import ru.yandex.market.logistic.api.model.delivery.Phone;
import ru.yandex.market.logistic.api.model.delivery.Place;
import ru.yandex.market.logistic.api.model.delivery.Recipient;
import ru.yandex.market.logistic.api.model.delivery.RecipientData;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.model.delivery.Service;
import ru.yandex.market.logistic.api.model.delivery.Supplier;
import ru.yandex.market.logistic.api.model.delivery.Tax;
import ru.yandex.market.logistic.api.model.delivery.TaxType;
import ru.yandex.market.logistic.api.model.delivery.Taxation;
import ru.yandex.market.logistic.api.model.delivery.TransitData;
import ru.yandex.market.logistic.api.model.delivery.UnitId;
import ru.yandex.market.logistic.api.model.delivery.VatValue;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.model.delivery.WorkTime;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderDeliveryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.CreateOrderYandexGoData;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;
import ru.yandex.market.logistic.api.model.delivery.response.entities.restricted.GetOrderRestrictedData;
import ru.yandex.market.logistic.api.model.delivery.response.entities.restricted.GetOrderYandexGoData;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;

public class DtoFactory {

    private DtoFactory() {
        throw new UnsupportedOperationException();
    }

    public static ResourceId createOrderId() {
        return new ResourceId.ResourceIdBuilder().setYandexId("12345").build();
    }

    private static ResourceId createResourceId() {
        return createResourceId("2384813", null);
    }

    public static ResourceId createResourceId(String yandexId, String deliveryId) {
        return new ResourceId.ResourceIdBuilder().setYandexId(yandexId).setDeliveryId(deliveryId).build();
    }

    private static Location createLocationFrom() {
        return createLocationFrom("Москва и Московская область");
    }

    public static Location createLocationFrom(String region) {
        return new Location.LocationBuilder("Россия", region, "Котельники")
            .setFederalDistrict("Центральный федеральный округ")
            .setStreet("Яничкин проезд")
            .setHouse("7")
            .setLocationId(213L)
            .build();
    }

    private static Location createLocationTo() {
        return new Location.LocationBuilder("Россия", "Свердловская область", "Екатеринбург")
            .setFederalDistrict("Уральский федеральный округ")
            .setSubRegion("Муниципальное образование Екатеринбург")
            .setStreet("ул. Техническая")
            .setHouse("16")
            .setLocationId(54L)
            .build();
    }

    private static PersonalLocation createPersonalLocationTo() {
        return new PersonalLocation.PersonalLocationBuilder("location_to_address_id")
            .setPersonalGpsId("location_to_gps_id")
            .build();
    }

    public static List<Map<String, String>> createInstances() {
        return Arrays.asList(
            new HashMap<String, String>() {{
                put("CIS", "123345-abcDEF");
            }},
            new HashMap<String, String>() {{
                put("CIS", "abcDEF-123345");
            }}
        );
    }

    public static List<Map<String, String>> createBase64EncodedInstances() {
        return Arrays.asList(
            new HashMap<String, String>() {{
                put("CIS", "MTIzMzQ1LWFiY0RFRg==");
                put("CIS_FULL", "MTIzMzQ1LWFiY0RFRi1YWVow");
            }},
            new HashMap<String, String>() {{
                put("CIS", "YWJjREVGLTEyMzM0NQ==");
                put("CIS_FULL", "YWJjREVGLTEyMzM0NS0wWllY");
            }}
        );
    }

    public static List<Item> createItems() {
        Item turkishItem =
            new Item.ItemBuilder(
                "Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге",
                1,
                BigDecimal.valueOf(135)
            )
                .setArticle("75690200345480.Checkouter-test-20")
                .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
                .setCategoryName("мебель")
                .setCargoType(CargoType.ANIMALS)
                .setCargoTypes(new CargoTypes(Arrays.asList(CargoType.ART, CargoType.FRAGILE_CARGO)))
                .setItemDescriptionEnglish("Item description")
                .setTransitData(createTurkishTransitData())
                .setInstances(createInstances())
                .setSupplier(
                    Supplier.builder()
                        .setName("Поставщик товара")
                        .setPhone(new Phone.PhoneBuilder("+79876543210").build())
                        .setInn("1231231234")
                        .build()
                )
                .setBase64EncodedInstances(createBase64EncodedInstances())
                .build();

        Item chineseItem = new Item.ItemBuilder("Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге", 1,
            BigDecimal.valueOf(135))
            .setArticle("75690200345481.Checkouter-test-20")
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
            .setCategoryName("мебель")
            .setCargoType(CargoType.LIQUID_CARGO)
            .setCargoTypes(new CargoTypes(Arrays.asList(CargoType.ABSORB_SMELL, CargoType.ANIMALS)))
            .setItemDescriptionEnglish("Item description")
            .setTransitData(createChineseTransitData())
            .setInstances(createInstances())
            .setBase64EncodedInstances(createBase64EncodedInstances())
            .build();

        return Arrays.asList(turkishItem, chineseItem);
    }

    private static TransitData createTurkishTransitData() {
        return new TransitData(Collections.singletonList(createTurkishCustomsTranslation()));
    }

    private static CustomsTranslation createTurkishCustomsTranslation() {
        return new CustomsTranslation(
            "tr",
            "Değişen dresser kova Teddy bear No. 2 Fildişi-wenge",
            "mobilya"
        );
    }

    private static TransitData createChineseTransitData() {
        return new TransitData(Collections.singletonList(createChineseCustomsTranslation()));
    }

    private static CustomsTranslation createChineseCustomsTranslation() {
        return new CustomsTranslation(
            "zh",
            "尿布更换胸部vedruss熊No.2象牙-wenge",
            "家具"
        );
    }

    public static List<ItemPlace> createItemPlaces() {
        return Collections.singletonList(
            new ItemPlace(new UnitId(0L, "75690200345480.Checkouter-test-20"), 1));
    }

    public static Recipient createRecipient() {
        return createRecipient(null);
    }

    public static Recipient createRecipient(PersonalDataStatus personalDataStatus) {
        return new Recipient.RecipientBuilder(
            new Person("Инокетий", "Смоктуновский", "Иванович"),
            Collections.singletonList(new Phone.PhoneBuilder("+79607481463").build())
        )
            .setEmail("ololoshenka@yandex-team.ru")
            .setPersonalDataStatus(personalDataStatus)
            .setRecipientData(new RecipientData(createResourceId()))
            .setUid(1000000001L)
            .build();
    }

    public static PersonalRecipient createPersonalRecipient() {
        List<PersonalPhone> personalPhones = Arrays.asList(
            new PersonalPhone("phone_id_1", "123"),
            new PersonalPhone("phone_id_2", null)
        );
        return new PersonalRecipient.PersonalRecipientBuilder("fio_id_1", personalPhones)
            .setPersonalEmailId("email_id_1")
            .setPersonalDataStatus(PersonalDataStatus.GATHERED)
            .setRecipientData(new RecipientData(createResourceId()))
            .setUid(1000000001L)
            .build();
    }

    public static Sender createSender() {
        return createSender(
            "ИП «Тестовый виртуальный магазин проекта Фулфиллмент»",
            createLocationFrom(),
            "111111111111111"
        );
    }

    public static Sender createSender(String incorporation, Location address, String ogrn) {
        return new Sender.SenderBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId("431782").build(),
            incorporation,
            ogrn)
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+7 1234567121").build()))
            .setName("Тестовый магазин проекта Фулфиллмент")
            .setType("ip")
            .setTaxation(Taxation.OSN)
            .setUrl("http://delivery.ff")
            .setAddress(address)
            .setInn("222222222222222")
            .setLegalForm(LegalForm.IP)
            .build();
    }

    private static Location createShipmentAddress() {
        return new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
            .setFederalDistrict("Центральный федеральный округ")
            .setSubRegion("Городской округ Котельники")
            .setStreet("Яничкин проезд")
            .setHouse("7")
            .setLocationId(21651L)
            .build();
    }

    public static Warehouse createWarehouse() {
        return new Warehouse.WarehouseBuilder(new ResourceId.ResourceIdBuilder().setYandexId("9955214").build(),
            createShipmentAddress(),
            Collections.singletonList(
                new WorkTime.WorkTimeBuilder(1, Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00" +
                    "+03:00"))).build()))
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("++74951234567").build()))
            .build();
    }

    private static List<Service> createServices() {
        return Collections.singletonList(
            new Service.ServiceBuilder(new ResourceId.ResourceIdBuilder().setYandexId("DELIVERY").setDeliveryId(
                "DELIVERY").build(),
                false
            )
                .setName("Доставка")
                .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.NO_NDS)))
                .build());
    }

    public static Order createOrder() {
        return createOrder(createSender(), PaymentMethod.PREPAID, null, null);
    }

    public static Order createOrderWithPersonalData() {
        return createOrder(
            createSender(),
            PaymentMethod.PREPAID,
            createPersonalRecipient(),
            createPersonalLocationTo()
        );
    }

    public static Order createOrder(
        Sender sender,
        PaymentMethod paymentMethod,
        PersonalRecipient personalRecipient,
        PersonalLocation personalLocationTo
    ) {
        return new Order.OrderBuilder(
            createResourceId(),
            createLocationFrom(),
            createLocationTo(),
            BigDecimal.valueOf(22),
            46,
            66,
            46,
            BigDecimal.valueOf(787.2),
            BigDecimal.valueOf(886.2),
            paymentMethod,
            "Самовывоз",
            DeliveryType.PICKUP_POINT,
            BigDecimal.valueOf(99),
            BigDecimal.ZERO,
            createItems(),
            createRecipient(),
            sender
        )
            .setWarehouseFrom(createWarehouse())
            .setCargoType(CargoType.UNKNOWN)
            .setAmountPrepaid(BigDecimal.ZERO)
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0)))
            .setPickupPointCode("YEKB6")
            .setPickupPointId(
                new ResourceId.ResourceIdBuilder().setYandexId("10000001234").setDeliveryId("YEKB6").build()
            )
            .setWarehouse(createWarehouse())
            .setServices(createServices())
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 22, 0, 0)))
            .setShipmentDateTime(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 22, 16, 30)))
            .setShipmentPointCode("main")
            .setPlaces(Collections.singletonList(
                new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setPartnerId("2222").build())
                    .setKorobyte(new Korobyte.KorobyteBuilder()
                        .setWidth(1)
                        .setHeight(2)
                        .setLength(3)
                        .setWeightGross(BigDecimal.TEN)
                        .build()
                    )
                    .setItemPlaces(createItemPlaces())
                    .build()))
            .setTags(Collections.singleton("ON_DEMAND"))
            .setPersonalRecipient(personalRecipient)
            .setPersonalLocationTo(personalLocationTo)
            .build();
    }

    public static CreateOrderYandexGoData createOrderYandexGoData() {
        return new CreateOrderYandexGoData(
            "4015cb74-1e50-43ee-b466-d47e959fdc90",
            25,
            13,
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 3, 22, 0, 0))
        );
    }

    public static CreateOrderRestrictedData createOrderRestrictedData() {
        return CreateOrderRestrictedData.builder()
            .setYandexGoData(createOrderYandexGoData())
            .setTransferCodes(ru.yandex.market.logistic.api.utils.common.DtoFactory.createOrderTransferCodes())
            .setPromise("promise")
            .build();
    }

    public static GetOrderRestrictedData getOrderRestrictedData() {
        return new GetOrderRestrictedData(new GetOrderYandexGoData(createCourier()));
    }

    public static OrderDeliveryDate createOrderDeliveryDate() {
        return new OrderDeliveryDate.OrderDeliveryDateBuilder(createOrderId(),
            DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0)))
            .setDeliveryInterval(new TimeInterval("03:00:00+03:00/03:59:00+03:00"))
            .setMessage("Message")
            .build();
    }

    public static Order createOrderNotTrimmed() {
        Sender sender = createSender("   ИП «Тестовый виртуальный магазин проекта Фулфиллмент» \t ",
            createLocationFrom(),
            "111111111111111");
        return createOrder(sender, PaymentMethod.PREPAID, null, null);
    }

    private static Courier.CourierBuilder courierBuilderWithCommonFields() {
        return new Courier.CourierBuilder(Collections.singletonList(new Person("Иван", "Доставляев", "Васильевич")))
            .setCar(createCar())
            .setPhone(new Phone.PhoneBuilder("++74951234567").build());
    }

    public static Courier createCourier() {
        return courierBuilderWithCommonFields().build();
    }


    private static Car createCar() {
        Car.CarBuilder carBuilder = new Car.CarBuilder("А019МР199");
        carBuilder.setDescription("Моя красная тачка");
        return carBuilder.build();

    }

    public static Courier createDetailedCourier() {
        return new Courier
            .CourierBuilder(Collections.singletonList(new Person("Роман", "Лобанов", "Викторович")))
            .build();
    }

    public static Warehouse createDetailedWarehouse() {
        return new Warehouse.WarehouseBuilder(new ResourceId.ResourceIdBuilder().setDeliveryId("1").setYandexId("9805"
        ).build(),
            new Location.LocationBuilder("Россия", "Москва и Московская область", "Москва")
                .setFederalDistrict("Центральный федеральный округ")
                .setStreet("Огородный проезд")
                .setSettlement("")
                .setHousing("")
                .setRoom("")
                .setHouse("20")
                .setBuilding("3")
                .setZipCode("0")
                .setLat(BigDecimal.valueOf(55.753960))
                .setLng(BigDecimal.valueOf(37.620393))
                .setLocationId(213L)
                .build(),
            IntStream.rangeClosed(1, 7)
                .mapToObj(i -> new WorkTime.WorkTimeBuilder(i, Collections.singletonList(new TimeInterval("10:00:00" +
                    "+03:00/22:00:00+03:00"))).build())
                .collect(Collectors.toList()))
            .setInstruction("")
            .setContact(new Person.PersonBuilder("старший", "смены").setPatronymic("").build())
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("79670496184").build()))
            .build();
    }

    public static UpdateOrderDeliveryRequest createUpdateOrderDelivery() {
        return new UpdateOrderDeliveryRequest.UpdateOrderDeliveryRequestBuilder(
            new ResourceId.ResourceIdBuilder()
                .setDeliveryId("1")
                .setYandexId("9805")
                .setPartnerId("1")
                .build()
        )
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0)))
            .setDeliveryInterval(new TimeInterval("10:00:00+03:00/22:00:00+03:00"))
            .setDeliveryType(DeliveryType.COURIER)
            .setLocationTo(
                new Location.LocationBuilder("Россия", "Москва и Московская область", "Москва")
                    .setFederalDistrict("Центральный федеральный округ")
                    .setStreet("Огородный проезд")
                    .setSettlement("")
                    .setHousing("")
                    .setRoom("")
                    .setHouse("20")
                    .setBuilding("3")
                    .setZipCode("0")
                    .setLat(BigDecimal.valueOf(55.753960))
                    .setLng(BigDecimal.valueOf(37.620393))
                    .setLocationId(213L)
                    .build())
            .setPickupPointCode("123")
            .setServices(Collections.singletonList(
                new Service.ServiceBuilder(new ResourceId.ResourceIdBuilder()
                    .setYandexId("DELIVERY_DATE_CHANGE")
                    .setDeliveryId("DELIVERY_DATE_CHANGE")
                    .build(), false)
                    .setName("Доставка")
                    .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.NO_NDS)))
                    .build()))
            .build();
    }

}
