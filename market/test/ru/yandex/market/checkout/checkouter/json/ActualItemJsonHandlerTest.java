package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.test.providers.RegionProvider.getManufacturerCountries;

public class ActualItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serializeInner() throws Exception {
        ActualItem actualItem = new ActualItem();
        actualItem.setFeedId(123L);
        actualItem.setOfferId("offerId");
        actualItem.setWareMd5("wareMd5");
        actualItem.setFeedCategoryId("feedCategoryId");
        actualItem.setCategoryId(234);
        actualItem.setOfferName("offerName");
        actualItem.setPrice(new BigDecimal("34.5"));
        actualItem.setCount(456);
        actualItem.setDelivery(true);
        actualItem.setPromoKey("promokey");
        actualItem.setErrors(Collections.singleton(new ValidationResult("code", ValidationResult.Severity.ERROR)));

        actualItem.setBuyerRegionId(789L);
        actualItem.setShopId(890L);
        actualItem.setNoAuth(true);
        actualItem.setFromCache(true);
        actualItem.setCacheDate(EntityHelper.CREATION_DATE);
        actualItem.setShopCurrency(Currency.USD);
        actualItem.setOutletIds(Arrays.asList(1L, 2L, 3L));
        actualItem.setSupplierDescription("some-offer-description-from-supplier");
        actualItem.setSupplierWorkSchedule("some-work-schedule-from-supplier");
        actualItem.setManufacturerCountries(getManufacturerCountries());

        String json = write(actualItem);

        checkJson(json, "$." + Names.OfferItem.FEED_ID, 123);
        checkJson(json, "$." + Names.OfferItem.OFFER_ID, "offerId");
        checkJson(json, "$." + Names.OfferItem.OFFER_NAME, "offerName");
        checkJson(json, "$." + Names.OfferItem.WARE_MD5, "wareMd5");
        checkJson(json, "$." + Names.OfferItem.FEED_CATEGORY_ID, "feedCategoryId");
        checkJson(json, "$." + Names.OfferItem.CATEGORY_ID, 234);
        checkJson(json, "$." + Names.OfferItem.OFFER_NAME, "offerName");
        checkJson(json, "$." + Names.OfferItem.PRICE, 34.5);
        checkJson(json, "$." + Names.OfferItem.COUNT, 456);
        checkJson(json, "$." + Names.OfferItem.DELIVERY, true);
        checkJson(json, "$." + Names.OrderItem.PROMO_KEY, "promokey");
        checkJson(json, "$." + Names.Validation.VALIDATION_ERRORS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Validation.VALIDATION_ERRORS, hasSize(1));

        checkJson(json, "$." + Names.ActualItem.BUYER_REGION_ID, 789);
        checkJson(json, "$." + Names.ActualItem.SHOP_ID, 890);
        checkJson(json, "$." + Names.ActualItem.NO_AUTH, true);
        checkJson(json, "$." + Names.ActualItem.FROM_CACHE, true);
        checkJson(json, "$." + Names.ActualItem.CACHE_DATE, "11-11-2017 15:00:00");
        checkJson(json, "$." + Names.ActualItem.SHOP_CURRENCY, Currency.USD.name());
        checkJsonMatcher(json, "$." + Names.ActualItem.OUTLET_IDS, CoreMatchers.hasItems(1, 2, 3));

        checkJson(json, "$." + Names.OfferItem.SUPPLIER_DESCRIPTION, "some-offer-description-from-supplier");
        checkJson(json, "$." + Names.OfferItem.SUPPLIER_WORK_SCHEDULE, "some-work-schedule-from-supplier");
        checkJson(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES, JsonPathExpectationsHelper::assertValueIsArray);

        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES, hasSize(2));
        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES + "[*].id", containsInAnyOrder(65, 213));
        checkJsonMatcher(json, "$." + Names.OfferItem.MANUFACTURER_COUNTRIES + "[*].name", containsInAnyOrder(
                "Новосибирск", "Москва"));
    }

    @Test
    public void deserializeInner() throws Exception {
        String json =
                "{\"feedId\":123,\"offerId\":\"offerId\",\"wareMd5\":\"wareMd5\"," +
                        "\"feedCategoryId\":\"feedCategoryId\",\"categoryId\":234,\"offerName\":\"offerName\"," +
                        "\"price\":34.5,\"count\":456,\"delivery\":true,\"availability\":\"ON_DEMAND\"," +
                        "\"isPromotedByVendor\":true,\"promoKey\":\"promokey\"," +
                        "\"validationErrors\":[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}]," +
                        "\"buyerRegionId\":789,\"shopId\":890,\"noAuth\":true,\"fromCache\":true," +
                        "\"cacheDate\":\"11-11-2017 15:00:00\",\"shopCurrency\":\"USD\",\"supplierDescription\": " +
                        "\"some-offer-description-from-supplier\", \"manufacturerCountries\": [{ \"entity\": " +
                        "\"region\", \"id\": \"213\", \"name\": \"Москва\", \"lingua\": { \"name\": { \"genitive\": " +
                        "\"Москвы\", \"preposition\": \"в\", \"prepositional\": \"Москве\" }}}, { " +
                        "\"entity\":\"region\", \"id\":65, \"name\":\"Новосибирск\", \"lingua\": { \"name\": { " +
                        "\"genitive\":\"Новосибирска\", \"preposition\":\"в\", \"prepositional\": \"Новосибирске\", " +
                        "\"accusative\": \"Новосибирск\" }}}], \"supplierWorkSchedule\": " +
                        "\"some-work-schedule-from-supplier\", \"outletIds\":[1,2,3]}\n";

        ActualItem actualItem = read(ActualItem.class, json);

        Assertions.assertEquals(123L, actualItem.getFeedId().longValue());
        Assertions.assertEquals("offerId", actualItem.getOfferId());
        Assertions.assertEquals("wareMd5", actualItem.getWareMd5());
        Assertions.assertEquals("feedCategoryId", actualItem.getFeedCategoryId());
        Assertions.assertEquals(234L, actualItem.getCategoryId().longValue());
        Assertions.assertEquals("offerName", actualItem.getOfferName());
        Assertions.assertEquals(new BigDecimal("34.5"), actualItem.getPrice());
        Assertions.assertEquals(456, actualItem.getCount().intValue());
        Assertions.assertEquals(true, actualItem.getDelivery());
        Assertions.assertEquals("promokey", actualItem.getPromoKey());
        Assertions.assertNull(actualItem.getErrors());

        Assertions.assertEquals(789L, actualItem.getBuyerRegionId().longValue());
        Assertions.assertEquals(890L, actualItem.getShopId().longValue());
        Assertions.assertEquals(true, actualItem.isNoAuth());
        Assertions.assertEquals(true, actualItem.isFromCache());
        Assertions.assertEquals(EntityHelper.CREATION_DATE, actualItem.getCacheDate());
        Assertions.assertEquals(Currency.USD, actualItem.getShopCurrency());
        assertThat(actualItem.getOutletIds(), CoreMatchers.hasItems(1L, 2L, 3L));

        Assertions.assertEquals("some-offer-description-from-supplier", actualItem.getSupplierDescription());
        assertThat(
                actualItem.getManufacturerCountries().stream().map(c -> c.getId().get()).collect(Collectors.toList()),
                containsInAnyOrder(65, 213)
        );
        assertThat(
                actualItem.getManufacturerCountries().stream().map(c -> c.getName().get()).collect(Collectors.toList()),
                containsInAnyOrder("Москва", "Новосибирск")
        );
        Assertions.assertEquals("some-work-schedule-from-supplier", actualItem.getSupplierWorkSchedule());
    }
}
