package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItemRequestDto;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.test.TestUtil.stringFromFile;

class TakeoutControllerTest extends MarketUtilsMockedDbTest {
    private static final long UID = 123456;
    private static final Uid IDENTITY = new Uid(UID);

    private static final String EMAIL = "foo@bar.buzz";
    private static final String ANOTHER_EMAIL = "another_foo@bar.buzz";
    private static final String UNCONFIRMED_EMAIL = "unconfirmed@bar.buzz";
    private static final String RED_PUSH_UID = "12345@lotalot.ru";
    private static final String COMPARISON_PRODUCT_ID = "sdlfk32l";


    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @Autowired
    private SubscriptionControllerInvoker subscriptionControllerInvoker;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupPassport() {
        blackBoxPassportService.doReturn(UID, Arrays.asList(EMAIL, ANOTHER_EMAIL));
    }

    private void createSimpleSubscriptionsAndComparisons() throws Exception {
        createSimple(EMAIL, NotificationType.ADVERTISING);
        createSimple(ANOTHER_EMAIL, NotificationType.STORE_ADVERTISING);
        createSimple(EMAIL, NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING);
        createComparison();
    }

    @Test
    void takeoutForWhiteMustReturnSubscriptionsAndComparisons() throws Exception {
        createSimpleSubscriptionsAndComparisons();
        invokeTakeoutAndCheck("white", "/data/takeout/white.json");
    }

    @Test
    void takeoutForBlueMustReturnSubscriptionsOnly() throws Exception {
        createSimpleSubscriptionsAndComparisons();
        invokeTakeoutAndCheck("blue", "/data/takeout/blue.json");
    }

    @Test
    void takeoutForRedMustReturnSubscriptionsOnly() throws Exception {
        createSimpleSubscriptionsAndComparisons();
        invokeTakeoutAndCheck("red", "/data/takeout/red.json");
    }

    @Test
    void mustReturnRegionAndPriceForPriceDrop() throws Exception {
        createPriceAlert(EMAIL, NotificationType.PA_ON_SALE, "12345", "213", "0");
        createPriceAlert(EMAIL, NotificationType.PRICE_DROP, "4567", "213", "100");
        invokeTakeoutAndCheck("white", "/data/takeout/price_drop.json");
    }

    @Test
    void mustReturnRegionButNotPlaceAndPlatformForAdvertising() throws Exception {
        createAdvertising(EMAIL, NotificationType.ADVERTISING);
        invokeTakeoutAndCheck("white", "/data/takeout/advertising.json");
    }

    @Test
    void mustReturnUnsubscribeAndNeedConfirmation() throws Exception {
        createUnsubscribed(EMAIL, NotificationType.ADVERTISING);
        createSimple(UNCONFIRMED_EMAIL, NotificationType.ADVERTISING);
        invokeTakeoutAndCheck("white", "/data/takeout/unsubscribed_unconfirmed.json");
    }

    @Test
    void mustReturnEmptyResponseForNonExistingUser() throws Exception {
        invokeTakeoutAndCheck("white", "/data/takeout/empty.json");
    }

    @Test
    void mustReturn400forUnknownColor() throws Exception {
        mockMvc.perform(takeoutRequest("strange"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletedComparisonsMustNotBeReturned() throws Exception {
        createComparison();
        deleteComparison();
        invokeTakeoutAndCheck("white", "/data/takeout/empty.json");
    }

    @Test
    void pushSubscriptionsMustNotContainEmail() throws Exception {
        createPushSubscription(RED_PUSH_UID, NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING_PUSH);
        invokeTakeoutAndCheck("red", "/data/takeout/red_push.json");

    }

    @Test
    void someTypesMustNotBeExported() throws Exception {
        createSimple(EMAIL, NotificationType.BT_STORE_ADVERTISING);
        invokeTakeoutAndCheck("blue", "/data/takeout/empty.json");
    }


    private void createUnsubscribed(String email, NotificationType type) throws Exception {
        createSimple(email, type);
        subscriptionControllerInvoker.unsubscribe(email, type);
    }

    private void createSimple(String email, NotificationType type) throws Exception {
        EmailSubscription subscription = new EmailSubscription(email, type, null);
        subscriptionControllerInvoker.createSubscriptions(IDENTITY, email, Collections.singletonList(subscription));
    }

    private void createAdvertising(String email, NotificationType type) throws Exception {
        EmailSubscription subscription = new EmailSubscription(email, type, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_PLACE, "footer");
        subscription.addParameter(EmailSubscriptionParam.PARAM_PLATFORM, "desktop");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        subscriptionControllerInvoker.createSubscriptions(IDENTITY, email, Collections.singletonList(subscription));
    }

    private void createPriceAlert(String email, NotificationType type, String modelId, String regionId,
                                  String price) throws Exception {
        EmailSubscription subscription = new EmailSubscription(email, type, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, modelId);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, regionId);
        subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, price);
        subscriptionControllerInvoker.createSubscriptions(IDENTITY, email, Collections.singletonList(subscription));
    }

    private void createPushSubscription(String email, NotificationType type) throws Exception {
        EmailSubscription subscription = new EmailSubscription(email, type, null);
        subscriptionControllerInvoker.createSubscriptions(IDENTITY, email, Collections.singletonList(subscription), true);
    }

    private void invokeTakeoutAndCheck(String color, String expectedResponseFilename) throws Exception {
        String response = mockMvc.perform(takeoutRequest(color))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(stringFromFile(expectedResponseFilename), response, jsonComparator());
    }

    private MockHttpServletRequestBuilder takeoutRequest(String color) {
        return get("/takeout/?uid={uid}&color={color}", UID, color);
    }

    private static CustomComparator jsonComparator() {
        return new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("comparisons[*].lastUpdate", (o1, o2) -> true)
        );
    }

    private void createComparison() throws Exception {
        String categoryId = "765234";
        List<ComparisonItemRequestDto> items = Collections.singletonList(new ComparisonItemRequestDto(categoryId, COMPARISON_PRODUCT_ID));
        mockMvc.perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    private void deleteComparison() throws Exception {
        mockMvc.perform(delete("/comparison/{userType}/{userId}/product/{productId}", "UID", UID, COMPARISON_PRODUCT_ID))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

}
