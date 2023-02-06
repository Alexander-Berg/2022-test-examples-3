package ru.yandex.market.logistic.gateway.utils;

import java.math.BigDecimal;
import java.util.Collections;

import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.DocData;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderParcelId;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Place;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;

public class DeliveryDtoFactory {

    private DeliveryDtoFactory() {
        throw new AssertionError();
    }

    public static Order createOrder() {
        return createOrder("111");
    }

    public static Order createOrder(String yandexId) {
        return new Order.OrderBuilder(
            createResourceId(yandexId),
            createLocation(),
            createLocation(),
            new Korobyte.KorobyteBuilder()
                .setWidth(1)
                .setHeight(1)
                .setLength(1)
                .setWeightGross(BigDecimal.ONE)
                .build(),
            Collections.singletonList(createItem()),
            "Безлимит",
            BigDecimal.valueOf(2250),
            PaymentMethod.CARD,
            DeliveryType.PICKUP_POINT,
            BigDecimal.ONE,
            createRecipient(),
            BigDecimal.ONE,
            createSender()
        )
            .setShipmentDate(new DateTime("2018-04-20T17:00:00+03:00"))
            .setPlaces(Collections.singletonList(createPlace()))
            .setAmountPrepaid(BigDecimal.valueOf(1000))
            .setCargoType(CargoType.ANIMALS)
            .setCargoCost(BigDecimal.valueOf(1000))
            .setDocumentData(createDocData())
            .build();
    }

    public static OrderParcelId createOrderParcelId() {
        return new OrderParcelId(createResourceId("111"), createResourceId("111"));
    }

    public static ResourceId createResourceId(String yandexId) {
        return ResourceId.builder().setYandexId(yandexId).build();
    }

    public static Location createLocation() {
        return new Location.LocationBuilder("Russia", "Moscow", "The federal city of Moscow")
            .build();
    }

    private static DocData createDocData() {
        return new DocData.DocDataBuilder("Big doc data").setVersion(100500).build();
    }

    private static Sender createSender() {
        return new Sender.SenderBuilder("ООО \"Java-господа\"", "777")
            .setId(ResourceId.builder().build())
            .setUrl("http://localhost/")
            .setLegalForm(LegalForm.OOO)
            .setInn("666")
            .setAddress(createLocation())
            .setTaxation(Taxation.OSN)
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("7777777777").build()))
            .build();
    }

    private static Place createPlace() {
        return new Place.PlaceBuilder(ResourceId.builder().build(),
            new Korobyte.KorobyteBuilder().build())
            .build();
    }

    private static Recipient createRecipient() {
        return new Recipient.RecipientBuilder(new Person("Афанасий", "Иванов", "Эдуардович"),
            Collections.singletonList(new Phone("8-800-555-35-35", null)))
            .build();
    }

    private static Item createItem() {
        return new Item.ItemBuilder("Бутылка", 3, BigDecimal.valueOf(50.0)).build();
    }

    public static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2019-02-14T00:00:00+03:00/2019-02-21T00:00:00+03:00");
    }
}

