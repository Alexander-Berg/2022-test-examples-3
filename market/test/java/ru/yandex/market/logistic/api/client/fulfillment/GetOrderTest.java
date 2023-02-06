package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.api.model.fulfillment.DocData;
import ru.yandex.market.logistic.api.model.fulfillment.DocTemplate;
import ru.yandex.market.logistic.api.model.fulfillment.DocTemplateType;
import ru.yandex.market.logistic.api.model.fulfillment.Email;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.Place;
import ru.yandex.market.logistic.api.model.fulfillment.Recipient;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Sender;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.Taxation;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistic.api.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.api.model.fulfillment.WorkTime;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderTest extends CommonServiceClientTest {

    @Test
    void testGetOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_order", PARTNER_URL);

        GetOrderResponse response =
            fulfillmentClient.getOrder(
                new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                getPartnerProperties()
            );
        assertEquals(getExpectedResponse(), response, "Должен вернуть корректный ответ GetOrderResponse");
    }

    @Test
    void testGetOrderWithOnlyOrderIdAndPlacesInResponseSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_order_with_only_order_id_and_places_in_response",
            PARTNER_URL);

        GetOrderResponse response =
            fulfillmentClient.getOrder(
                new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                getPartnerProperties()
            );
        assertEquals(getExpectedResponseWithOnlyOrderIdAndPlaces(), response,
            "Должен вернуть корректный ответ GetOrderResponse");
    }

    @Test
    void testGetOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_order_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOrder(
                new ResourceId.ResourceIdBuilder().setYandexId("12346").build(), getPartnerProperties()
            )
        );
    }

    @Test
    void testGetOrderValidationFailed() {
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getOrder(
                null, getPartnerProperties()
            )
        );
    }

    private GetOrderResponse getExpectedResponse() {

        Warehouse warehouse = new Warehouse.WarehouseBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId("555").build(),
            new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                .setFederalDistrict("Центральный федеральный округ")
                .setSubRegion("Городской округ Котельники")
                .setStreet("Яничкин проезд")
                .setHouse("7")
                .setLocationId(213)
                .build(),
            Collections.singletonList(
                new WorkTime(1, Collections.singletonList(
                    new TimeInterval("03:00:00+03:00/02:59:00+03:00")))),
            "тест Incorporation")
            .setResourceId(new ResourceId.ResourceIdBuilder().setYandexId("555").build())
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+74951234567").build()))
            .build();

        return new GetOrderResponse(new Order.OrderBuilder(
            getExpectedOrderId(),
            new Location.LocationBuilder("Россия", "Свердловская область", "Екатеринбург")
                .setFederalDistrict("Уральский федеральный округ")
                .setSubRegion("Муниципальное образование Екатеринбург")
                .setStreet("ул. Техническая")
                .setHouse("16")
                .setLocationId(54)
                .build(),
            Collections.singletonList(new Item.ItemBuilder("Пеленальный комод Ведрусс Мишка №2 слоновая кость-венге",
                1, BigDecimal.valueOf(135))
                .setUnitId(new UnitId.UnitIdBuilder(0L, "75690200345480.Checkouter-test-20").build())
                .setArticle("75690200345480.Checkouter-test-20")
                .setVendorCodes(Collections.singletonList("code_1"))
                .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder("barcode").build()))
                .setDescription("Описание")
                .setTax(new Tax(TaxType.VAT, VatValue.EIGHTEEN))
                .setUntaxedPrice(BigDecimal.valueOf(135))
                .setCargoType(CargoType.UNKNOWN)
                .setCargoTypes(new CargoTypes(Collections.singletonList(CargoType.ART)))
                .setKorobyte(new Korobyte.KorobyteBuiler(66, 46, 46, BigDecimal.valueOf(22)).build())
                .setHasLifeTime(false)
                .setUndefinedCount(13)
                .build()),
            new Korobyte.KorobyteBuiler(66, 46, 46, BigDecimal.valueOf(22)).build(),
            BigDecimal.valueOf(787.2),
            BigDecimal.valueOf(886.2),
            PaymentType.PREPAID,
            new Delivery.DeliveryBuilder(
                "Служба тест",
                Collections.singletonList(new Phone.PhoneBuilder("+79999999999").build()),
                "Контактное лицо",
                Collections.singletonList(
                    new DocTemplate("тест", 1, DocTemplateType.DOCUMENT, "template")),
                1)
                .setDeliveryId(new ResourceId.ResourceIdBuilder().setYandexId("123").build())
                .setIntakeTime(Collections.singletonList(
                    new WorkTime(1, Collections.singletonList(
                        new TimeInterval("14:30:00+03:00/15:30:00+03:00")))))
                .build(),
            DeliveryType.POST,
            BigDecimal.valueOf(99),
            new DocData.DocDataBuilder("doc name", 0).build(),
            warehouse,
            warehouse,
            new Recipient.RecipientBuilder(
                new Person.PersonBuilder("Инокетий").setSurname("Смоктуновский").setPatronymic("Иванович").build(),
                Collections.singletonList(new Phone.PhoneBuilder("+79999999999").build()))
                .setEmail(new Email("ololoshenka@yandex-team.ru"))
                .build(),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100))
            .setPlaces(getExpectedPlaces())
            .setExternalId(new ResourceId.ResourceIdBuilder().setPartnerId("p2384813").build())
            .setWaybill(Collections.singletonList(new Param.ParamBuilder("key").setValue("value").setComment("comment"
            ).build()))
            .setLocationFrom(new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                .setFederalDistrict("Центральный федеральный округ")
                .setStreet("Яничкин проезд")
                .setHouse("7")
                .setLocationId(213)
                .build())
            .setWeight(BigDecimal.valueOf(22))
            .setWidth(66)
            .setHeight(46)
            .setLength(46)
            .setCargoType(CargoType.UNKNOWN)
            .setPickupPointCode("YEKB6")
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 1, 1, 10, 0, 0)))
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 1, 1, 10, 0, 0)))
            .setShipmentDateTime(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 1, 1, 17, 48, 0)))
            .setDeliveryInterval(new TimeInterval("14:30:00+03:00/15:30:00+03:00"))
            .setServices(Collections.singletonList(
                new Service.ServiceBuilder(ServiceType.OTHER, false).setName("Доставка").build()))
            .setSender(new Sender.SenderBuilder(new ResourceId.ResourceIdBuilder().setYandexId("431782").build(),
                "ИП «Тестовый виртуальный магазин проекта Фулфиллмент»",
                "http://delivery.ff")
                .setPartnerId("7654321")
                .setName("Тестовый магазин проекта Фулфиллмент")
                .setInn("222222222222222")
                .setOgrn("111111111111111")
                .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+7 1234567121").build()))
                .setEmail("delivery@yandex.ru")
                .setContact(new Person.PersonBuilder("Имя").build())
                .setLegalForm(LegalForm.OOO)
                .setAddress(new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                    .setFederalDistrict("Центральный федеральный округ")
                    .setStreet("Яничкин проезд")
                    .setHouse("7")
                    .setLocationId(213)
                    .build())
                .setTaxation(Taxation.OSN)
                .setType("ip")
                .build())
            .setTariff("Самовывоз")
            .setToFulfillment(true)
            .setTags(Collections.singleton("EXPRESS"))
            .build());
    }

    private GetOrderResponse getExpectedResponseWithOnlyOrderIdAndPlaces() {
        return new GetOrderResponse(new Order.OrderBuilder(getExpectedOrderId(),
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            .setPlaces(getExpectedPlaces())
            .build());
    }

    private ResourceId getExpectedOrderId() {
        return new ResourceId.ResourceIdBuilder().setYandexId("2384813").build();
    }

    private List<Place> getExpectedPlaces() {
        return Collections.singletonList(
            new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setPartnerId("2222").build())
                .setItemPlaces(
                    Collections.singletonList(
                        new ItemPlace(new UnitId.UnitIdBuilder(0L, "75690200345480.Checkouter-test-20").build(),
                            1)))
                .setKorobyte(new Korobyte.KorobyteBuiler(1, 2, 3, BigDecimal.TEN).build())
                .build());
    }
}
