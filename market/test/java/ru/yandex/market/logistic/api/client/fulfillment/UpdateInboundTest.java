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
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.InboundType;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateInboundResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.model.fulfillment.CargoType.PERISHABLE_CARGO;
import static ru.yandex.market.logistic.api.model.fulfillment.ServiceType.SORT;
import static ru.yandex.market.logistic.api.model.fulfillment.ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY;
import static ru.yandex.market.logistic.api.model.fulfillment.TaxType.VAT;
import static ru.yandex.market.logistic.api.model.fulfillment.VatValue.TEN;


/**
 * Тест для {@link FulfillmentClient#updateInbound(Inbound, PartnerProperties)}.
 */
class UpdateInboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "23456";
    private static final String PARTNER_ID = "Zakazik";

    private static final OffsetDateTime INTERVAL_FROM = LocalDateTime.of(2019, 1, 2, 11, 0, 0)
        .atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime INTERVAL_TO = LocalDateTime.of(2019, 2, 12, 12, 0, 5)
        .atOffset(ZoneOffset.UTC);

    @Test
    void testUpdateInboundSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_update_inbound", PARTNER_URL);


        UpdateInboundResponse response = fulfillmentClient.updateInbound(getInbound(), getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ UpdateInboundResponse"
        );
    }

    @Test
    void testUpdateInboundWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_update_inbound", "ff_update_inbound_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.updateInbound(getInbound(), getPartnerProperties())
        );
    }

    private Inbound getInbound() {
        Item item = new Item.ItemBuilder("Name", 10, BigDecimal.valueOf(130.5))
            .setUnitId(new UnitId.UnitIdBuilder(2L, "art")
                .setId("1")
                .build())
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
            .build();

        Consignment consignment = new Consignment.ConsignmentBuilder(item).build();
        Consignment consignment2 = new Consignment.ConsignmentBuilder(item2).build();

        return new Inbound.InboundBuilder(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setFulfillmentId(PARTNER_ID)
            .setPartnerId(PARTNER_ID)
            .build(),
            InboundType.DEFAULT,
            Arrays.asList(consignment, consignment2),
            new DateTimeInterval(INTERVAL_FROM, INTERVAL_TO))
            .setWarehouse(new Warehouse.WarehouseBuilder(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId("65655")
                    .setFulfillmentId("Y656D")
                    .setPartnerId("Y656D")
                    .build(),
                new Location.LocationBuilder(null, null, null).build(),
                null,
                "fillgoodinc")
                .setResourceId(new ResourceId.ResourceIdBuilder()
                    .setYandexId("98798")
                    .setFulfillmentId("FF4545")
                    .setPartnerId("FF4545")
                    .build())
                .setContact(new Person.PersonBuilder("SayMyName").build())
                .setPhones(Collections.singletonList(new Phone.PhoneBuilder("+79613888370").build()))
                .build())
            .setCourier(new Courier.CourierBuilder(Collections.singletonList(new Person.PersonBuilder("Skyler")
                .build())
            )
                .setCar(new Car("RU36577", null))
                .build())
            .setComment("TestComment")
            .build();
    }

    private UpdateInboundResponse getExpectedResponse() {
        return new UpdateInboundResponse(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setFulfillmentId(PARTNER_ID)
            .setPartnerId(PARTNER_ID)
            .build());
    }

}
