package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;
import ru.yandex.market.common.report.model.specs.InternalSpec;
import ru.yandex.market.common.report.model.specs.Specs;
import ru.yandex.market.common.report.model.specs.UsedParam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.test.providers.RegionProvider.getManufacturerCountries;
import static ru.yandex.market.checkout.test.providers.ResaleSpecsProvider.getResaleSpecs;

public class OrderItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final String JSON_FOR_DESERIALIZATION = "{\n" +
            "  \"id\": 123,\n" +
            "  \"feedId\": 234,\n" +
            "  \"offerId\": \"345\",\n" +
            "  \"wareMd5\": \"456\",\n" +
            "  \"categoryId\": 567,\n" +
            "  \"offerName\": \"offerName\",\n" +
            "  \"price\": 67.89,\n" +
            "  \"count\": 789,\n" +
            "  \"delivery\": true,\n" +
            "  \"isPromotedByVendor\": true,\n" +
            "  \"promoKey\": \"promoKey\",\n" +
            "  \"validationErrors\": [\n" +
            "    {\n" +
            "      \"type\": \"basic\",\n" +
            "      \"code\": \"code\",\n" +
            "      \"severity\": \"ERROR\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"modelId\": 987,\n" +
            "  \"description\": \"description\",\n" +
            "  \"pictures\": [\n" +
            "    {\n" +
            "      \"url\": \"//avatars.mds.yandex" +
            ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\n" +
            "      \"width\": 12,\n" +
            "      \"height\": 34,\n" +
            "      \"containerWidth\": 78,\n" +
            "      \"containerHeight\": 56\n" +
            "    }\n" +
            "  ],\n" +
            "  \"buyerPrice\": 87.6,\n" +
            "  \"fee\": 7.65,\n" +
            "  \"feeInt\": 765,\n" +
            "  \"feeSum\": 6.54,\n" +
            "  \"pp\": 1000,\n" +
            "  \"showUid\": \"showUid\",\n" +
            "  \"realShowUid\": \"realShowUid\",\n" +
            "  \"cartShowUid\": \"cartShowUid\",\n" +
            "  \"showInfo\": \"showInfo\",\n" +
            "  \"cartShowInfo\": \"cartShowInfo\",\n" +
            "  \"shopUrl\": \"shopUrl\",\n" +
            "  \"kind2Params\": [\n" +
            "    {\n" +
            "      \"type\": \"type\",\n" +
            "      \"subType\": \"subType\",\n" +
            "      \"name\": \"name\",\n" +
            "      \"value\": \"value\",\n" +
            "      \"unit\": \"unit\",\n" +
            "      \"code\": \"code\",\n" +
            "      \"specifiedForOffer\": true,\n" +
            "      \"units\": [\n" +
            "        {\n" +
            "          \"values\": [\n" +
            "            \"a\",\n" +
            "            \"b\",\n" +
            "            \"c\"\n" +
            "          ],\n" +
            "          \"shopValues\": [\n" +
            "            \"d\",\n" +
            "            \"e\",\n" +
            "            \"f\"\n" +
            "          ],\n" +
            "          \"unitId\": \"unitId\",\n" +
            "          \"defaultUnit\": true\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"vat\": \"VAT_18\",\n" +
            "  \"subsidy\": 5.43,\n" +
            "  \"buyerDiscount\": 4.32,\n" +
            "  \"buyerPriceBeforeDiscount\": 3.21,\n" +
            "  \"buyerSubsidy\": 2.1,\n" +
            "  \"promos\": [\n" +
            "    {\n" +
            "      \"type\": \"MARKET_COUPON\",\n" +
            "      \"subsidy\": 5.43\n" +
            "    }\n" +
            "  ],\n" +
            "  \"changes\": [\n" +
            "    \"COUNT\"\n" +
            "  ],\n" +
            "  \"sku\": \"100131945205\",\n" +
            "  \"shopSku\": \"shopSku\",\n" +
            "  \"msku\": 100131945205, \n" +
            "  \"supplierId\": 111,\n" +
            "  \"vendorId\": 10545982,\n" +
            "  \"supplierDescription\": \"some-offer-description-from-supplier\"," +
            "  \"manufacturerCountries\": [" +
            "        {\n" +
            "                \"entity\": \"region\",\n" +
            "                \"id\": \"213\",\n" +
            "                \"name\": \"Москва\",\n" +
            "                \"lingua\": {\n" +
            "                    \"name\": {\n" +
            "                        \"genitive\": \"Москвы\",\n" +
            "                        \"preposition\": \"в\",\n" +
            "                        \"prepositional\": \"Москве\"\n" +
            "                    }\n" +
            "                }\n" +
            "        },\n" +
            "        {\n" +
            "          \"entity\":\"region\",\n" +
            "          \"id\":65,\n" +
            "          \"name\":\"Новосибирск\",\n" +
            "          \"lingua\":\n" +
            "            {\n" +
            "              \"name\":\n" +
            "                {\n" +
            "                  \"genitive\":\"Новосибирска\",\n" +
            "                  \"preposition\":\"в\",\n" +
            "                  \"prepositional\":\"Новосибирске\",\n" +
            "                  \"accusative\":\"Новосибирск\"\n" +
            "                }\n" +
            "            }\n" +
            "        }" +
            "   ]," +
            "  \"supplierWorkSchedule\": \"some-work-schedule-from-supplier\"," +
            "  \"specsObj\": {\n" +
            "          \"internal\": [\n" +
            "            {\n" +
            "              \"type\": \"spec\",\n" +
            "              \"value\": \"medicine\",\n" +
            "              \"usedParams\": []\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"spec\",\n" +
            "              \"value\": \"vidal\",\n" +
            "              \"usedParams\": [{\"id\": 24343365, \"name\": \"J05AX13\" }]\n" +
            "            }\n" +
            "     ]\n" +
            "   }," +
            "   \"isResale\": false,\n" +
            "    \"resaleSpecs\": {\n" +
            "            \"condition\": { \n" +
            "               \"value\": \"resale_perfect\",\n" +
            "               \"text\": \"Хорошее\"\n" +
            "              },\n" +
            "               \"reason\": {\n" +
            "               \"value\": \"1\",\n" +
            "               \"text\": \"Б/У\"\n" +
            "            }\n" +
            "        }" +
            "}";

    @Test
    public void serializeInner() throws Exception {
        OrderItem orderItem = EntityHelper.getOrderItem();

        String json = write(orderItem);
        System.out.println(json);

        checkJson(json, "$." + Names.OrderItem.ID, 123);
        checkJson(json, "$." + Names.OfferItem.FEED_ID, 234);
        checkJson(json, "$." + Names.OfferItem.OFFER_ID, "345");
        checkJson(json, "$." + Names.OfferItem.WARE_MD5, "456");
        checkJson(json, "$." + Names.OfferItem.CATEGORY_ID, 567);
        checkJson(json, "$." + Names.OfferItem.OFFER_NAME, "offerName");
        checkJson(json, "$." + Names.OfferItem.PRICE, 67.89);
        checkJson(json, "$." + Names.OfferItem.COUNT, 789);
        checkJson(json, "$." + Names.OfferItem.DELIVERY, true);
        checkJson(json, "$." + Names.OrderItem.PROMO_KEY, "promoKey");
        checkJson(json, "$." + Names.Validation.VALIDATION_ERRORS + "[0].code", "code");
        checkJson(json, "$." + Names.Validation.VALIDATION_ERRORS + "[0].severity", ValidationResult.Severity.ERROR
                .name());
        checkJson(json, "$." + Names.OrderItem.MODEL_ID, 987);
        checkJson(json, "$." + Names.OrderItem.DESCRIPTION, "description");
        checkJson(json, "$." + Names.OrderItem.PICTURES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$." + Names.OrderItem.BUYER_PRICE, 87.6);
        checkJson(json, "$." + Names.OrderItem.FEE, 7.65);
        checkJson(json, "$." + Names.OrderItem.FEE_INT, 765);
        checkJson(json, "$." + Names.OrderItem.FEE_SUM, 6.54);
        checkJson(json, "$." + Names.OrderItem.SHOW_UID, "showUid");
        checkJson(json, "$." + Names.OrderItem.REAL_SHOW_UID, "realShowUid");
        checkJson(json, "$." + Names.OrderItem.SHOW_INFO, "showInfo");
        checkJson(json, "$." + Names.OrderItem.SHOP_URL, "shopUrl");
        checkJson(json, "$." + Names.OrderItem.KIND2_PARAMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.OrderItem.KIND2_PARAMS, hasSize(1));
        checkJson(json, "$." + Names.OrderItem.VAT, VatType.VAT_18.name());
        checkJson(json, "$." + Names.OfferItem.SUBSIDY, 5.43);
        checkJson(json, "$." + Names.OfferItem.BUYER_DISCOUNT, 4.32);
        checkJson(json, "$." + Names.OfferItem.BUYER_PRICE_BEFORE_DISCOUNT, 91.92);
        checkJson(json, "$." + Names.OfferItem.BUYER_SUBSIDY, 2.1);
        checkJson(json, "$." + Names.OrderItem.PROMOS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS, hasSize(2));
        checkJsonMatcher(json, "$." + Names.OrderItem.CHANGES, hasItem(ItemChange.COUNT.name()));
        checkJson(json, "$." + Names.OrderItem.SKU, EntityHelper.SKU);
        checkJson(json, "$." + Names.OrderItem.SHOP_SKU, "shopSku");
        checkJson(json, "$." + Names.OrderItem.MSKU, EntityHelper.SKU);
        checkJson(json, "$." + Names.OrderItem.FULFILMENT_SHOP_ID, 111);
        checkJson(json, "$." + Names.OrderItem.SUPPLIER_ID, 111);
        checkJson(json, "$." + Names.OrderItem.VENDOR_ID, 10545982);
        checkJson(json, "$." + Names.OrderItem.IS_RESALE, false);
        checkJsonNotExist(json, "$." + Names.OrderItem.WIDTH);
        checkJsonNotExist(json, "$." + Names.OrderItem.HEIGHT);
        checkJsonNotExist(json, "$." + Names.OrderItem.DEPTH);
        checkJsonNotExist(json, "$." + Names.OrderItem.RESALE_SPECS);
    }

    @Test
    public void serializeSpecs() throws Exception {
        OrderItem orderItem = EntityHelper.getOrderItem();
        orderItem.setMedicalSpecsInternal(
                new Specs(Set.of(
                        new InternalSpec("vidal", List.of(new UsedParam("J05AX13")))
                ))
        );
        String json = write(orderItem);

        JSONObject specsObj = new JSONObject(json).getJSONObject("specsObj");
        JSONArray internalSpecsData = specsObj.getJSONArray("internal");
        Assertions.assertEquals(internalSpecsData.length(), 1);

        JSONObject internalSpecObj = internalSpecsData.getJSONObject(0);
        String internalSpecJson = internalSpecObj.toString();
        checkJson(internalSpecJson, "$." + "type", "spec");
        checkJson(internalSpecJson, "$." + "value", "vidal");

        JSONArray usedParamsData = internalSpecObj.getJSONArray("usedParams");
        Assertions.assertEquals(usedParamsData.length(), 1);
        checkJson(usedParamsData.get(0).toString(), "$." + "name", "J05AX13");
    }

    @Test
    public void serializeResaleSpecs() throws Exception {
        OrderItem orderItem = EntityHelper.getOrderItem();
        orderItem.setResaleSpecs(getResaleSpecs(null));

        String json = write(orderItem);
        JSONObject rs = new JSONObject(json).getJSONObject("resaleSpecs");
        JSONObject rsCondition = rs.getJSONObject("condition");
        JSONObject rsReason = rs.getJSONObject("reason");

        String rsConditionJson = rsCondition.toString();
        String rsReasonJson = rsReason.toString();
        checkJson(rsConditionJson, "$." + "text", "ct");
        checkJson(rsConditionJson, "$." + "text", "ct");
        checkJson(rsReasonJson, "$." + "text", "rt");
        checkJson(rsReasonJson, "$." + "value", "rv");
    }

    @Test
    public void serializeSupplierInformation() throws Exception {
        OrderItem orderItem = EntityHelper.getOrderItem();
        orderItem.setSupplierDescription("some-offer-description-from-supplier");
        orderItem.setSupplierWorkSchedule("some-work-schedule-from-supplier");
        orderItem.setManufacturerCountries(getManufacturerCountries());

        String json = write(orderItem);

        checkJson(json, "$." + Names.OfferItem.SUPPLIER_DESCRIPTION, "some-offer-description-from-supplier");
        checkJson(json, "$." + Names.OfferItem.SUPPLIER_WORK_SCHEDULE, "some-work-schedule-from-supplier");
        checkJson(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES, hasSize(2));
        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES + "[*].id", containsInAnyOrder(65, 213));
        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES + "[*].name", containsInAnyOrder(
                "Новосибирск", "Москва"));
    }

    @Test
    public void deserializeInnerBackwardsCompatible() throws Exception {
        performDeserializationTest(JSON_FOR_DESERIALIZATION.replace("supplierId", "fulfilmentShopId"));
    }

    @Test
    public void deserializeInner() throws Exception {
        performDeserializationTest(JSON_FOR_DESERIALIZATION);
    }

    private void performDeserializationTest(String json) throws java.io.IOException {
        OrderItem orderItem = read(OrderItem.class, json);

        Assertions.assertEquals(123L, orderItem.getId().longValue());
        Assertions.assertEquals(234L, orderItem.getFeedId().longValue());
        Assertions.assertEquals("345", orderItem.getOfferId());
        Assertions.assertEquals("456", orderItem.getWareMd5());
        Assertions.assertEquals(567, orderItem.getCategoryId().intValue());
        Assertions.assertEquals("offerName", orderItem.getOfferName());
        Assertions.assertEquals(new BigDecimal("67.89"), orderItem.getPrice());
        Assertions.assertEquals(789, orderItem.getCount().intValue());
        Assertions.assertTrue(orderItem.getDelivery());
        Assertions.assertEquals("promoKey", orderItem.getPromoKey());
        Assertions.assertNotNull(orderItem.getPictures());
        assertThat(orderItem.getPictures(), hasSize(1));
        Assertions.assertEquals(new BigDecimal("87.6"), orderItem.getBuyerPrice());
        Assertions.assertEquals(new BigDecimal("7.65"), orderItem.getFee());
        Assertions.assertEquals(765, orderItem.getFeeInt().intValue());
        Assertions.assertEquals(new BigDecimal("6.54"), orderItem.getFeeSum());
        Assertions.assertEquals(1000, orderItem.getPp().intValue());
        Assertions.assertEquals("showUid", orderItem.getShowUid());
        Assertions.assertEquals("realShowUid", orderItem.getRealShowUid());
        Assertions.assertEquals("cartShowUid", orderItem.getCartShowUid());
        Assertions.assertEquals("cartShowInfo", orderItem.getCartShowInfo());
        Assertions.assertEquals("showInfo", orderItem.getShowInfo());
        Assertions.assertEquals("shopUrl", orderItem.getShopUrl());
        assertThat(orderItem.getKind2Parameters(), notNullValue());
        assertThat(orderItem.getKind2Parameters(), hasSize(1));
        Assertions.assertEquals(VatType.VAT_18, orderItem.getVat());
        Assertions.assertEquals(new BigDecimal("5.43"), orderItem.getPrices().getSubsidy());
        Assertions.assertEquals(new BigDecimal("4.32"), orderItem.getPrices().getBuyerDiscount());
        Assertions.assertEquals(new BigDecimal("2.1"), orderItem.getPrices().getBuyerSubsidy());
        ItemPromo itemPromo = orderItem.getPromos().iterator().next();
        Assertions.assertEquals(marketCouponPromo(), itemPromo.getPromoDefinition());
        Assertions.assertEquals(new BigDecimal("5.43"), itemPromo.getSubsidy());
        assertThat(orderItem.getChanges(), hasItem(ItemChange.COUNT));
        Assertions.assertEquals("100131945205", orderItem.getSku());
        Assertions.assertEquals("shopSku", orderItem.getShopSku());
        Assertions.assertEquals(100131945205L, (long) orderItem.getMsku());
        Assertions.assertEquals(111L, orderItem.getSupplierId().longValue());
        Assertions.assertNull(orderItem.getWidth(), "width");
        Assertions.assertNull(orderItem.getHeight(), "height");
        Assertions.assertNull(orderItem.getDepth(), "depth");
        Assertions.assertEquals("some-offer-description-from-supplier", orderItem.getSupplierDescription());
        assertThat(
                orderItem.getManufacturerCountries().stream().map(c -> c.getId().get()).collect(Collectors.toList()),
                containsInAnyOrder(65, 213)
        );
        assertThat(
                orderItem.getManufacturerCountries().stream().map(c -> c.getName().get()).collect(Collectors.toList()),
                containsInAnyOrder("Москва", "Новосибирск")
        );
        Assertions.assertEquals("some-work-schedule-from-supplier", orderItem.getSupplierWorkSchedule());
        Assertions.assertEquals(orderItem.getMedicalSpecsInternal(),
                new Specs(Set.of(
                        new InternalSpec("vidal", List.of(new UsedParam("J05AX13"))),
                        new InternalSpec("medicine")
                ))
        );
        ResaleSpecs resaleSpecs = new ResaleSpecs();
        resaleSpecs.setConditionValue("resale_perfect");
        resaleSpecs.setConditionText("Хорошее");
        resaleSpecs.setReasonValue("1");
        resaleSpecs.setReasonText("Б/У");
        Assertions.assertEquals(resaleSpecs, orderItem.getResaleSpecs());
    }

    @Test
    public void serializeDimensions() throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setWidth(11L);
        orderItem.setHeight(22L);
        orderItem.setDepth(33L);

        String json = write(orderItem);
        System.out.println(json);

        checkJson(json, "$." + Names.OrderItem.WIDTH, 11L);
        checkJson(json, "$." + Names.OrderItem.HEIGHT, 22L);
        checkJson(json, "$." + Names.OrderItem.DEPTH, 33L);
    }

    @Test
    public void serializeError() throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setWidth(0L);
        orderItem.setHeight((Long) null);

        String json = write(orderItem);
        System.out.println(json);

        checkJson(json, "$." + Names.OrderItem.WIDTH, "0");
        checkJsonNotExist(json, "$." + Names.OrderItem.HEIGHT);
        checkJson(json, "$." + Names.OrderItem.WIDTH, 0L);
        checkJsonNotExist(json, "$." + Names.OrderItem.HEIGHT);
    }

    @Test
    public void deserializeDimensions() throws Exception {
        String json = "{\"width\": 11, \n" +
                "\"height\": 22,\n" +
                "\"depth\": 33}";

        OrderItem orderItem = read(OrderItem.class, json);

        Assertions.assertEquals(orderItem.getWidth(), (Long) 11L, "width");
        Assertions.assertEquals(orderItem.getHeight(), (Long) 22L, "height");
        Assertions.assertEquals(orderItem.getDepth(), (Long) 33L, "depth");
    }

    @Test
    public void deserializeStepCount() throws Exception {
        String json = "{\"countStep\": 10}";
        OrderItem orderItem = read(OrderItem.class, json);
        Assertions.assertEquals(10, orderItem.getCountStep(), "countStep");
    }

    @Test
    public void deserializeResaleSpecs() throws Exception {
        String json = "     {\n" +
                "            \"condition\": { \n" +
                "               \"value\": \"resale_perfect\",\n" +
                "               \"text\": \"Хорошее\"\n" +
                "              },\n" +
                "        \"reason\": {\n" +
                "               \"value\": \"1\",\n" +
                "               \"text\": \"Б/У\"\n" +
                "            }\n" +
                "           }";
        ResaleSpecs resaleSpecs = read(ResaleSpecs.class, json);

        Assertions.assertEquals("resale_perfect", resaleSpecs.getConditionValue());
        Assertions.assertEquals("Хорошее", resaleSpecs.getConditionText());
        Assertions.assertEquals("1", resaleSpecs.getReasonValue());
        Assertions.assertEquals("Б/У", resaleSpecs.getReasonText());
    }

}
