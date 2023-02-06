package ru.yandex.market.logistic.gateway.utils.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import ru.yandex.market.logistic.api.model.delivery.Phone;
import ru.yandex.market.logistic.api.model.delivery.Place;
import ru.yandex.market.logistic.api.model.delivery.Recipient;
import ru.yandex.market.logistic.api.model.delivery.RecipientData;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.model.delivery.Service;
import ru.yandex.market.logistic.api.model.delivery.Tax;
import ru.yandex.market.logistic.api.model.delivery.TaxType;
import ru.yandex.market.logistic.api.model.delivery.Taxation;
import ru.yandex.market.logistic.api.model.delivery.TransitData;
import ru.yandex.market.logistic.api.model.delivery.UnitId;
import ru.yandex.market.logistic.api.model.delivery.VatValue;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.model.delivery.WorkTime;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.api.utils.TimeInterval;

public class ApiDtoFactory {

    // same class as in logistics-api

    public static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-02-14T00:00:00+03:00/2019-02-21T00:00:00+03:00");

    public static final float VOLUME = 1.1f;
    public static final float WEIGHT = 5.5f;

    public static ResourceId createOrderId() {
        return new ResourceId.ResourceIdBuilder().setYandexId("12345").build();
    }

    private static ResourceId createResourceId() {
        return createResourceId("2384813", null);
    }

    public static ResourceId createResourceId(String yandexId, String partnerId) {
        return new ResourceId.ResourceIdBuilder().setYandexId(yandexId).setPartnerId(partnerId).build();
    }

    private static Location createLocationFrom() {
        return new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
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

    private static List<Item> createItems() {
        Item turkishItem = new Item.ItemBuilder("Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге", 1, BigDecimal.valueOf(135))
            .setArticle("75690200345480.Checkouter-test-20")
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
            .setCategoryName("мебель")
            .setCargoType(CargoType.ANIMALS)
            .setCargoTypes(new CargoTypes(Arrays.asList(CargoType.ART, CargoType.FRAGILE_CARGO)))
            .setItemDescriptionEnglish("Item description")
            .setTransitData(createTurkishTransitData())
            .setInstances(createItemInstances())
            .build();

        Item chineseItem = new Item.ItemBuilder("Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге", 1, BigDecimal.valueOf(135))
            .setArticle("75690200345481.Checkouter-test-20")
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
            .setCategoryName("мебель")
            .setCargoType(CargoType.LIQUID_CARGO)
            .setCargoTypes(new CargoTypes(Arrays.asList(CargoType.ABSORB_SMELL, CargoType.ANIMALS)))
            .setItemDescriptionEnglish("Item description")
            .setTransitData(createChineseTransitData())
            .setInstances(createItemInstances())
            .build();

        return Arrays.asList(turkishItem, chineseItem);
    }

    public static List<Map<String, String>> createItemInstances() {
        return List.of(
            Map.of("cis", "123abc"),
            Map.of("cis", "cba321")
        );
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
        return new Recipient.RecipientBuilder(new Person.PersonBuilder("Инокетий", "Смоктуновский").setPatronymic("Иванович").build(),
            Collections.singletonList(new Phone.PhoneBuilder("+79607481463").build()))
            .setEmail("ololoshenka@yandex-team.ru")
            .setRecipientData(new RecipientData(createResourceId()))
            .build();
    }

    public static Sender createSender() {
        return new Sender.SenderBuilder(new ResourceId.ResourceIdBuilder().setYandexId("431782").build(),
            "ИП «Тестовый виртуальный магазин проекта Фулфиллмент»",
            "111111111111111")
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+7 1234567121").build()))
            .setName("Тестовый магазин проекта Фулфиллмент")
            .setType("ip")
            .setTaxation(Taxation.OSN)
            .setUrl("http://delivery.ff")
            .setAddress(createLocationFrom())
            .setInn("222222222222222")
            .setLegalForm(LegalForm.IP)
            .build();
    }

    private static Location createShipmentAddress() {
        return new Location.LocationBuilder("Россия", "Свердловская область", "Екатеринбург")
            .setFederalDistrict("Уральский федеральный округ")
            .setSubRegion("Муниципальное образование Екатеринбург")
            .setStreet("ул. Техническая")
            .setHouse("16")
            .setLocationId(54L)
            .build();
    }

    public static Warehouse createWarehouse() {
        return new Warehouse.WarehouseBuilder(new ResourceId.ResourceIdBuilder().setYandexId("9955214").build(),
            createShipmentAddress(),
            Collections.singletonList(
                new WorkTime(1, Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00")))))
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("++74951234567").build()))
            .build();
    }

    private static List<Service> createServices() {
        return Collections.singletonList(new Service.ServiceBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId("DELIVERY").setPartnerId("DELIVERY").build(),
            false)
            .setName("Доставка")
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.NO_NDS)))
            .build());
    }


    public static Order createOrder() {
        return new Order.OrderBuilder(createResourceId(),
            createLocationFrom(),
            createLocationTo(),
            BigDecimal.valueOf(22),
            46,
            66,
            46,
            BigDecimal.valueOf(787.2),
            BigDecimal.valueOf(886.2),
            PaymentMethod.PREPAID,
            "Самовывоз",
            DeliveryType.PICKUP_POINT,
            BigDecimal.valueOf(99),
            BigDecimal.ZERO,
            createItems(),
            createRecipient(),
            createSender())
            .setCargoType(CargoType.UNKNOWN)
            .setAmountPrepaid(BigDecimal.ZERO)
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0)))
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 22, 0, 0)))
            .setPickupPointCode("YEKB6")
            .setWarehouse(createWarehouse())
            .setServices(createServices())
            .setShipmentPointCode("main")
            .setPlaces(Collections.singletonList(
                new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setPartnerId("2222").build())
                    .setKorobyte(new Korobyte.KorobyteBuilder().setWidth(1).setHeight(2).setLength(3).setWeightGross(BigDecimal.TEN).build())
                    .setItemPlaces(createItemPlaces())
                    .build()))
            .build();
    }

    public static OrderDeliveryDate createOrderDeliveryDate(){
        return new OrderDeliveryDate.OrderDeliveryDateBuilder(createOrderId(), DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0)))
            .setDeliveryInterval(new TimeInterval("03:00:00+03:00/03:59:00+03:00"))
            .setMessage("Message")
            .build();
    }


    public static Courier createCourier() {
        return new Courier.CourierBuilder(Collections.singletonList(new Person.PersonBuilder("Иван", "Доставляев").setPatronymic("Васильевич").build()))
            .setCar(createCar())
            .build();
    }


    private static Car createCar() {
        return new Car.CarBuilder("А019МР199")
            .setDescription("Моя красная тачка")
            .build();
    }


}
