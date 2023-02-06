package ru.yandex.market.loyalty.client;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponActivationRequest;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.CouponParamName;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.DiscountRequest;
import ru.yandex.market.loyalty.api.model.DiscountResponse;
import ru.yandex.market.loyalty.api.model.LoyaltyDateFormat;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.OrderRequest;
import ru.yandex.market.loyalty.api.model.OrderResponse;
import ru.yandex.market.loyalty.api.model.PromoBalance;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.events.LoginEventDto;
import ru.yandex.market.loyalty.api.model.events.LoyaltyEvent;
import ru.yandex.market.loyalty.api.model.events.SubscriptionEventDto;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkRequest;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.api.utils.PumpkinUtils;
import ru.yandex.market.loyalty.client.test.MarketLoyaltyMockedServerTest;
import ru.yandex.market.loyalty.client.utils.BeanHolder;
import ru.yandex.market.pers.notify.model.NotificationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionByPropertyValuesAs;

/**
 * @author ukchuvrus
 */

public class MarketLoyaltyClientTest extends MarketLoyaltyMockedServerTest {
    private static final String EMAIL = "email@yandex-team.ru";
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private RestTemplate restTemplate;
    @Autowired
    private BeanHolder<ObjectMapper> objectMapperHolder;

    @Test
    public void notExposeObjectMapperAndRestTemplate() {
        assertNull(objectMapper);
        assertNull(restTemplate);
    }

    @Test
    public void testGenerateCouponByRequest() throws Exception {
        CouponCreationRequest request = CouponCreationRequest.builder("key2", 100L)
                .identity(new Uid(2L))
                .forceActivation(true)
                .params(Collections.singletonMap(CouponParamName.USER_EMAIL, "valter@email.ru"))
                .build();
        CouponDto coupon = new CouponDto("coupon_code2", CouponStatus.ACTIVE);

        CouponDto result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(startsWith(loyaltyUrl + "/coupon/source/key2/promoId/100?")))
                    .andExpect(queryParam("forceActivation", "true"))
                    .andExpect(queryParam("requiredForUserType", "UID"))
                    .andExpect(queryParam("requiredForUserId", "2"))
                    .andExpect(content().string("{\"user_email\":\"valter@email.ru\"}"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withSuccess(coupon));

            result = marketLoyaltyClient.generateCoupon(request);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(coupon));
    }

    @Test
    public void testGenerateAndActivateCouponByPromoId() throws Exception {
        final String key = "key2";
        Uid uid = new Uid(2L);
        CouponDto coupon = new CouponDto("coupon_code2", CouponStatus.ACTIVE);
        final Long promoId = 100L;

        CouponDto result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(startsWith(loyaltyUrl + "/coupon/source/key2/promoId/100?")))
                    .andExpect(queryParam("forceActivation", "true"))
                    .andExpect(queryParam("requiredForUserType", "UID"))
                    .andExpect(queryParam("requiredForUserId", "2"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withSuccess(coupon));

            result = marketLoyaltyClient.generateAndActivateCoupon(key, uid, promoId);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(coupon));
    }

    @Test
    public void testGenerateAndActivateCouponByPromoIdWithoutIdentity() throws Exception {
        final String key = "key2";
        CouponDto coupon = new CouponDto("coupon_code2", CouponStatus.ACTIVE);
        final Long promoId = 100L;

        CouponDto result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(startsWith(loyaltyUrl + "/coupon/source/key2/promoId/100?")))
                    .andExpect(queryParam("forceActivation", "true"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withSuccess(coupon));

            result = marketLoyaltyClient.generateAndActivateCoupon(key, null, promoId);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(coupon));
    }

    @Test
    public void testGenerateCouponByPromoId() throws Exception {
        final String key = "key";
        Uid uid = new Uid(1L);
        CouponDto coupon = new CouponDto("coupon_code", CouponStatus.ACTIVE);
        final Long promoId = 100L;

        CouponDto result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/coupon/source/key/promoId/100?requiredForUserType=UID" +
                    "&requiredForUserId=1"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withSuccess(coupon));

            result = marketLoyaltyClient.generateCoupon(key, uid, promoId);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(coupon));

    }

    @Test
    public void testActivateCoupons() throws Exception {
        CouponActivationRequest couponActivationRequest = podamFactory.manufacturePojo(CouponActivationRequest.class);

        List<CouponDto> expect = couponActivationRequest.getItems().stream()
                .map(item -> {
                    CouponDto couponDto = new CouponDto();
                    couponDto.setStatus(CouponStatus.ACTIVE);
                    couponDto.setCode(item.getCode());
                    return couponDto;
                }).collect(Collectors.toList());

        List<CouponDto> result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/coupon/activate"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(couponActivationRequest)))
                    .andRespond(withSuccess(expect));

            result = marketLoyaltyClient.activateCoupons(couponActivationRequest);

            server.verify();
        }

        assertThat(result, containsInAnyOrder(sameCollectionByPropertyValuesAs(expect)));
    }

    @Test
    public void testActivatePromocodes() throws Exception {
        PromocodeActivationRequest promocodeActivationRequest =
                podamFactory.manufacturePojo(PromocodeActivationRequest.class);

        List<PromocodeActivationResult> expect = promocodeActivationRequest.getCodes().stream()
                .map(code -> PromocodeActivationResult.of(code, PromocodeActivationResultCode.SUCCESS))
                .collect(Collectors.toList());

        PromocodeActivationResponse result;

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/promocodes/v1/activate"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(promocodeActivationRequest)))
                    .andRespond(withSuccess(new PromocodeActivationResponse(expect)));

            result = marketLoyaltyClient.activatePromocodes(promocodeActivationRequest);

            server.verify();
        }

        assertThat(result.getActivationResults(),
                containsInAnyOrder(sameCollectionByPropertyValuesAs(expect)));
    }

    private DiscountResponse testDiscounts(String url, Function<DiscountRequest, DiscountResponse> service) throws Exception {
        DiscountRequest request = podamFactory.manufacturePojo(DiscountRequest.class);

        OrderRequest orderRequest = request.getOrderRequest();

        DiscountResponse expect = podamFactory.manufacturePojo(DiscountResponse.class);

        OrderResponse orderResponse = expect.getOrderResponse();
        orderResponse.setOrderId(orderRequest.getOrderId());
        orderResponse.setItems(orderRequest.getItems().stream()
                .map(itemRequest -> {
                    OrderItemResponse itemResponse = podamFactory.manufacturePojo(OrderItemResponse.class);
                    itemResponse.setFeedId(itemRequest.getFeedId());
                    itemResponse.setOfferId(itemRequest.getOfferId());
                    itemResponse.setPrice(itemRequest.getPrice());
                    itemResponse.setQuantity(itemRequest.getQuantity());
                    return itemResponse;
                }).collect(Collectors.toList()));

        DiscountResponse result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(url))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(request)))
                    .andRespond(withSuccess(expect));

            result = service.apply(request);

            server.verify();
        }

        assertThat(result.getOrderResponse(), samePropertyValuesAs(expect.getOrderResponse()));
        assertThat(result.getOrderResponse().getItems(),
                contains(sameCollectionByPropertyValuesAs(expect.getOrderResponse().getItems(), "itemError", "promos"
                        , "cashback")));
        assertThat(
                result.getOrderResponse().getItems().stream()
                        .map(OrderItemResponse::getPromos)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()),
                contains(sameCollectionByPropertyValuesAs(expect.getOrderResponse().getItems().stream()
                                .map(OrderItemResponse::getPromos)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()),
                        "usedCoin", "cashback"
                )));
        return result;
    }

    @Test
    public void shouldWriteDateInMskTimeZone() throws Exception {
        SubscriptionEventDto subscriptionEvent = new SubscriptionEventDto();
        subscriptionEvent.setEmail("user@yandex.ru");
        subscriptionEvent.setUid(123L);
        SimpleDateFormat dateFormat = new SimpleDateFormat(LoyaltyDateFormat.LOYALTY_DATE_FORMAT);
        String dateAsString = "30-06-2018 00:00:00";
        subscriptionEvent.setLastUnsubscribeDate(dateFormat.parse(dateAsString));

        String json = objectMapperHolder.get().writeValueAsString(subscriptionEvent);
        assertThat(json, containsString(dateAsString));
        assertEquals(TimeZone.getTimeZone("Europe/Moscow"), TimeZone.getDefault());
    }

    @Test
    public void testCalculateDiscountWithCouponCode() throws Exception {
        String couponCode = UUID.randomUUID().toString();

        testDiscounts(loyaltyUrl + "/discount/calc?couponCode=" + couponCode, discountRequest ->
                marketLoyaltyClient.calculateDiscount(couponCode, discountRequest));
    }

    @Test
    public void testCalculateDiscount() throws Exception {
        testDiscounts(loyaltyUrl + "/discount/calc", discountRequest ->
                marketLoyaltyClient.calculateDiscount("", discountRequest));
        testDiscounts(loyaltyUrl + "/discount/calc", discountRequest ->
                marketLoyaltyClient.calculateDiscount(null, discountRequest));
    }

    @Test
    public void testSpendDiscountWithCouponCode() throws Exception {
        String couponCode = UUID.randomUUID().toString();

        testDiscounts(loyaltyUrl + "/discount/spend?couponCode=" + couponCode, discountRequest ->
                marketLoyaltyClient.spendDiscount(couponCode, discountRequest));
    }

    @Test
    public void testCalcDiscountPumpkin() throws InterruptedException {
        HttpHeaders pumpkinHeaders = new HttpHeaders();
        pumpkinHeaders.add(PumpkinUtils.PUMPKIN_HEADER_NAME, "true");
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/discount/calc/v3"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess().headers(pumpkinHeaders));
            try {
                marketLoyaltyClient.calculateDiscount(podamFactory.manufacturePojo(MultiCartWithBundlesDiscountRequest.class));
                throw new RuntimeException("calculateDiscount should throw exception");
            } catch (MarketLoyaltyException mle) {
                assertEquals(MarketLoyaltyErrorCode.RESPONSE_FROM_PUMPKIN, mle.getMarketLoyaltyErrorCode());
            }
            server.verify();
        }
    }

    @Test
    public void testSpendDiscount() throws Exception {
        testDiscounts(loyaltyUrl + "/discount/spend", discountRequest ->
                marketLoyaltyClient.spendDiscount("", discountRequest));
        testDiscounts(loyaltyUrl + "/discount/spend", discountRequest ->
                marketLoyaltyClient.spendDiscount(null, discountRequest));
    }

    @Test
    public void testSpendDiscountPumpkin() throws InterruptedException {
        HttpHeaders pumpkinHeaders = new HttpHeaders();
        pumpkinHeaders.add(PumpkinUtils.PUMPKIN_HEADER_NAME, "true");
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/discount/spend/v3"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess().headers(pumpkinHeaders));
            try {
                marketLoyaltyClient.spendDiscount(podamFactory.manufacturePojo(MultiCartWithBundlesDiscountRequest.class));
                throw new RuntimeException("spendDiscount should throw exception");
            } catch (MarketLoyaltyException mle) {
                assertEquals(MarketLoyaltyErrorCode.RESPONSE_FROM_PUMPKIN, mle.getMarketLoyaltyErrorCode());
            }
            server.verify();
        }
    }


    @Test
    public void testRevertDiscount() throws Exception {
        String discountToken = UUID.randomUUID().toString();

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/discount/revert?discountToken=" + discountToken))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess());

            marketLoyaltyClient.revertDiscount(Collections.singleton(discountToken));

            server.verify();
        }
    }

    @Test
    public void testProcessLoginEvent() throws Exception {
        LoginEventDto loginEventDto = new LoginEventDto();
        loginEventDto.setUid(123L);
        loginEventDto.setPlatform(MarketPlatform.BLUE);
        loginEventDto.setRegion(234L);

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl +
                    String.format("/event/%s/process", LoyaltyEvent.LOGIN)))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(loginEventDto)))
                    .andRespond(withSuccess());

            marketLoyaltyClient.processEvent(loginEventDto);

            server.verify();
        }
    }

    @Test
    public void testProcessLoginEventWithError() throws Exception {
        LoginEventDto loginEventDto = new LoginEventDto();
        loginEventDto.setUid(123L);
        loginEventDto.setPlatform(MarketPlatform.BLUE);
        loginEventDto.setRegion(234L);

        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl +
                    String.format("/event/%s/process", LoyaltyEvent.LOGIN)))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(loginEventDto)))
                    .andRespond(withServerError());

            try {
                marketLoyaltyClient.processEvent(loginEventDto);
                fail();
            } catch (MarketLoyaltyException e) {
                assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR, e.getMarketLoyaltyErrorCode());
            }

            server.verify();
        }
    }

    @Test
    public void testSerializeSubscriptionEvent() throws Exception {
        SubscriptionEventDto request = new SubscriptionEventDto();
        request.setEmail(EMAIL);
        request.setPlatform(MarketPlatform.BLUE);
        request.setNotificationType(NotificationType.ADVERTISING);

        String requestBody = getObjectMapper().writeValueAsString(request);
        assertThat(requestBody, containsString("\"notificationType\":\"ADVERTISING\""));
    }

    @Test
    public void testUnknownEnum() throws Exception {
        String request = '{' +
                "\"clientDeviceType\": \"SOME_NEW_ENUM\"" +
                '}';

        OperationContextDto parsed = getObjectMapper().readValue(request, OperationContextDto.class);
        assertEquals(UsageClientDeviceType.UNKNOWN, parsed.getClientDeviceType());
    }

    @Test
    public void testGetBalance() throws Exception {
        PromoBalance balance = podamFactory.manufacturePojo(PromoBalance.class);
        long promoId = ThreadLocalRandom.current().nextLong();

        PromoBalance result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/promo/" + promoId + "/balance"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(balance));

            result = marketLoyaltyClient.getBalance(promoId);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(balance));
    }

    @Test
    public void testBuyPerkAllOk() throws Exception {
        BuyPerkRequest request = podamFactory.manufacturePojo(BuyPerkRequest.class);
        BuyPerkResponse response = BuyPerkResponse.SimpleResponses.ALL_OK.getInstance();
        BuyPerkResponse result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/perk/buy"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(request)))
                    .andRespond(withSuccess(response));

            result = marketLoyaltyClient.buyPerk(request);

            server.verify();
        }

        assertEquals(response, result);
    }

    @Test
    public void testBuyPerkAlreadyBoughtOnSameOrderId() throws Exception {
        BuyPerkRequest request = podamFactory.manufacturePojo(BuyPerkRequest.class);
        BuyPerkResponse response = BuyPerkResponse.SimpleResponses.ALREADY_PURCHASED_ON_SAME_ORDER.getInstance();
        BuyPerkResponse result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/perk/buy"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(request)))
                    .andRespond(withSuccess(response));

            result = marketLoyaltyClient.buyPerk(request);

            server.verify();
        }

        assertEquals(response, result);
    }

    @Test
    public void testBuyPerkAlreadyBoughtOnOtherOrderId() throws Exception {
        BuyPerkRequest request = podamFactory.manufacturePojo(BuyPerkRequest.class);
        BuyPerkResponse response = podamFactory.manufacturePojo(BuyPerkResponse.AlreadyPurchasedOnAnotherOrder.class);
        BuyPerkResponse result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/perk/buy"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(request)))
                    .andRespond(withSuccess(response));

            result = marketLoyaltyClient.buyPerk(request);

            server.verify();
        }

        assertThat(result, samePropertyValuesAs(response));
    }

    @Test
    public void testBuyPerkUnknownResponse() throws Exception {
        BuyPerkRequest request = podamFactory.manufacturePojo(BuyPerkRequest.class);
        BuyPerkResponse response = new SomeNewBuyPerkResponse();
        BuyPerkResponse result;
        try (CloseableMockRestServiceServerHolder server = getMockRestServiceServer()) {
            server.expect(requestTo(loyaltyUrl + "/perk/buy"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(getObjectMapper().writeValueAsString(request)))
                    .andRespond(withSuccess(response));

            result = marketLoyaltyClient.buyPerk(request);

            server.verify();
        }

        assertEquals(BuyPerkResponse.SimpleResponses.UNKNOWN.getInstance(), result);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            defaultImpl = BuyPerkResponse.Unknown.class
    )
    @JsonSubTypes(
            @JsonSubTypes.Type(
                    value = SomeNewBuyPerkResponse.class,
                    name = "SomeNewBuyPerkResponse"
            )
    )
    private static class SomeNewBuyPerkResponse implements BuyPerkResponse {
        @Override
        public <T, E extends Throwable> T apply(Visitor<T, E> visitor) {
            fail("Do not want");
            return null;
        }
    }
}
