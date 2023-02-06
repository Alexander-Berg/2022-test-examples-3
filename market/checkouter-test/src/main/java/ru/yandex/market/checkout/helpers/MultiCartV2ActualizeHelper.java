package ru.yandex.market.checkout.helpers;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.util.StringListUtils;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.MultiCartRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.configuration.ActualizationRequestConfiguration;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.Optional.ofNullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_TEST_BUCKETS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.REDUCE_PICTURES;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_HIT_RATE_GROUP;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_MARKET_REQUEST_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_MOCK_LOYALTY_DEGRADATION;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_ORDER_META;

@WebTestHelper
public class MultiCartV2ActualizeHelper extends MockMvcAware {

    @Autowired
    public MultiCartV2ActualizeHelper(WebApplicationContext webApplicationContext,
                                      TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public MultiCartResponse multiCartActualize(MultiCartRequest multiCartRequest) {
        return multiCartActualize(multiCartRequest, new ActualizationRequestConfiguration());
    }

    public MultiCartResponse multiCartActualize(MultiCartRequest multiCartRequest,
                                                ActualizationRequestConfiguration configuration) {
        try {
            return multiCartActualize(multiCartRequest, configuration, buildHeaders(configuration));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public MultiCartResponse multiCartActualize(MultiCartRequest multiCartRequest,
                                                ActualizationRequestConfiguration configuration,
                                                HttpHeaders headers) throws Exception {

        var content = testSerializationService.serializeCheckouterObject(multiCartRequest);
        var uid = Objects.requireNonNull(multiCartRequest.getBuyer().getUserId()) + "";
        MockHttpServletRequestBuilder requestBuilder = post("/v2/multicart/actualize")
                .param("clientRole", ClientRole.USER.name())
                .param("uid", uid)
                .param("platform",
                        Objects.toString(ofNullable(configuration.getPlatform()).orElse(Platform.UNKNOWN)))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content);

        if (configuration.isSandbox()) {
            requestBuilder.param("sandbox", "true").param("context", "SANDBOX");
        }

        if (configuration.isReducePictures()) {
            requestBuilder.param(REDUCE_PICTURES, "true");
        }

        if (!configuration.getPriceDropMskuSet().isEmpty()) {
            requestBuilder.param(CheckouterClientParams.PRICE_DROP_MSKU_LIST,
                    StringListUtils.toString(
                            configuration.getPriceDropMskuSet(),
                            ",",
                            String::valueOf
                    )
            );
        }

        if (configuration.getContext() != null) {
            requestBuilder.param(CheckouterClientParams.CONTEXT,
                    configuration.getContext().name());
        }

        if (configuration.getApiSettings() != null) {
            requestBuilder.param(CheckouterClientParams.API_SETTINGS,
                    configuration.getApiSettings().name());
        }

        if (configuration.getColor() != null) {
            requestBuilder.param(CheckouterClientParams.RGB,
                    configuration.getColor().name());
        }

        if (configuration.isMinifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.MINIFY_OUTLETS,
                    Boolean.toString(configuration.isMinifyOutlets()));
        }

        if (configuration.isSimplifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.SIMPLIFY_OUTLETS,
                    Boolean.toString(configuration.isSimplifyOutlets()));
        }

        if (StringUtils.isNotEmpty(configuration.getExperiments())) {
            requestBuilder.header(X_EXPERIMENTS, configuration.getExperiments());
        }

        if (StringUtils.isNotEmpty(configuration.getTestBuckets())) {
            requestBuilder.header(X_TEST_BUCKETS, configuration.getTestBuckets());
        }

        if (Boolean.TRUE.equals(configuration.isUserHasPrime())) {
            requestBuilder.param(CheckouterClientParams.PRIME,
                    Boolean.toString(configuration.isUserHasPrime()));
        }

        if (Boolean.TRUE.equals(configuration.isYandexPlus())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_PLUS,
                    Boolean.toString(configuration.isYandexPlus()));
        }

        if (Boolean.TRUE.equals(configuration.isYandexEmployee())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_EMPLOYEE,
                    Boolean.toString(configuration.isYandexEmployee()));
        }

        if (configuration.getPerkPromoId() != null) {
            requestBuilder.param(CheckouterClientParams.PERK_PROMO_ID,
                    configuration.getPerkPromoId());
        }

        if (configuration.isShowCredits()) {
            requestBuilder.param(CheckouterClientParams.SHOW_CREDITS,
                    Boolean.toString(configuration.isShowCredits()));
        }

        if (configuration.isShowCreditBroker()) {
            requestBuilder.param(CheckouterClientParams.SHOW_CREDIT_BROKER,
                    Boolean.toString(configuration.isShowCredits()));
        }

        if (configuration.isShowInstallments()) {
            requestBuilder.param(CheckouterClientParams.SHOW_INSTALLMENTS,
                    Boolean.toString(configuration.isShowInstallments()));
        }

        if (configuration.isShowSbp()) {
            requestBuilder.param(CheckouterClientParams.SHOW_SBP,
                    Boolean.toString(configuration.isShowSbp()));
        }

        if (Boolean.TRUE.equals(configuration.isDebugAllCourierOptions())) {
            requestBuilder.param(CheckouterClientParams.DEBUG_ALL_COURIER_OPTIONS, Boolean.TRUE.toString());
        }

        if (configuration.getForceShipmentDay() != null) {
            requestBuilder.param(CheckouterClientParams.FORCE_SHIPMENT_DAY,
                    String.valueOf(configuration.getForceShipmentDay()));
        }

        if (configuration.getSkipDiscountCalculation() != null) {
            requestBuilder.param(CheckouterClientParams.SKIP_DISCOUNT_CALCULATION,
                    Boolean.toString(configuration.getSkipDiscountCalculation()));
        }

        if (configuration.getMockLoyaltyDegradation() != null) {
            requestBuilder.header(X_MOCK_LOYALTY_DEGRADATION,
                    Boolean.toString(configuration.getMockLoyaltyDegradation()));
        }

        if (Boolean.TRUE.equals(configuration.getShowMultiServiceIntervals())) {
            requestBuilder.param(CheckouterClientParams.SHOW_MULTI_SERVICE_INTERVALS, Boolean.TRUE.toString());
        }

        if (configuration.getForceDeliveryId() != null) {
            requestBuilder.param(
                    CheckouterClientParams.FORCE_DELIVERY_ID,
                    configuration.getForceDeliveryId().toString()
            );
        }

        if (configuration.isShowVat()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_VAT, Boolean.toString(configuration.isShowVat()));
        }

        requestBuilder.param(
                CheckouterClientParams.PERKS, configuration.getPerks());

        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andDo(MockMvcResultHandlers.log());

        MvcResult mvcResult = resultActions.andReturn();
        if (mvcResult.getResponse().getStatus() != 200) {
            throw new RuntimeException(mvcResult.getResponse().getErrorMessage());
        }
        String responseJson = mvcResult.getResponse().getContentAsString();
        return testSerializationService.deserializeCheckouterObject(responseJson, MultiCartResponse.class);
    }

    private HttpHeaders buildHeaders(ActualizationRequestConfiguration configuration) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(X_HIT_RATE_GROUP, avoidNull(configuration.getHitRateGroup(),
                HitRateGroup.UNLIMIT).name());
        if (configuration.getMetaInfo() != null) {
            headers.set(X_ORDER_META, configuration.getMetaInfo());
        }
        if (configuration.getMarketRequestId() != null) {
            headers.set(X_MARKET_REQUEST_ID, configuration.getMarketRequestId());
        }
        return headers;
    }

}
