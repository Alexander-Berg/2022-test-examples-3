package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import org.hamcrest.collection.IsCollectionWithSize;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentRecord;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class DeliveryJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final String EXPECTED_STR =
            "{\"paymentOptions\":[" +
                    "{\"paymentType\":\"POSTPAID\",\"paymentMethod\":\"CARD_ON_DELIVERY\"}" +
                    ",{\"paymentType\":\"PREPAID\",\"paymentMethod\":\"BANK_CARD\"}" +
                    "]}";

    private static final String FULL_EXPECTED_STR =
            "{\"paymentOptions\":[" +
                    "{\"paymentType\":\"PREPAID\",\"paymentMethod\":\"BANK_CARD\",\"deliveryPrice\":0," +
                    "\"promoType\":\"FREE_PICKUP\"}" +
                    ",{\"paymentType\":\"POSTPAID\",\"paymentMethod\":\"CARD_ON_DELIVERY\",\"deliveryPrice\":99}" +
                    "]}";

    @Test
    public void shouldSerializePaymentOptions() throws IOException, JSONException {
        Delivery delivery = new Delivery();
        delivery.setPaymentRecords(
                new HashSet<>(Arrays.asList(new PaymentRecord(PaymentMethod.BANK_CARD),
                        new PaymentRecord(PaymentMethod.CARD_ON_DELIVERY))
                ));
        String body = write(delivery);
        JSONAssert.assertEquals(EXPECTED_STR, body, false);
    }

    @Test
    public void shouldSerializeFullPaymentOptions() throws IOException, JSONException {
        Delivery delivery = new Delivery();
        delivery.setPaymentRecords(
                new HashSet<>(Arrays.asList(new PaymentRecord(PaymentMethod.BANK_CARD, BigDecimal.ZERO,
                                PromoType.FREE_PICKUP),
                        new PaymentRecord(PaymentMethod.CARD_ON_DELIVERY, BigDecimal.valueOf(99L)))
                ));
        String body = write(delivery);
        JSONAssert.assertEquals(FULL_EXPECTED_STR, body, false);
    }

    @Test
    public void testSerializeFirstItemOfShipmentsAsShipment() throws JSONException, IOException {
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        Parcel shipment1 = new Parcel();
        shipment1.setId(10L);
        shipment1.addTrack(new Track("iddqd1", 123L));
        shipment1.addParcelItem(new ParcelItem(1L, 1));

        Parcel shipment2 = new Parcel();
        shipment2.setId(11L);
        shipment2.addTrack(new Track("iddqd2", 123L));

        delivery.setParcels(Arrays.asList(shipment1, shipment2));

        String body = write(delivery);
        JSONAssert.assertEquals(
                "{\"shipments\":[{\"id\":10,\"tracks\":[{\"trackCode\":\"iddqd1\"," +
                        "\"deliveryServiceId\" : 123}]," +
                        "\"items\":[{\"itemId\":1,\"count\":1}]},{\"id\":11," +
                        "\"tracks\":[{\"trackCode\":\"iddqd2\",\"deliveryServiceId\":123}]}]," +
                        "\"shipment\": {\"id\": 10}," +
                        "\"tracks\":[{\"trackCode\":\"iddqd1\",\"deliveryServiceId\" : 123}]}",
                body,
                false
        );
    }

    @Test
    public void serialize() throws Exception {
        Delivery delivery = EntityHelper.getDelivery();

        String json = write(delivery);
        System.out.println(json);

        checkJson(json, "$." + Names.ID, "id");
        checkJson(json, "$." + Names.Delivery.SHOP_DELIVERY_ID, "shopDeliveryId");
        checkJson(json, "$." + Names.Delivery.HASH, "hash");
        checkJson(json, "$." + Names.Delivery.DELIVERY_OPTION_ID, "deliveryOptionId");
        checkJson(json, "$." + Names.Delivery.TYPE, "DELIVERY");
        checkJson(json, "$." + Names.Delivery.SERVICE_NAME, "serviceName");
        checkJson(json, "$." + Names.Delivery.PRICE, 12.34);
        checkJson(json, "$." + Names.Delivery.BUYER_PRICE, 34.56);
        checkJson(json, "$." + Names.Delivery.DELIVERY_DATES, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Delivery.VALIDATED_DELIVERY_DATES,
                JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Delivery.DELIVERY_INTERVALS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$." + Names.Delivery.REGION_ID, 2);
        checkJson(json, "$." + AddressType.UNKNOWN.getJsonFieldName(),
                JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + AddressType.BUYER.getJsonFieldName(),
                JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + AddressType.SHOP.getJsonFieldName(), JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Delivery.OUTLET_ID, 123);
        checkJson(json, "$." + Names.Delivery.OUTLET_CODE, "outletCode");
        checkJson(json, "$." + Names.Delivery.OUTLET, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Delivery.OUTLETS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Delivery.OUTLETS, hasSize(1));
        checkJson(json, "$." + Names.Order.PAYMENT_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Order.PAYMENT_OPTIONS, hasSize(1));
        checkJson(json, "$." + Names.Order.PAYMENT_OPTIONS + "[0]" + "." + Names.Order.PAYMENT_METHOD,
                PaymentMethod.YANDEX.name());
        checkJson(json, "$." + Names.Order.PAYMENT_OPTIONS + "[0]" + "." + Names.Order.PAYMENT_TYPE,
                PaymentType.PREPAID.name());
        checkJson(json, "$." + Names.Delivery.HIDDEN_PAYMENT_OPTIONS,
                JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Delivery.HIDDEN_PAYMENT_OPTIONS, hasSize(1));
        checkJson(json, "$." + Names.Delivery.VALIDATION_RESULTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Delivery.VALIDATION_RESULTS, hasSize(1));
        checkJson(json, "$." + Names.Delivery.VALID_FEATURES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Delivery.VALID_FEATURES, IsCollectionWithSize.hasSize(1));
        checkJsonMatcher(json, "$." + Names.Delivery.VALID_FEATURES, hasItem(MarketplaceFeature.PLAINCPA.name()));
        checkJson(json, "$." + Names.Delivery.DELIVERY_SERVICE_ID, 99);
        checkJson(json, "$." + Names.Delivery.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.SHOP.name());
        checkJson(json, "$." + Names.Delivery.SHIPMENTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Delivery.SHIPMENTS, hasSize(1));
        checkJson(json, "$." + Names.Delivery.TRACKS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$." + Names.Delivery.VAT, VatType.VAT_18.name());
        checkJson(json, "$." + Names.Delivery.USER_RECEIVED, true);
        checkJson(json, "$." + Names.Delivery.IS_TRYING_AVAILABLE, true);
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void deserialize() throws Exception {
        String json = "{\n" +
                "  \"id\": \"id\",\n" +
                "  \"shopDeliveryId\": \"shopDeliveryId\",\n" +
                "  \"hash\": \"hash\",\n" +
                "  \"deliveryOptionId\": \"deliveryOptionId\",\n" +
                "  \"type\": \"DELIVERY\",\n" +
                "  \"serviceName\": \"serviceName\",\n" +
                "  \"price\": 12.34,\n" +
                "  \"buyerPrice\": 34.56,\n" +
                "  \"dates\": {\n" +
                "    \"fromDate\": \"29-10-2017\",\n" +
                "    \"toDate\": \"31-10-2017\"\n" +
                "  },\n" +
                "  \"validatedDates\": {\n" +
                "    \"fromDate\": \"29-10-2017\",\n" +
                "    \"toDate\": \"31-10-2017\"\n" +
                "  },\n" +
                "  \"deliveryIntervals\": [],\n" +
                "  \"regionId\": 2,\n" +
                "  \"address\": {\n" +
                "    \"country\": \"country\",\n" +
                "    \"postcode\": \"postcode\",\n" +
                "    \"city\": \"city\",\n" +
                "    \"subway\": \"subway\",\n" +
                "    \"street\": \"street\",\n" +
                "    \"house\": \"house\",\n" +
                "    \"block\": \"block\",\n" +
                "    \"entrance\": \"entrance\",\n" +
                "    \"entryphone\": \"entryPhone\",\n" +
                "    \"floor\": \"floor\",\n" +
                "    \"apartment\": \"apartment\",\n" +
                "    \"recipient\": \"recipient\",\n" +
                "    \"phone\": \"phone\",\n" +
                "    \"language\": \"ENG\"\n" +
                "  },\n" +
                "  \"buyerAddress\": {\n" +
                "    \"country\": \"country\",\n" +
                "    \"postcode\": \"postcode\",\n" +
                "    \"city\": \"city\",\n" +
                "    \"subway\": \"subway\",\n" +
                "    \"street\": \"street\",\n" +
                "    \"house\": \"house\",\n" +
                "    \"block\": \"block\",\n" +
                "    \"entrance\": \"entrance\",\n" +
                "    \"entryphone\": \"entryPhone\",\n" +
                "    \"floor\": \"floor\",\n" +
                "    \"apartment\": \"apartment\",\n" +
                "    \"recipient\": \"recipient\",\n" +
                "    \"phone\": \"phone\",\n" +
                "    \"language\": \"ENG\"\n" +
                "  },\n" +
                "  \"shopAddress\": {\n" +
                "    \"country\": \"country\",\n" +
                "    \"postcode\": \"postcode\",\n" +
                "    \"city\": \"city\",\n" +
                "    \"subway\": \"subway\",\n" +
                "    \"street\": \"street\",\n" +
                "    \"house\": \"house\",\n" +
                "    \"block\": \"block\",\n" +
                "    \"entrance\": \"entrance\",\n" +
                "    \"entryphone\": \"entryPhone\",\n" +
                "    \"floor\": \"floor\",\n" +
                "    \"apartment\": \"apartment\",\n" +
                "    \"recipient\": \"recipient\",\n" +
                "    \"phone\": \"phone\",\n" +
                "    \"language\": \"ENG\"\n" +
                "  },\n" +
                "  \"outletId\": 123,\n" +
                "  \"outletCode\": \"outletCode\",\n" +
                "  \"outlet\": {\n" +
                "    \"id\": 123,\n" +
                "    \"name\": \"name\",\n" +
                "    \"regionId\": 456,\n" +
                "    \"city\": \"city\",\n" +
                "    \"street\": \"street\",\n" +
                "    \"km\": \"km\",\n" +
                "    \"house\": \"HOUSE\",\n" +
                "    \"building\": \"building\",\n" +
                "    \"estate\": \"estate\",\n" +
                "    \"block\": \"block\",\n" +
                "    \"gps\": \"gps\",\n" +
                "    \"notes\": \"notes\",\n" +
                "    \"phones\": [\n" +
                "      {\n" +
                "        \"countryCode\": \"+7\",\n" +
                "        \"cityCode\": \"495\",\n" +
                "        \"number\": \"2234562\",\n" +
                "        \"extNumber\": \"albatros\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"schedule\": [\n" +
                "      {\n" +
                "        \"dayFrom\": 0,\n" +
                "        \"timeFrom\": \"08:00\",\n" +
                "        \"dayTo\": 5,\n" +
                "        \"timeTo\": \"16:00\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"deliveryService\": \"MARKET_INPOST\"\n" +
                "  },\n" +
                "  \"outlets\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"name\": \"name\",\n" +
                "      \"regionId\": 456,\n" +
                "      \"city\": \"city\",\n" +
                "      \"street\": \"street\",\n" +
                "      \"km\": \"km\",\n" +
                "      \"house\": \"HOUSE\",\n" +
                "      \"building\": \"building\",\n" +
                "      \"estate\": \"estate\",\n" +
                "      \"block\": \"block\",\n" +
                "      \"gps\": \"gps\",\n" +
                "      \"notes\": \"notes\",\n" +
                "      \"phones\": [\n" +
                "        {\n" +
                "          \"countryCode\": \"+7\",\n" +
                "          \"cityCode\": \"495\",\n" +
                "          \"number\": \"2234562\",\n" +
                "          \"extNumber\": \"albatros\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"schedule\": [\n" +
                "        {\n" +
                "          \"dayFrom\": 0,\n" +
                "          \"timeFrom\": \"08:00\",\n" +
                "          \"dayTo\": 5,\n" +
                "          \"timeTo\": \"16:00\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"deliveryService\": \"MARKET_INPOST\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"paymentOptions\": [\n" +
                "    {\n" +
                "      \"paymentType\": \"PREPAID\",\n" +
                "      \"paymentMethod\": \"YANDEX\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"hiddenPaymentOptions\": [\n" +
                "    {\n" +
                "      \"paymentType\": \"POSTPAID\",\n" +
                "      \"paymentMethod\": \"CASH_ON_DELIVERY\",\n" +
                "      \"hiddenReason\": \"MULTICART\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"validationErrors\": [\n" +
                "    {\n" +
                "      \"type\": \"basic\",\n" +
                "      \"code\": \"code\",\n" +
                "      \"severity\": \"ERROR\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"validFeatures\": [\n" +
                "    \"PLAINCPA\"\n" +
                "  ],\n" +
                "  \"deliveryServiceId\": 99,\n" +
                "  \"deliveryPartnerType\": \"SHOP\",\n" +
                "  \"parcels\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"shopShipmentId\": 345,\n" +
                "      \"weight\": 567,\n" +
                "      \"height\": 789,\n" +
                "      \"depth\": 901,\n" +
                "      \"status\": \"NEW\",\n" +
                "      \"labelURL\": \"labelUrl\",\n" +
                "      \"tracks\": [\n" +
                "        {\n" +
                "          \"id\": 123,\n" +
                "          \"trackCode\": \"code\",\n" +
                "          \"deliveryServiceId\": 123,\n" +
                "          \"trackerId\": 456,\n" +
                "          \"status\": \"STARTED\",\n" +
                "          \"checkpoints\": [\n" +
                "            {\n" +
                "              \"id\": 123,\n" +
                "              \"trackerCheckpointId\": 456,\n" +
                "              \"country\": \"country\",\n" +
                "              \"city\": \"city\",\n" +
                "              \"location\": \"location\",\n" +
                "              \"message\": \"message\",\n" +
                "              \"status\": \"DELIVERED\",\n" +
                "              \"zipCode\": \"zipCode\",\n" +
                "              \"date\": \"10-07-1973 03:11:51\",\n" +
                "              \"deliveryStatus\": 123,\n" +
                "              \"translatedCountry\": \"страна\",\n" +
                "              \"translatedCity\": \"город\",\n" +
                "              \"translatedLocation\": \"местоположение\",\n" +
                "              \"translatedMessage\": \"сообщение\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"creationDate\": \"02-06-72389 17:37:02\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"itemId\": 123,\n" +
                "          \"count\": 234\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"tracks\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"trackCode\": \"code\",\n" +
                "      \"deliveryServiceId\": 123,\n" +
                "      \"trackerId\": 456,\n" +
                "      \"status\": \"STARTED\",\n" +
                "      \"checkpoints\": [\n" +
                "        {\n" +
                "          \"id\": 123,\n" +
                "          \"trackerCheckpointId\": 456,\n" +
                "          \"country\": \"country\",\n" +
                "          \"city\": \"city\",\n" +
                "          \"location\": \"location\",\n" +
                "          \"message\": \"message\",\n" +
                "          \"status\": \"DELIVERED\",\n" +
                "          \"zipCode\": \"zipCode\",\n" +
                "          \"date\": \"10-07-1973 03:11:51\",\n" +
                "          \"deliveryStatus\": 123,\n" +
                "          \"translatedCountry\": \"страна\",\n" +
                "          \"translatedCity\": \"город\",\n" +
                "          \"translatedLocation\": \"местоположение\",\n" +
                "          \"translatedMessage\": \"сообщение\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"creationDate\": \"02-06-72389 17:37:02\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"vat\": \"VAT_18\",\n" +
                "  \"userReceived\": true,\n" +
                "  \"isTryingAvailable\": true\n" +
                "}";

        Delivery delivery = read(Delivery.class, json);

        Assertions.assertEquals("id", delivery.getId());
        Assertions.assertEquals("shopDeliveryId", delivery.getShopDeliveryId());
        Assertions.assertEquals("hash", delivery.getHash());
        Assertions.assertEquals("deliveryOptionId", delivery.getDeliveryOptionId());
        Assertions.assertEquals(DeliveryType.DELIVERY, delivery.getType());
        Assertions.assertEquals("serviceName", delivery.getServiceName());
        Assertions.assertEquals(new BigDecimal("12.34"), delivery.getPrice());
        Assertions.assertEquals(new BigDecimal("34.56"), delivery.getBuyerPrice());
        Calendar from = Calendar.getInstance();
        from.set(2017, Calendar.OCTOBER, 29, 0, 0, 0);
        from.set(Calendar.MILLISECOND, 0);
        Calendar to = Calendar.getInstance();
        to.set(2017, Calendar.OCTOBER, 31, 0, 0, 0);
        to.set(Calendar.MILLISECOND, 0);
        Assertions.assertEquals(new DeliveryDates(from.getTime(), to.getTime()), delivery.getDeliveryDates());
        Assertions.assertEquals(new DeliveryDates(from.getTime(), to.getTime()), delivery.getValidatedDeliveryDates());
        Assertions.assertEquals(2L, delivery.getRegionId().longValue());
        Assertions.assertNotNull(delivery.getAddress());
        Assertions.assertEquals(AddressType.UNKNOWN, delivery.getAddress().getType());
        Assertions.assertNotNull(delivery.getBuyerAddress());
        Assertions.assertEquals(AddressType.BUYER, delivery.getBuyerAddress().getType());
        Assertions.assertNotNull(delivery.getShopAddress());
        Assertions.assertEquals(AddressType.SHOP, delivery.getShopAddress().getType());
        Assertions.assertEquals(123L, delivery.getOutletId().longValue());
        Assertions.assertEquals("outletCode", delivery.getOutletCode());
        Assertions.assertNotNull(delivery.getOutlet());
        Assertions.assertNotNull(delivery.getOutlets());
        Assertions.assertNotNull(delivery.getPaymentOptions());
        assertThat(delivery.getPaymentOptions(), hasSize(1));
        assertThat(delivery.getPaymentOptions(), hasItem(PaymentMethod.YANDEX));
        assertThat(delivery.getHiddenPaymentOptions(), hasSize(1));
        assertThat(delivery.getValidationErrors(), hasSize(1));
        assertThat(delivery.getValidFeatures(), hasSize(1));
        assertThat(delivery.getValidFeatures(), hasItem(MarketplaceFeature.PLAINCPA));
        Assertions.assertEquals(99L, delivery.getDeliveryServiceId().longValue());
        Assertions.assertEquals(DeliveryPartnerType.SHOP, delivery.getDeliveryPartnerType());
        assertThat(delivery.getParcels(), hasSize(1));
        Assertions.assertEquals(VatType.VAT_18, delivery.getVat());
        Assertions.assertTrue(delivery.getUserReceived());
        Assertions.assertTrue(delivery.getTryingAvailable());
    }

}
