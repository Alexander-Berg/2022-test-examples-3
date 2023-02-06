package ru.yandex.market.checkout.checkouter.controllers;

import java.util.Set;
import java.util.stream.IntStream;

import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.limit.HitRateGroupRateLimitsChecker;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.ratelimit.RateLimitCheckerHelper;
import ru.yandex.market.checkout.helpers.ActualizeHelper;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_FORWARDED_FOR;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_HIT_RATE_GROUP;

/**
 * @author ifilippov5
 */
public class RateLimitWhitelistTest extends AbstractWebTestBase {

    private static final int STATUS_OK = 200;
    private static final int STATUS_HIT_RATE_LIMIT_EXCEEDED = 420;
    private static final Set<String> IP_WHITELIST = Set.of("78.40.29.146", "78.40.29.147");

    @Autowired
    protected AuthHelper authHelper;

    @Autowired
    protected ActualizeHelper actualizeHelper;

    @Autowired
    private HitRateGroupRateLimitsChecker userActualizationLimits;

    @Autowired
    private HitRateGroupRateLimitsChecker userCartLimits;

    @Autowired
    private HitRateGroupRateLimitsChecker userCheckoutLimits;

    @BeforeEach
    public void setUp() {
        setFixedTime(getClock().instant());
    }

    @Test
    @Story(Stories.CART)
    public void cartIpWhitelist() {
        clearWhiteList();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        parameters.setCheckCartErrors(false);

        int cartDailyRequestLimit = 100;
        IntStream.rangeClosed(1, cartDailyRequestLimit)
                .forEach(i ->
                        userCartLimits.increment(order.getBuyer().getUid(), HitRateGroup.LIMIT, 1));
        RateLimitCheckerHelper.awaitEmptyQueue(userCartLimits.getExecutorService());
        parameters.setExpectedCartReturnCode(STATUS_HIT_RATE_LIMIT_EXCEEDED);
        orderCreateHelper.cart(parameters, buildHeaders());
        initIpWhitelist();
        parameters.setExpectedCartReturnCode(STATUS_OK);
        orderCreateHelper.cart(parameters, buildHeaders());
    }

    @Test
    @Story(Stories.CHECKOUT)
    public void checkoutIpWhitelist() throws Exception {
        clearWhiteList();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExpectedCartReturnCode(STATUS_OK);
        MultiCart cart = orderCreateHelper.cart(parameters, buildHeaders());

        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
        int checkoutDailyRequestLimit = 50;
        IntStream.rangeClosed(1, checkoutDailyRequestLimit)
                .forEach(i ->
                        userCheckoutLimits.increment(cart.getBuyer().getUid(), HitRateGroup.LIMIT, 1));
        RateLimitCheckerHelper.awaitEmptyQueue(userCheckoutLimits.getExecutorService());
        parameters.setExpectedCheckoutReturnCode(STATUS_HIT_RATE_LIMIT_EXCEEDED);
        orderCreateHelper.checkout(cart, parameters, buildHeaders());
        initIpWhitelist();
        parameters.setExpectedCheckoutReturnCode(STATUS_OK);
        orderCreateHelper.checkout(cart, parameters, buildHeaders());
    }

    @Test
    public void authIpWhitelist() throws Exception {
        clearWhiteList();
        String ip = "78.40.29.146";
        String userAgent = "test";
        for (int attempt = 0; attempt < 20; attempt++) {
            authHelper.auth(ip, userAgent, STATUS_OK);
        }
        authHelper.auth(ip, userAgent, STATUS_HIT_RATE_LIMIT_EXCEEDED);
        initIpWhitelist();
        authHelper.auth(ip, userAgent, STATUS_OK);
    }

    @Test
    public void ordersByBindKeyIpWhitelist() throws Exception {
        clearWhiteList();
        int orderNotFoundStatus = 404;
        for (int attempt = 0; attempt < 5; attempt++) {
            authHelper.ordersByBindKey(buildHeaders(), orderNotFoundStatus);
        }
        authHelper.ordersByBindKey(buildHeaders(), STATUS_HIT_RATE_LIMIT_EXCEEDED);
        initIpWhitelist();
        authHelper.ordersByBindKey(buildHeaders(), orderNotFoundStatus);
    }

    @Test
    @Story(Stories.ACTUALIZE)
    @Disabled("flaky")
    public void actualizeIpWhitelist() throws Exception {
        clearWhiteList();
        MockHttpServletRequestBuilder request = actualizeHelper.createRequestBuilder();
        request.headers(buildHeaders());
        IntStream.rangeClosed(1, 200)
                .forEach(i ->
                        userActualizationLimits.increment(123, HitRateGroup.LIMIT, 1));
        RateLimitCheckerHelper.awaitEmptyQueue(userActualizationLimits.getExecutorService());
        mockMvc.perform(request).andExpect(status().is(STATUS_HIT_RATE_LIMIT_EXCEEDED));
        initIpWhitelist();
        mockMvc.perform(request).andExpect(status().is4xxClientError());
    }

    @Test
    @Story(Stories.MULTICART_ACTUALIZE)
    public void multicartActualizeIpWhitelist() {
        clearWhiteList();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        parameters.setCheckCartErrors(false);

        int cartDailyRequestLimit = 100;
        IntStream.rangeClosed(1, cartDailyRequestLimit)
                .forEach(i ->
                        userCartLimits.increment(order.getBuyer().getUid(), HitRateGroup.LIMIT, 1));
        RateLimitCheckerHelper.awaitEmptyQueue(userCartLimits.getExecutorService());
        parameters.setExpectedCartReturnCode(STATUS_HIT_RATE_LIMIT_EXCEEDED);
        orderCreateHelper.multiCartActualize(parameters, buildHeaders());
        initIpWhitelist();
        parameters.setExpectedCartReturnCode(STATUS_OK);
        orderCreateHelper.multiCartActualize(parameters, buildHeaders());
    }


    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(X_HIT_RATE_GROUP, HitRateGroup.LIMIT.name());
        headers.set(X_FORWARDED_FOR,
                "78.40.29.146, 78.40.29.146, 2a02:6b8:c14:2c21:10d:dbba:0:1be9, 2a02:6b8:c02:453:0:577:303f:7882");
        return headers;
    }

    private void clearWhiteList() {
        checkouterFeatureWriter.writeValue(CollectionFeatureType.CLIENT_IPS_WHITELIST, Set.of());
    }

    private void initIpWhitelist() {
        checkouterFeatureWriter.writeValue(CollectionFeatureType.CLIENT_IPS_WHITELIST, IP_WHITELIST);
    }
}
