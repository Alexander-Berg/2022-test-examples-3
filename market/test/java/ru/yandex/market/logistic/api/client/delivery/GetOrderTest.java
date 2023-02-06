package ru.yandex.market.logistic.api.client.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.logistic.api.model.delivery.CargoTypes;
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
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.restricted.GetOrderRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.getOrderRestrictedData;

class GetOrderTest extends CommonServiceClientTest {

    @Test
    void testGetOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_order", PARTNER_URL);

        GetOrderResponse response = deliveryServiceClient.getOrder(
            new ResourceId.ResourceIdBuilder().setYandexId("12345").build(), getPartnerProperties()
        );
        assertEquals(getExpectedResponse(), response, "Должен вернуть корректный ответ GetOrderResponse");
    }

    @Test
    void testGetOrderWithRestrictedDataSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_order",
            "ds_get_order_with_restricted_data",
            PARTNER_URL);

        GetOrderResponse response = deliveryServiceClient.getOrder(
            new ResourceId.ResourceIdBuilder().setYandexId("12345").build(), getPartnerProperties()
        );
        assertEquals(
            getExpectedResponse(getOrderRestrictedData()),
            response,
            "Должен вернуть корректный ответ GetOrderResponse"
        );
    }

    @Test
    void testGetOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_order", "ds_get_order_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getOrder(
                new ResourceId.ResourceIdBuilder().setYandexId("12345").build(), getPartnerProperties()
            )
        );
    }

    private GetOrderResponse getExpectedResponse() {
        return getExpectedResponse(null);
    }

    private GetOrderResponse getExpectedResponse(@Nullable GetOrderRestrictedData restrictedData) {
        Order order = new Order.OrderBuilder(new ResourceId.ResourceIdBuilder().setYandexId("2384813").build(),
            new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                .setFederalDistrict("Центральный федеральный округ")
                .setStreet("Яничкин проезд")
                .setHouse("7")
                .setLocationId(213L)
                .build(),
            new Location.LocationBuilder("Россия", "Свердловская область", "Екатеринбург")
                .setFederalDistrict("Уральский федеральный округ")
                .setSubRegion("Муниципальное образование Екатеринбург")
                .setStreet("ул. Техническая")
                .setHouse("16")
                .setLocationId(54L)
                .build(),
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
            BigDecimal.valueOf(0),
            Collections.singletonList(new Item.ItemBuilder(
                "Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге",
                1,
                BigDecimal.valueOf(135)
            )
                .setArticle("75690200345480.Checkouter-test-20")
                .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
                .setCategoryName("мебель")
                .setCargoType(CargoType.UNKNOWN)
                .setCargoTypes(new CargoTypes(Collections.singletonList(CargoType.ART)))
                .setItemDescriptionEnglish("Item description")
                .setTransitData(new TransitData(Collections.singletonList(
                    new CustomsTranslation("tr",
                        "Değişen dresser kova Teddy bear No. 2 Fildişi-wenge",
                        "mobilya"))))
                .build()),
            new Recipient.RecipientBuilder(new Person("Инокетий", "Смоктуновский", "Иванович"),
                Collections.singletonList(new Phone.PhoneBuilder("+79607481463").build()))
                .setEmail("ololoshenka@yandex-team.ru")
                .setRecipientData(new RecipientData(new ResourceId.ResourceIdBuilder().setYandexId("2384813").build()))
                .build(),
            new Sender.SenderBuilder(new ResourceId.ResourceIdBuilder().setYandexId("431782").build(),
                "ИП «Тестовый виртуальный магазин проекта Фулфиллмент»",
                "111111111111111")
                .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+7 1234567121").build()))
                .setName("Тестовый магазин проекта Фулфиллмент")
                .setType("ip")
                .setUrl("http://delivery.ff")
                .setTaxation(Taxation.OSN)
                .setInn("222222222222222")
                .setLegalForm(LegalForm.IP)
                .setAddress(new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                    .setFederalDistrict("Центральный федеральный округ")
                    .setStreet("Яничкин проезд")
                    .setHouse("7")
                    .setLocationId(213L)
                    .build())
                .build()
        )
            .setCargoType(CargoType.UNKNOWN)
            .setAmountPrepaid(BigDecimal.valueOf(0))
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 29, 0, 0, 0)))
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 8, 22, 0, 0, 0)))
            .setPickupPointCode("YEKB6")
            .setWarehouse(new Warehouse.WarehouseBuilder(
                new ResourceId.ResourceIdBuilder().setYandexId("9955214").build(),
                new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                    .setFederalDistrict("Центральный федеральный округ")
                    .setSubRegion("Городской округ Котельники")
                    .setStreet("Яничкин проезд")
                    .setHouse("7")
                    .setLocationId(21651L)
                    .build(),
                Collections.singletonList(
                    new WorkTime.WorkTimeBuilder(1, Collections.singletonList(
                        new TimeInterval("03:00:00+03:00/02:59:00+03:00"))).build()))
                .setPhones(Collections.singletonList(new Phone.PhoneBuilder("++74951234567").build()))
                .build())
            .setServices(Collections.singletonList(
                new Service.ServiceBuilder(new ResourceId.ResourceIdBuilder().setYandexId("DELIVERY").setDeliveryId(
                    "DELIVERY").build(), false)
                    .setName("Доставка")
                    .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.NO_NDS)))
                    .build()))
            .setShipmentPointCode("main")
            .setPlaces(Collections.singletonList(
                new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setPartnerId("2222").build())
                    .setItemPlaces(
                        Collections.singletonList(
                            new ItemPlace(new UnitId(0L, "75690200345480.Checkouter-test-20"), 1)))
                    .setKorobyte(new Korobyte.KorobyteBuilder()
                        .setWidth(1)
                        .setHeight(2)
                        .setLength(3)
                        .setWeightGross(BigDecimal.TEN)
                        .build()
                    )
                    .build()))
            .setTags(Collections.singleton("ON_DEMAND"))
            .build();

        return new GetOrderResponse(order, restrictedData);
    }
}
