package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.Car;
import ru.yandex.market.logistic.api.model.fulfillment.Consignor;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnBox;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnInbound;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnInboundType;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnItem;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateReturnInboundResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.model.fulfillment.CargoType.PERISHABLE_CARGO;
import static ru.yandex.market.logistic.api.model.fulfillment.ServiceType.SORT;
import static ru.yandex.market.logistic.api.model.fulfillment.ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY;
import static ru.yandex.market.logistic.api.model.fulfillment.TaxType.VAT;
import static ru.yandex.market.logistic.api.model.fulfillment.VatValue.TEN;

/**
 * Тест для {@link FulfillmentClient#createReturnInbound(ReturnInbound, PartnerProperties)}.
 */
public class CreateReturnInboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "23456";
    private static final String PARTNER_ID = "Zakazik";

    private static final OffsetDateTime INTERVAL_FROM = LocalDateTime.of(2019, 1, 2, 11, 0, 0)
        .atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime INTERVAL_TO = LocalDateTime.of(2019, 2, 12, 12, 0, 5)
        .atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime MAX_RECEIPT_DATE = LocalDateTime.of(2019, 1, 21, 11, 0, 0)
        .atOffset(ZoneOffset.UTC);

    @Test
    void testCreateReturnInboundSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_return_inbound", PARTNER_URL);


        CreateReturnInboundResponse response =
            fulfillmentClient.createReturnInbound(getReturnInbound(), getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ CreateInboundResponse"
        );
    }

    @Test
    void testCreateReturnInboundWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_create_return_inbound", "ff_create_return_inbound_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createReturnInbound(getReturnInbound(), getPartnerProperties())
        );
    }

    private ReturnInbound getReturnInbound() {
        Item item = new Item.ItemBuilder("Name", 10, BigDecimal.valueOf(130.5))
            .setUnitId(new UnitId.UnitIdBuilder(2L, "art")
                .setId("1")
                .build())
            .setArticle("Article")
            .setVendorCodes(Arrays.asList("123", "456", "ABC"))
            .setCategoryId(10L)
            .setBarcodes(
                Collections.singletonList(
                    new Barcode.BarcodeBuilder("code")
                        .setType("type")
                        .setSource(BarcodeSource.UNKNOWN)
                        .build()))
            .setInboundServices(Arrays.asList(
                new Service.ServiceBuilder(SORT, false)
                    .setName("Lunapark with BlackJack and whores")
                    .setDescription("Short description for current service")
                    .build(),
                new Service.ServiceBuilder(STORE_DEFECTIVE_ITEMS_SEPARATELY, false)
                    .setName("Check ignore case")
                    .setDescription("Short description for current service")
                    .build()
            ))
            .setDescription("description")
            .setCargoType(PERISHABLE_CARGO)
            .setKorobyte(new Korobyte.KorobyteBuiler(100, 101, 102, BigDecimal.valueOf(100.0))
                .setWeightNet(BigDecimal.valueOf(100.1))
                .setWeightTare(BigDecimal.valueOf(100.2))
                .build())
            .setHasLifeTime(true)
            .setBoxCapacity(50)
            .setTax(new Tax(VAT, TEN))
            .build();

        Item item2 = new Item.ItemBuilder("Name", 10, BigDecimal.valueOf(130.5))
            .setUnitId(new UnitId.UnitIdBuilder(2L, "art")
                .setId("2")
                .build())
            .setContractor(new Contractor("Id", "Name"))
            .setArticle("Article")
            .setVendorCodes(Arrays.asList("123", "456", "ABC"))
            .setBarcodes(
                Collections.singletonList(
                    new Barcode.BarcodeBuilder("code")
                        .setType("type")
                        .setSource(BarcodeSource.UNKNOWN)
                        .build()))
            .setInboundServices(Arrays.asList(
                new Service.ServiceBuilder(SORT, false)
                    .setName("Lunapark with BlackJack and whores")
                    .setDescription("Short description for current service")
                    .build(),
                new Service.ServiceBuilder(STORE_DEFECTIVE_ITEMS_SEPARATELY, false)
                    .setName("Check ignore case")
                    .setDescription("Short description for current service")
                    .build()
            ))
            .setDescription("description")
            .setCargoType(PERISHABLE_CARGO)
            .setKorobyte(new Korobyte.KorobyteBuiler(100, 101, 102, BigDecimal.valueOf(100.0))
                .setWeightNet(BigDecimal.valueOf(100.1))
                .setWeightTare(BigDecimal.valueOf(100.2))
                .build())
            .setHasLifeTime(true)
            .setBoxCapacity(50)
            .setTax(new Tax(VAT, TEN))
            .setUrls(Collections.singletonList("https://beru.ru/product/1723947007"))
            .setSurplusAllowed(true)
            .build();

        ReturnBox returnBox = new ReturnBox.ReturnBoxBuilder(
            "box23456",
            true)
            .setBarcodes(
                Collections.singletonList(
                    new Barcode.BarcodeBuilder("boxBarcode23456")
                        .setType("type")
                        .setSource(BarcodeSource.UNKNOWN)
                        .build()))
            .setOrderId("order23456")
            .setBoxesInOrder(2)
            .setMaxReceiptDate(DateTime.fromOffsetDateTime(MAX_RECEIPT_DATE))
            .build();

        ReturnBox returnBox2 = new ReturnBox.ReturnBoxBuilder(
            "box234567",
            false)
            .setBarcodes(
                Collections.singletonList(
                    new Barcode.BarcodeBuilder("boxBarcode234567")
                        .setType("type")
                        .setSource(BarcodeSource.UNKNOWN)
                        .build()))
            .setOrderId("order23456")
            .setMaxReceiptDate(DateTime.fromOffsetDateTime(MAX_RECEIPT_DATE))
            .build();

        ReturnItem returnItem = new ReturnItem.ReturnItemBuilder("order23456", item)
                .setSourceFulfillmentId(145L)
                .setBoxIds(Arrays.asList("box23456", "box234567"))
                .build();
        ReturnItem returnItem2 = new ReturnItem.ReturnItemBuilder("order234567", item2).build();

        Consignor consignor = new Consignor.ConsignorBuilder()
                .setId(190L)
                .setName("Служба доставки")
                .build();

        return new ReturnInbound.ReturnInboundBuilder(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setFulfillmentId(PARTNER_ID)
            .setPartnerId(PARTNER_ID)
            .build(),
            ReturnInboundType.VALID_UNREDEEMED,
            consignor,
            "АПП №5",
            Arrays.asList(returnBox, returnBox2),
            new DateTimeInterval(INTERVAL_FROM, INTERVAL_TO))
            .setReturnItems(Arrays.asList(returnItem, returnItem2))
            .setCourier(new Courier.CourierBuilder(Collections.singletonList(new Person.PersonBuilder("Skyler")
                .build())
            )
                .setCar(new Car("RU36577", null))
                .build())
            .setComment("TestComment")
            .build();
    }

    private CreateReturnInboundResponse getExpectedResponse() {
        return new CreateReturnInboundResponse(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setFulfillmentId(PARTNER_ID)
            .setPartnerId(PARTNER_ID)
            .build());
    }
}
