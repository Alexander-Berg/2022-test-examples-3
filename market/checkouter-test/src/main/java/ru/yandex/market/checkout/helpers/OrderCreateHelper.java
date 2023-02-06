package ru.yandex.market.checkout.helpers;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.CheckouterMockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.shop.ShipmentDateCalculationRuleService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.checkout.checkouter.stock.model.SSItemKey;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.StringListUtils;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.MultiCartRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.CheckoutOptionParameters;
import ru.yandex.market.checkout.providers.v2.multicart.request.MultiCartRequestProvider;
import ru.yandex.market.checkout.providers.v2.multicart.response.MultiCartResponseProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.geocoder.GeocoderConfigurer;
import ru.yandex.market.checkout.util.geocoder.GeocoderParameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;
import ru.yandex.market.checkout.util.report.ColoredReportConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.report.MarketSearchSession.CpaContextRecord;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.common.util.ObjectUtils.defaultIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.firstOrNull;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_TEST_BUCKETS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.REDUCE_PICTURES;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_HIT_RATE_GROUP;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_MARKET_REQUEST_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_MOCK_LOYALTY_DEGRADATION;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_ORDER_META;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_YANDEX_ICOOKIE;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.UNFREEZE_STOCKS_TIME;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.ORDER_CREATION_DATE;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.SHOW_INFO;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.cpaRecord;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.generateShowInfo;

/**
 * @author Nikolai Iusiumbeli
 * date: 18/07/2017
 */
@WebTestHelper
public class OrderCreateHelper extends MockMvcAware {

    public static final String FAKE_SHOW_UID = "selfcheck";
    public static final long FAKE_DELIVERY_SERVICE_ID = -1;

    private static final Logger LOG = LoggerFactory.getLogger(OrderCreateHelper.class);

    @Autowired
    private ShopService shopService;
    @Autowired
    private ColorConfig colorConfig;
    @Autowired
    private ColoredReportConfigurer coloredReportConfigurer;
    @Autowired
    private PushApiConfigurer pushApiConfigurer;
    @Autowired
    private GeocoderConfigurer geocoderConfigurer;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private OrderFinancialService financialService;
    @Autowired
    private LoyaltyConfigurer loyaltyConfigurer;
    @Autowired
    private TrustMockConfigurer trustMockConfigurer;
    @Resource(name = "reportCipherService")
    private CipherService reportCipherService;
    @Autowired
    private ShipmentDateCalculationRuleService shipmentDateCalculationRuleService;
    @Autowired
    private CheckouterProperties checkouterProperties;

    @Autowired
    public OrderCreateHelper(
            WebApplicationContext webApplicationContext,
            TestSerializationService testSerializationService
    ) {
        super(webApplicationContext, testSerializationService);
    }

    public MultiOrder createMultiOrder(Parameters parameters) {
        return createMultiOrder(parameters, multiCart -> {
        });
    }

    public MultiOrder createMultiOrderFake(Parameters parameters) {
        return createMultiOrderFake(parameters, multiCart -> {
        });
    }

    public MultiOrder createMultiOrder(Parameters parameters, Consumer<MultiCart> betweenCartAndCheckoutHook) {
        for (Order order : parameters.getOrders()) {
            financialService.calculateAndSetOrderTotals(order);
            setupShopsMetadata(parameters);
        }

        MultiCart originalMultiCart = parameters.getBuiltMultiCart();
        try {
            MultiCart multiCart = cart(
                    originalMultiCart,
                    parameters.getOrders(),
                    parameters,
                    buildHeaders(parameters)
            );
            betweenCartAndCheckoutHook.accept(multiCart);

            return checkout(multiCart, parameters);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public MultiOrder createMultiOrderFake(Parameters parameters, Consumer<MultiCart> betweenCartAndCheckoutHook) {
        for (Order order : parameters.getOrders()) {
            financialService.calculateAndSetOrderTotals(order);
            setupShopsMetadata(parameters);
        }

        MultiCart originalMultiCart = parameters.getBuiltMultiCart();
        try {
            MultiCart multiCart = cartFake(
                    originalMultiCart,
                    parameters.getOrders(),
                    parameters,
                    buildHeaders(parameters)
            );
            multiCart.getCarts().forEach(cart -> cart.getItems().forEach(item -> item.setShowUid(FAKE_SHOW_UID)));
            betweenCartAndCheckoutHook.accept(multiCart);

            return checkoutFake(multiCart, parameters);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Order createOrder(Parameters parameters) {
        return createOrder(parameters, multiCart -> {
        });
    }

    public Order createOrder(Parameters parameters, Consumer<MultiCart> betweenCartAndCheckoutHook) {
        MultiOrder createdMultiOrder = createMultiOrder(parameters, betweenCartAndCheckoutHook);
        Order createdOrder = firstOrNull(emptyIfNull(createdMultiOrder.getOrders()));
        if (parameters.configuration().checkout().response().checkOrderCreateErrors()) {
            assertThat(createdOrder.getId(), notNullValue());
        }
        return createdOrder;
    }

    public Order createOrderFake(Parameters parameters) {
        MultiOrder createdMultiOrder = createMultiOrderFake(parameters);
        Order createdOrder = firstOrNull(emptyIfNull(createdMultiOrder.getOrders()));
        if (parameters.configuration().checkout().response().checkOrderCreateErrors()) {
            assertThat(createdOrder.getId(), notNullValue());
        }
        return createdOrder;
    }

    public MultiCart cart(Parameters parameters) {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        return cart(reportConfigurerAuto, parameters);
    }

    public MultiCart cartFake(Parameters parameters) {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        return cartFake(reportConfigurerAuto, parameters);
    }

    public MultiCart cart(ReportConfigurer reportConfigurer, Parameters parameters) {
        try {
            return cart(reportConfigurer, parameters.getBuiltMultiCart(), parameters.getOrders(), parameters,
                    buildHeaders(parameters));
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public MultiCart cartFake(ReportConfigurer reportConfigurer, Parameters parameters) {
        try {
            return cartFake(reportConfigurer, parameters.getBuiltMultiCart(), parameters.getOrders(), parameters,
                    buildHeaders(parameters));
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public MultiCart cart(Parameters parameters, HttpHeaders headers) {
        try {
            return cart(parameters.getBuiltMultiCart(), parameters.getOrders(), parameters,
                    headers);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    private MultiCart cart(MultiCart originalMultiCart, List<Order> orders, Parameters parameters,
                           HttpHeaders headers) throws Exception {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        return cart(reportConfigurerAuto, originalMultiCart, orders, parameters, headers);
    }

    private MultiCart cartFake(MultiCart originalMultiCart, List<Order> orders, Parameters parameters,
                               HttpHeaders headers) throws Exception {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        return cartFake(reportConfigurerAuto, originalMultiCart, orders, parameters, headers);
    }

    private Color getColorByParameters(Parameters parameters) {
        // try to find out color to create request.
        // Color can be defined in request (can be missing) or should be in the order itself.
        // color in request has priority over color on order
        Color requestColor = parameters.configuration().cart().request().getColor();
        if (requestColor == null) {
            requestColor = parameters.getOrder().getRgb();
        }
        return requestColor;
    }

    private MultiCart cart(ReportConfigurer reportConfigurer, MultiCart originalMultiCart, List<Order> orders,
                           Parameters parameters, HttpHeaders headers) throws Exception {
        hackOrderVat(orders, parameters); //эта хрень тут из-за неконфигурируемости OrderItemProvider'а
        initializeMock(reportConfigurer, parameters);

        String content = testSerializationService.serializeCheckouterObject(originalMultiCart);
        MockHttpServletRequestBuilder requestBuilder = post("/cart")
                .param("clientRole", ClientRole.USER.name())
                .param("uid",
                        Objects.toString(
                                Stream.of(
                                        Optional.ofNullable(originalMultiCart).map(MultiCart::getBuyer)
                                                .map(Buyer::getUid),
                                        orders.stream().findFirst().map(Order::getBuyer).map(Buyer::getUid),
                                        Optional.of(parameters).map(Parameters::getBuyer).map(Buyer::getUid)
                                ).filter(Optional::isPresent).findFirst().orElse(Optional.empty()).orElse(null)
                        )
                )
                .param("platform",
                        Objects.toString(ofNullable(parameters.configuration().cart().request().getPlatform())
                                .orElse(Platform.UNKNOWN)))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content);

        String responseJson = perform(parameters, requestBuilder);
        MultiCart multiCart = testSerializationService.deserializeCheckouterObject(responseJson, MultiCart.class);
        ofNullable(parameters.configuration().cart().response().getMultiCartChecker())
                .ifPresent(multiCartConsumer -> multiCartConsumer.accept(multiCart));
        return multiCart;
    }

    private MultiCart cartFake(ReportConfigurer reportConfigurer, MultiCart originalMultiCart, List<Order> orders,
                               Parameters parameters, HttpHeaders headers) throws Exception {
        hackOrderVat(orders, parameters); //эта хрень тут из-за неконфигурируемости OrderItemProvider'а
        initializeMockFake(reportConfigurer, parameters);

        String content = testSerializationService.serializeCheckouterObject(originalMultiCart);
        MockHttpServletRequestBuilder requestBuilder = post("/cart-fake")
                .param("clientRole", ClientRole.USER.name())
                .param("uid",
                        Objects.toString(
                                Stream.of(
                                        Optional.ofNullable(originalMultiCart).map(MultiCart::getBuyer)
                                                .map(Buyer::getUid),
                                        orders.stream().findFirst().map(Order::getBuyer).map(Buyer::getUid),
                                        Optional.of(parameters).map(Parameters::getBuyer).map(Buyer::getUid)
                                ).filter(Optional::isPresent).findFirst().orElse(Optional.empty()).orElse(null)
                        )
                )
                .param("platform",
                        Objects.toString(ofNullable(parameters.configuration().cart().request().getPlatform())
                                .orElse(Platform.UNKNOWN)))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content);

        String responseJson = perform(parameters, requestBuilder);
        MultiCart multiCart = testSerializationService.deserializeCheckouterObject(responseJson, MultiCart.class);
        ofNullable(parameters.configuration().cart().response().getMultiCartChecker())
                .ifPresent(multiCartConsumer -> multiCartConsumer.accept(multiCart));
        return multiCart;
    }


    public MultiCart multiCartActualizeWithMapToMultiCart(Parameters parameters) {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        MultiCartResponse multiCartResponse = multiCartActualize(reportConfigurerAuto, parameters);
        return new MultiCartResponseProvider().toMultiCart(multiCartResponse, parameters);
    }

    public MultiCartResponse multiCartActualize(Parameters parameters) {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        return multiCartActualize(reportConfigurerAuto, parameters);
    }

    public MultiCartResponse multiCartActualize(Parameters parameters, HttpHeaders headers) {
        try {
            ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
            return multiCartActualize(reportConfigurerAuto, parameters, headers);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public MultiCartResponse multiCartActualize(ReportConfigurer reportConfigurer, Parameters parameters) {
        try {
            return multiCartActualize(reportConfigurer, parameters, buildHeaders(parameters));
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    private MultiCartResponse multiCartActualize(ReportConfigurer reportConfigurer, Parameters parameters,
                                                 HttpHeaders headers) throws Exception {

        MultiCartRequest multiCartRequest = MultiCartRequestProvider.fromMultiCart(
                parameters.configuration().cart().request(),
                parameters.getBuiltMultiCart());
        hackOrderVat(parameters); //эта хрень тут из-за неконфигурируемости OrderItemProvider'а
        initializeMock(reportConfigurer, parameters);

        String content = testSerializationService.serializeCheckouterObject(multiCartRequest);
        MockHttpServletRequestBuilder requestBuilder = post("/v2/multicart/actualize")
                .param("platform",
                        Objects.toString(ofNullable(parameters.configuration().cart().request().getPlatform())
                                .orElse(Platform.UNKNOWN)))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content);


        String responseJson = perform(parameters, requestBuilder);
        return testSerializationService.deserializeCheckouterObject(responseJson, MultiCartResponse.class);
    }

    private String perform(Parameters parameters, MockHttpServletRequestBuilder requestBuilder) throws Exception {
        buildParameters(parameters, requestBuilder);

        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andExpect(status().is(parameters.configuration().cart().response().getExpectedCartReturnCode()));

        if (parameters.configuration().cart().response().checkCartErrors()) {
            try {
                resultActions
                        .andExpect(jsonPath("$.carts[*].changes").value(empty()))
                        .andExpect(jsonPath("$.carts[*].items[*].changes").value(empty()))
                        .andExpect(jsonPath("$.carts[*].validationErrors").doesNotExist())
                        .andExpect(jsonPath("$.validationErrors").doesNotExist());
            } catch (Exception e) {
                // логируем запрос/ответ только в случае ошибки
                resultActions
                        .andDo(CheckouterMockMvcResultHandlers.log());

            }
        }

        parameters.cartResultActions().propagateResultActions(resultActions);

        MvcResult mvcResult = resultActions.andReturn();

        if (parameters.configuration().cart().response().isExceptionExpected()) {
            Class<? extends Exception> expected = parameters.configuration().cart().response().getExpectedException();
            Class<? extends Exception> actual = extractExceptionClass(mvcResult);
            assertEquals(expected, actual);
        }

        return mvcResult.getResponse().getContentAsString();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void buildParameters(Parameters parameters, MockHttpServletRequestBuilder requestBuilder) {
        if (parameters.configuration().cart().request().isSandbox()) {
            requestBuilder.param("sandbox", "true").param("context", "SANDBOX");
        }

        if (parameters.configuration().cart().request().isReducePictures()) {
            requestBuilder.param(REDUCE_PICTURES, "true");
        }

        if (!parameters.configuration().cart().request().getPriceDropMskuSet().isEmpty()) {
            requestBuilder.param(CheckouterClientParams.PRICE_DROP_MSKU_LIST,
                    StringListUtils.toString(
                            parameters.configuration().cart().request().getPriceDropMskuSet(),
                            ",",
                            String::valueOf
                    )
            );
        }

        if (parameters.configuration().cart().request().getContext() != null) {
            requestBuilder.param(CheckouterClientParams.CONTEXT,
                    parameters.configuration().cart().request().getContext().name());
        }

        if (parameters.configuration().cart().request().getApiSettings() != null) {
            requestBuilder.param(CheckouterClientParams.API_SETTINGS,
                    parameters.configuration().cart().request().getApiSettings().name());
        }

        if (parameters.configuration().cart().request().getColor() != null) {
            requestBuilder.param(CheckouterClientParams.RGB,
                    parameters.configuration().cart().request().getColor().name());
        }

        if (parameters.configuration().cart().request().isMinifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.MINIFY_OUTLETS,
                    Boolean.toString(parameters.configuration().cart().request().isMinifyOutlets()));
        }

        if (parameters.configuration().cart().request().isSimplifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.SIMPLIFY_OUTLETS,
                    Boolean.toString(parameters.configuration().cart().request().isSimplifyOutlets()));
        }

        if (StringUtils.isNotEmpty(parameters.configuration().cart().request().getExperiments())) {
            requestBuilder.header(X_EXPERIMENTS, parameters.configuration().cart().request().getExperiments());
        }

        if (StringUtils.isNotEmpty(parameters.configuration().cart().request().getTestBuckets())) {
            requestBuilder.header(X_TEST_BUCKETS, parameters.configuration().cart().request().getTestBuckets());
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isUserHasPrime())) {
            requestBuilder.param(CheckouterClientParams.PRIME,
                    Boolean.toString(parameters.configuration().cart().request().isUserHasPrime()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isYandexPlus())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_PLUS,
                    Boolean.toString(parameters.configuration().cart().request().isYandexPlus()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isYandexEmployee())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_EMPLOYEE,
                    Boolean.toString(parameters.configuration().cart().request().isYandexEmployee()));
        }

        if (parameters.configuration().cart().request().getPerkPromoId() != null) {
            requestBuilder.param(CheckouterClientParams.PERK_PROMO_ID,
                    parameters.configuration().cart().request().getPerkPromoId());
        }

        if (parameters.configuration().cart().request().isShowCredits()) {
            requestBuilder.param(CheckouterClientParams.SHOW_CREDITS,
                    Boolean.toString(parameters.configuration().cart().request().isShowCredits()));
        }

        if (parameters.configuration().cart().request().isShowCreditBroker()) {
            requestBuilder.param(CheckouterClientParams.SHOW_CREDIT_BROKER,
                    Boolean.toString(parameters.configuration().cart().request().isShowCreditBroker()));
        }

        if (parameters.configuration().cart().request().isShowInstallments()) {
            requestBuilder.param(CheckouterClientParams.SHOW_INSTALLMENTS,
                    Boolean.toString(parameters.configuration().cart().request().isShowInstallments()));
        }

        if (parameters.configuration().cart().request().isShowSbp()) {
            requestBuilder.param(CheckouterClientParams.SHOW_SBP,
                    Boolean.toString(parameters.configuration().cart().request().isShowSbp()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isDebugAllCourierOptions())) {
            requestBuilder.param(CheckouterClientParams.DEBUG_ALL_COURIER_OPTIONS, Boolean.TRUE.toString());
        }

        if (parameters.configuration().cart().request().getForceShipmentDay() != null) {
            requestBuilder.param(CheckouterClientParams.FORCE_SHIPMENT_DAY,
                    String.valueOf(parameters.configuration().cart().request().getForceShipmentDay()));
        }

        if (parameters.configuration().cart().request().getSkipDiscountCalculation() != null) {
            requestBuilder.param(CheckouterClientParams.SKIP_DISCOUNT_CALCULATION,
                    Boolean.toString(parameters.configuration().cart().request().getSkipDiscountCalculation()));
        }

        if (parameters.configuration().cart().request().getMockLoyaltyDegradation() != null) {
            requestBuilder.header(X_MOCK_LOYALTY_DEGRADATION,
                    Boolean.toString(parameters.configuration().cart().request().getMockLoyaltyDegradation()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().getShowMultiServiceIntervals())) {
            requestBuilder.param(CheckouterClientParams.SHOW_MULTI_SERVICE_INTERVALS, Boolean.TRUE.toString());
        }

        if (parameters.configuration().cart().request().getForceDeliveryId() != null) {
            requestBuilder.param(
                    CheckouterClientParams.FORCE_DELIVERY_ID,
                    parameters.configuration().cart().request().getForceDeliveryId().toString()
            );
        }

        if (parameters.configuration().cart().request().isShowVat()) {
            requestBuilder.param(CheckouterClientParams.SHOW_VAT,
                    Boolean.toString(parameters.configuration().cart().request().isShowVat()));
        }

        if (parameters.configuration().cart().request().isSpreadAlgorithmV2()) {
            requestBuilder.param(CheckouterClientParams.SPREAD_ALGORITHM_V2,
                    Boolean.toString(parameters.configuration().cart().request().isSpreadAlgorithmV2()));
        }

        requestBuilder.param(
                CheckouterClientParams.PERKS,
                parameters.configuration().cart().request().getPerks());

        if (CollectionUtils.isNonEmpty(parameters.configuration().cart().request().getBnplFeatures())) {
            requestBuilder.param(CheckouterClientParams.BNPL_FEATURES,
                    parameters.configuration().cart().request().getBnplFeatures().toArray(new String[]{}));
        }

        if (parameters.configuration().cart().request().isOptionalRulesEnabled()) {
            requestBuilder.param(CheckouterClientParams.IS_OPTIONAL_RULES_ENABLED,
                    Boolean.toString(parameters.configuration().cart().request().isOptionalRulesEnabled()));
        }

        if (parameters.configuration().cart().request().isCalculateOrdersSeparately()) {
            requestBuilder.param(CheckouterClientParams.CALCULATE_ORDERS_SEPARATELY,
                    Boolean.toString(parameters.configuration().cart().request().isCalculateOrdersSeparately()));
        }

    }

    private void hackOrderVat(List<Order> orders, Parameters parameters) {
        for (Order order : orders) {
            ReportGeneratorParameters reportParameters =
                    parameters.configuration().cart().mocks(order.getLabel()).getReportParameters();
            order.getItems().forEach(i -> i.setVat(reportParameters.getItemVat()));
        }
    }

    private void hackOrderVat(Parameters parameters) {
        for (Order order : parameters.getOrders()) {
            ReportGeneratorParameters reportParameters =
                    parameters.configuration().cart().mocks(order.getLabel()).getReportParameters();
            order.getItems().forEach(i -> i.setVat(reportParameters.getItemVat()));
        }
    }

    public void initializeMock(Parameters parameters) throws IOException {
        ReportConfigurer reportConfigurerAuto = coloredReportConfigurer.getBy(getColorByParameters(parameters));
        initializeMock(reportConfigurerAuto, parameters);
    }

    public void initializeMock(ReportConfigurer reportConfigurer, Parameters parameters) throws IOException {
        setupShopsMetadata(parameters);
        setupItemsShowInfo(parameters);
        initializeOrderMock(reportConfigurer, parameters);
    }

    public void initializeMockFake(ReportConfigurer reportConfigurer, Parameters parameters) throws IOException {
        setupShopsMetadata(parameters);
        initializeFakeOrderMock(reportConfigurer, parameters);
    }

    private void initializeOrderMock(ReportConfigurer reportConfigurer, Parameters parameters) throws IOException {
        initializeLoyaltyMock(parameters);
        GeocoderParameters geocoderParameters =
                parameters.configuration().cart().multiCartMocks().getGeocoderParameters();
        if (geocoderParameters.isAutoMock()) {
            geocoderConfigurer.mock(geocoderParameters);
        }

        boolean hasPriceDropParams = !parameters.configuration().cart().request().getPriceDropMskuSet().isEmpty();

        List<Pair<List<SSItem>, List<SSItemAmount>>> multiOrderStock = new ArrayList<>();
        List<Pair<List<SSItem>, List<SSItemAmount>>> multiPreorderStock = new ArrayList<>();

        for (Order order : parameters.getOrders()) {
            String cartLabel = order.getLabel();

            ReportGeneratorParameters generatorParameters = parameters
                    .configuration()
                    .cart()
                    .mocks(cartLabel)
                    .getReportParameters();

            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.OFFER_INFO)) {
                if (hasPriceDropParams) {
                    reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, generatorParameters,
                            b -> b.withQueryParam("promo-by-cart-mskus", equalTo(StringListUtils.toString(
                                    parameters.configuration().cart().request().getPriceDropMskuSet(),
                                    ",",
                                    String::valueOf
                            ))));
                } else {
                    reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, generatorParameters,
                            b -> b.withQueryParam("regset",
                                    Boolean.TRUE.equals(checkouterProperties.getRegSet2ByDefault())
                                            ? equalTo("2") : equalTo("1")));
                }
            }
            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.SHOP_INFO)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.SHOP_INFO, generatorParameters);
            }
            reportConfigurer.mockCurrencyConvert(
                    generatorParameters,
                    parameters.configuration().cart().mocks(cartLabel).getPushApiDeliveryResponses(),
                    parameters.getBuiltMultiCart().getBuyerCurrency()
            );
            // currency service always uses white report
            coloredReportConfigurer.getBy(Color.WHITE).mockCurrencyConvert(
                    generatorParameters,
                    generatorParameters.getActualDelivery(),
                    parameters.getBuiltMultiCart().getBuyerCurrency()
            );

            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.OUTLETS)) {
                reportConfigurer.mockOutlets(generatorParameters);
            }
            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.ACTUAL_DELIVERY)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, generatorParameters);
            }

            if (generatorParameters.getDeliveryRoute() != null &&
                    !generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.DELIVERY_ROUTE)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.DELIVERY_ROUTE, generatorParameters);
            }

            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.CREDIT_INFO)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.CREDIT_INFO, generatorParameters);
            }

            mockPushApi(parameters, order, cartLabel);
            mockStockStorage(parameters, multiOrderStock, multiPreorderStock, order, cartLabel);

            ReportGeneratorParameters deliveryRegionReportParameters = parameters
                    .configuration()
                    .cart()
                    .mocks(cartLabel)
                    .getDeliveryRegionReportParameters();

            if (deliveryRegionReportParameters != null
                    && !deliveryRegionReportParameters.getSkipAutoConfiguration()
                    .contains(MarketReportPlace.OFFER_INFO)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, deliveryRegionReportParameters);
            }
        }

        if (!multiOrderStock.isEmpty()) {
            mockMultiOrderStockStorageGetAvailableCount(multiOrderStock, false);
        }

        if (!multiPreorderStock.isEmpty()) {
            mockMultiOrderStockStorageGetAvailableCount(multiPreorderStock, true);
        }
    }

    private void initializeFakeOrderMock(ReportConfigurer reportConfigurer, Parameters parameters) throws IOException {
        initializeLoyaltyMock(parameters);
        GeocoderParameters geocoderParameters =
                parameters.configuration().cart().multiCartMocks().getGeocoderParameters();
        if (geocoderParameters.isAutoMock()) {
            geocoderConfigurer.mock(geocoderParameters);
        }

        List<Pair<List<SSItem>, List<SSItemAmount>>> multiOrderStock = new ArrayList<>();
        List<Pair<List<SSItem>, List<SSItemAmount>>> multiPreorderStock = new ArrayList<>();

        for (Order order : parameters.getOrders()) {
            String cartLabel = order.getLabel();

            ReportGeneratorParameters generatorParameters = parameters
                    .configuration()
                    .cart()
                    .mocks(cartLabel)
                    .getReportParameters();

            if (!generatorParameters.getSkipAutoConfiguration().contains(MarketReportPlace.SHOP_INFO)) {
                reportConfigurer.mockReportPlace(MarketReportPlace.SHOP_INFO, generatorParameters);
            }
            reportConfigurer.mockCurrencyConvert(
                    generatorParameters,
                    parameters.configuration().cart().mocks(cartLabel).getPushApiDeliveryResponses(),
                    parameters.getBuiltMultiCart().getBuyerCurrency()
            );

            mockFakeOrderPushApi(parameters, order, cartLabel);
            mockStockStorage(parameters, multiOrderStock, multiPreorderStock, order, cartLabel);
        }

        if (!multiOrderStock.isEmpty()) {
            mockMultiOrderStockStorageGetAvailableCount(multiOrderStock, false);
        }

        if (!multiPreorderStock.isEmpty()) {
            mockMultiOrderStockStorageGetAvailableCount(multiPreorderStock, true);
        }
    }

    private void mockPushApi(Parameters parameters, Order order, String cartLabel) {
        if (parameters.configuration().cart().multiCartMocks().mockPushApi()) {
            List<DeliveryResponse> deliveryResponses = parameters
                    .configuration()
                    .cart()
                    .mocks(cartLabel)
                    .getPushApiDeliveryResponses();
            OrderAcceptMethod acceptMethod = getOrderAcceptMethod(parameters, order);
            pushApiConfigurer.mockCart(order.getItems(), order.getShopId(), deliveryResponses,
                    acceptMethod, parameters.isMultiCart());
        }
    }

    private void mockFakeOrderPushApi(Parameters parameters, Order order, String cartLabel) {
        if (parameters.configuration().cart().multiCartMocks().mockPushApi()) {
            OrderAcceptMethod acceptMethod = getOrderAcceptMethod(parameters, order);

            List<DeliveryResponse> deliveryResponses = parameters
                    .configuration()
                    .cart()
                    .mocks(cartLabel)
                    .getPushApiDeliveryResponses();

            pushApiConfigurer.mockCart(order.getItems(), order.getShopId(), deliveryResponses,
                    acceptMethod, parameters.isMultiCart());
        }
    }

    private void initializeLoyaltyMock(Parameters parameters) throws IOException {
        if (parameters.configuration().cart().multiCartMocks().mockLoyalty()
                || parameters.getLoyaltyParameters().getCustomLoyaltyMockConfiguration() != null) {
            mockLoyalty(parameters);
        } else {
            mockLoyaltyNoDiscounts(parameters);
            mockLoyaltyDefaultCashback(parameters);
        }
        mockLoyaltyPromocodes(parameters);

        mockTrust(parameters);
        loyaltyConfigurer.mockPerkStatus(parameters);
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private void mockStockStorage(
            Parameters parameters,
            List<Pair<List<SSItem>, List<SSItemAmount>>> multiOrderStock,
            List<Pair<List<SSItem>, List<SSItemAmount>>> multiPreorderStock,
            Order order,
            String cartLabel
    ) {
        if (parameters.configuration().cart().mocks(cartLabel).shouldMockStockStorageGetAmountResponse()) {
            Pair<List<SSItem>, List<SSItemAmount>> singleOrderStock =
                    mockStockStorageGetAvailableCount(parameters, order);
            if (order.isPreorder() || order.getItems().stream().allMatch(OfferItem::isPreorder)) {
                multiPreorderStock.add(singleOrderStock);
            } else {
                multiOrderStock.add(singleOrderStock);
            }
        }

        switch (parameters.configuration().cart().mocks(cartLabel).getStockStorageFreezeMockType()) {
            case OK:
                stockStorageConfigurer.mockOkForFreeze();
                break;
            case PREORDER_OK:
                stockStorageConfigurer.mockOkForPreorderFreeze();
                break;
            case ERROR:
                stockStorageConfigurer.mockErrorForFreeze();
                break;
            case PREORDER_ERROR:
                stockStorageConfigurer.mockErrorForPreorderFreeze();
                break;
            case NO_STOCKS:
                stockStorageConfigurer.mockNoStocksForFreeze();
                break;
            case REQUEST_TIMEOUT:
                stockStorageConfigurer.mockRequestTimeoutForFreeze();
                break;
            case NO:
                break;
        }
    }

    private OrderAcceptMethod getOrderAcceptMethod(Parameters parameters, Order order) {
        OrderAcceptMethod acceptMethod = order.getAcceptMethod();
        if (parameters.isShopAdmin() != null) {
            acceptMethod = parameters.isShopAdmin() ? OrderAcceptMethod.WEB_INTERFACE :
                    OrderAcceptMethod.PUSH_API;
        }
        return acceptMethod;
    }

    private void mockTrust(Parameters parameters) throws IOException {
        Consumer<TrustMockConfigurer> customTrustMockConfiguration =
                parameters.getTrustParameters().getCustomTrustMockConfiguration();
        if (customTrustMockConfiguration != null) {
            customTrustMockConfiguration.accept(trustMockConfigurer);
        } else {
            trustMockConfigurer.mockListPaymentMethods();
        }
    }

    private void mockMultiOrderStockStorageGetAvailableCount(List<Pair<List<SSItem>, List<SSItemAmount>>> multiStocks,
                                                             boolean isPreorder) {
        stockStorageConfigurer.mockGetAvailableCount(
                multiStocks.stream().map(Pair::getFirst).flatMap(Collection::stream).collect(Collectors.toList()),
                isPreorder,
                multiStocks.stream().map(Pair::getSecond).flatMap(Collection::stream).collect(Collectors.toList()));
    }

    private Pair<List<SSItem>, List<SSItemAmount>> mockStockStorageGetAvailableCount(Parameters parameters,
                                                                                     Order order) {
        if (order.getItems().stream().allMatch(item -> item.getWarehouseId() == null)) {
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }
        List<SSItem> stockStorageRequest = order.getItems()
                .stream()
                .filter(oi -> {
                    ItemInfo orderItemsOverride =
                            parameters.configuration().cart()
                                    .mocks(order.getLabel())
                                    .getReportParameters()
                                    .getOrderItemsOverride()
                                    .get(oi.getFeedOfferId());
                    return orderItemsOverride == null || !orderItemsOverride.hideOffer();
                })
                .filter(oi -> oi.getCount() > 0)
                .collect(Collectors.toList()).stream()
                .map(oi -> {
                    ItemInfo itemInfo = parameters.configuration().cart()
                            .mocks(order.getLabel())
                            .getReportParameters()
                            .getOrderItemsOverride()
                            .get(oi.getFeedOfferId());
                    String shopSku = Optional.ofNullable(itemInfo).map(ItemInfo::getFulfilment)
                            .map(ItemInfo.Fulfilment::getShopSku).orElse(colorConfig.getFor(order).getShopSku(oi));
                    long supplierId = Optional.ofNullable(itemInfo).map(ItemInfo::getFulfilment)
                            .map(ItemInfo.Fulfilment::getSupplierId).orElse(oi.getSupplierId());
                    int warehouseId = Optional.ofNullable(itemInfo)
                            .map(ItemInfo::getFulfilment)
                            .map(ItemInfo.Fulfilment::getWarehouseId)
                            .orElseGet(() -> ObjectUtils.firstNonNull(oi.getWarehouseId(), 1));

                    return SSItem.of(shopSku, supplierId, warehouseId);
                })
                .peek(si -> LOG.info("Prepared stock storage request: {}", si))
                .collect(Collectors.toList());
        List<SSItemAmount> stockStorageResponse;
        if (parameters.configuration().cart().mocks(order.getLabel()).getStockStorageResponse() == null) {
            Map<SSItemKey, Integer> quantityMap = order.getItems()
                    .stream()
                    .filter(oi -> {
                        ItemInfo orderItemsOverride =
                                parameters.configuration().cart()
                                        .mocks(order.getLabel())
                                        .getReportParameters()
                                        .getOrderItemsOverride()
                                        .get(oi.getFeedOfferId());
                        return orderItemsOverride == null || !orderItemsOverride.hideOffer();
                    })
                    .filter(oi -> oi.getCount() > 0)
                    .collect(groupingBy(oi -> {
                        ItemInfo itemInfo = parameters.configuration().cart()
                                .mocks(order.getLabel())
                                .getReportParameters()
                                .getOrderItemsOverride()
                                .get(oi.getFeedOfferId());
                        String shopSku = Optional.ofNullable(itemInfo)
                                .map(ItemInfo::getFulfilment)
                                .map(ItemInfo.Fulfilment::getShopSku)
                                .orElse(colorConfig.getFor(order).getShopSku(oi));
                        long supplierId = Optional.ofNullable(itemInfo)
                                .map(ItemInfo::getFulfilment)
                                .map(ItemInfo.Fulfilment::getSupplierId)
                                .orElse(oi.getSupplierId());
                        int warehouseId = Optional.ofNullable(itemInfo)
                                .map(ItemInfo::getFulfilment)
                                .map(ItemInfo.Fulfilment::getWarehouseId)
                                .orElseGet(() -> ObjectUtils.firstNonNull(oi.getWarehouseId(), 1));

                        return SSItemKey.of(shopSku, supplierId, warehouseId, false);
                    }, Collectors.summingInt(OrderItem::getCount)));

            stockStorageResponse = quantityMap.entrySet().stream()
                    .map(entry -> SSItemAmount.of(SSItem.of(
                            entry.getKey().getShopSku(),
                            entry.getKey().getVendorId(),
                            entry.getKey().getWarehouseId()
                    ), entry.getValue()))
                    .collect(Collectors.toList());
        } else {
            stockStorageResponse = parameters.configuration().cart().mocks(order.getLabel()).getStockStorageResponse();
        }
        boolean preorder = order.isPreorder() || order.getItems().stream().anyMatch(OrderItem::isPreorder);
        stockStorageConfigurer.mockGetAvailableCount(stockStorageRequest, preorder, stockStorageResponse);
        return Pair.of(stockStorageRequest, stockStorageResponse);
    }

    private void setupItemsShowInfo(Parameters parameters) {
        parameters.getBuiltMultiCart().getCarts()
                .forEach(order -> {
                    var reportParameters = parameters.configuration().cart()
                            .mocks(order.getLabel())
                            .getReportParameters();
                    order.getItems().forEach(item -> updateShowFeeFor(reportParameters, item,
                            parameters.configuration().cart().body().isSkipShowInfoAdjusting()));
                });
    }

    private void updateShowFeeFor(
            ReportGeneratorParameters reportParameters,
            OrderItem item,
            boolean isSkipShowInfoAdjusting
    ) {
        ItemInfo itemInfo = reportParameters.overrideItemInfo(item.getFeedOfferId());
        String wareMd5 = defaultIfNull(itemInfo.getWareMd5(), item.getWareMd5());
        String feeShow = defaultIfNull(item.getShowInfo(), SHOW_INFO);
        Integer pp = defaultIfNull(item.getPp(), 1000);

        if (wareMd5 == null || isSkipShowInfoAdjusting) {
            return;
        }

        CpaContextRecord cpaContextRecord = cpaRecord(
                feeShow,
                item.getFee(),
                item.getFeeSum(),
                item.getShowUid(),
                wareMd5,
                pp,
                reportCipherService
        );

        item.setFee(new BigDecimal(cpaContextRecord.getFee()));
        item.setFeeSum(new BigDecimal(cpaContextRecord.getFeeSum()));
        item.setShowUid(cpaContextRecord.getShowUid());

        item.setShowInfo(generateShowInfo(cpaContextRecord, reportCipherService));
        var cpaContextRecordForCartShow = cpaRecord(
                feeShow,
                item.getFee(),
                item.getFeeSum(),
                item.getCartShowUid(),
                wareMd5,
                pp,
                reportCipherService
        );
        item.setCartShowInfo(generateShowInfo(cpaContextRecordForCartShow, reportCipherService));
    }

    private void mockLoyaltyNoDiscounts(Parameters parameters) {
        Map<OfferItemKey, List<LoyaltyDiscount>> loyaltyDiscountsByOfferId =
                parameters.getLoyaltyParameters().getLoyaltyDiscountsByOfferId();
        parameters.getOrders().stream()
                .filter(Objects::nonNull)
                .flatMap(o -> o.getItems().stream())
                .filter(Objects::nonNull)
                .distinct()
                .forEach(i -> loyaltyDiscountsByOfferId.put(i.getOfferItemKey(), Collections.emptyList()));
        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);
    }

    private void mockLoyaltyPromocodes(Parameters parameters) {
        final List<PromocodeDiscountEntry> promocodeDiscountEntries =
                parameters.getLoyaltyParameters().getPromocodeDiscountEntries();

        final List<PromocodeActivationResult> promocodeActivationResults = promocodeDiscountEntries.stream()
                .filter(pe -> pe.getPromoType() == PromoType.MARKET_PROMOCODE)
                .map(pe -> PromocodeActivationResult.of(pe.getPromocode(), pe.getActivationResultCode()))
                .collect(Collectors.toUnmodifiableList());
        if (!promocodeActivationResults.isEmpty()) {
            loyaltyConfigurer.mockPromocodeActivationResponse(promocodeActivationResults);
        } else if (StringUtils.isNotEmpty(parameters.configuration().cart().body().multiCart().getPromoCode())) {
            loyaltyConfigurer.mockPromocodeActivationResponse(List.of(PromocodeActivationResult.of(
                    parameters.configuration().cart().body().multiCart().getPromoCode(),
                    PromocodeActivationResultCode.NOT_COMPATIBLE_PROMO)));
        } else {
            loyaltyConfigurer.mockPromocodeActivationResponse(List.of());
        }
    }

    private void mockLoyaltyDefaultCashback(Parameters parameters) {
        parameters.getLoyaltyParameters().setExpectedCashbackOptionsResponse(
                new CashbackOptionsResponse(Collections.emptyList()));
        loyaltyConfigurer.mockCashbackOptions(parameters);
    }

    private void mockLoyalty(Parameters parameters) {
        Consumer<LoyaltyConfigurer> customLoyaltyMockConfiguration = parameters.getLoyaltyParameters()
                .getCustomLoyaltyMockConfiguration();
        if (customLoyaltyMockConfiguration != null) {
            customLoyaltyMockConfiguration.accept(loyaltyConfigurer);
        } else {
            loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);
            loyaltyConfigurer.mockCashbackOptions(parameters);
            loyaltyConfigurer.mockSuccessRevert();
            loyaltyConfigurer.mockPerkBuy(new BuyPerkResponse.AllOk());
        }
    }

    public MultiOrder checkout(MultiCart multiCart, Parameters parameters) throws Exception {
        return checkout(multiCart, parameters, buildHeaders(parameters));
    }

    public MultiOrder checkoutFake(MultiCart multiCart, Parameters parameters) throws Exception {
        return checkoutFake(multiCart, parameters, buildHeaders(parameters));
    }

    public MultiOrder checkout(MultiCart multiCart, Parameters parameters, HttpHeaders headers) throws Exception {
        MultiOrder multiOrder = mapCartToOrder(multiCart, parameters);
        if (parameters.configuration().checkout().response().getPrepareBeforeCheckoutAction() != null) {
            parameters.configuration().checkout().response().getPrepareBeforeCheckoutAction().accept(multiOrder);
        }
        return checkout(multiOrder, parameters, headers);
    }

    public MultiOrder checkoutFake(MultiCart multiCart, Parameters parameters, HttpHeaders headers) throws Exception {
        MultiOrder multiOrder = mapCartToOrder(multiCart, parameters);
        if (parameters.configuration().checkout().response().getPrepareBeforeCheckoutAction() != null) {
            parameters.configuration().checkout().response().getPrepareBeforeCheckoutAction().accept(multiOrder);
        }
        return checkoutFake(multiOrder, parameters, headers);
    }

    public MultiOrder checkout(MultiOrder multiOrder, Parameters parameters) throws Exception {
        return checkout(multiOrder, parameters, buildHeaders(parameters));
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public MultiOrder checkoutFake(MultiOrder multiOrder, Parameters parameters, HttpHeaders headers) throws Exception {
        mockPushApi(multiOrder, parameters);

        MultiOrder createdMultiOrder = makeCheckoutRequest(multiOrder, parameters, headers, "/checkout-fake");
        ofNullable(parameters.configuration().checkout().response().getMultiOrderChecker())
                .ifPresent(multiOrderConsumer -> multiOrderConsumer
                        .accept(createdMultiOrder));
        return createdMultiOrder;
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public MultiOrder checkout(MultiOrder multiOrder, Parameters parameters, HttpHeaders headers) throws Exception {
        mockPushApi(multiOrder, parameters);

        MultiOrder createdMultiOrder = makeCheckoutRequest(multiOrder, parameters, headers, "/checkout");
        ofNullable(parameters.configuration().checkout().response().getMultiOrderChecker())
                .ifPresent(multiOrderConsumer -> multiOrderConsumer
                        .accept(createdMultiOrder));
        return createdMultiOrder;
    }

    private void mockPushApi(MultiOrder multiOrder, Parameters parameters) {
        if (parameters.configuration().cart().multiCartMocks().mockPushApi()) {
            if (multiOrder.getCarts() != null) {
                for (Order order : multiOrder.getCarts()) {
                    pushApiConfigurer.mockAccept(order,
                            parameters.configuration().checkout().mocks(order.getLabel()).isAcceptOrder());

                    List<DeliveryResponse> deliveryResponses = parameters
                            .configuration()
                            .cart()
                            .mocks(order.getLabel())
                            .getPushApiDeliveryResponses();

                    pushApiConfigurer.mockCart(order, deliveryResponses, parameters.isMultiCart());
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private MultiOrder makeCheckoutRequest(
            MultiOrder multiOrder,
            Parameters parameters,
            HttpHeaders headers,
            String checkoutUrlTemplate
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(checkoutUrlTemplate)
                .param("clientRole", ClientRole.USER.name())
                .param("uid", Objects.toString(parameters.getBuyer().getUid()))
                .param("platform",
                        Objects.toString(ofNullable(parameters.configuration().cart().request().getPlatform())
                                .orElse(Platform.UNKNOWN)))
                .param("reserveOnly", String.valueOf(parameters.configuration().checkout().request()
                        .isReserveOnly()))
                .param("asyncPaymentCardId", parameters.configuration().checkout().request()
                        .getAsyncPaymentCardId())
                .param("loginId", parameters.configuration().checkout().request().getLoginId())
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(multiOrder));

        if (!parameters.configuration().cart().request().getPriceDropMskuSet().isEmpty()) {
            requestBuilder.param(CheckouterClientParams.PRICE_DROP_MSKU_LIST,
                    StringListUtils.toString(
                            parameters.configuration().cart().request().getPriceDropMskuSet(),
                            ",",
                            String::valueOf
                    )
            );
        }

        if (parameters.configuration().cart().request().getContext() != null) {
            requestBuilder.param(CheckouterClientParams.CONTEXT,
                    parameters.configuration().cart().request().getContext().name());
        }

        if (parameters.configuration().cart().request().getApiSettings() != null) {
            requestBuilder.param(CheckouterClientParams.API_SETTINGS,
                    parameters.configuration().cart().request().getApiSettings().name());
        }

        if (parameters.configuration().cart().request().isSandbox()) {
            requestBuilder.param("sandbox", "true").param("context", "SANDBOX");
        }

        if (parameters.configuration().cart().request().isReducePictures()) {
            requestBuilder.param(REDUCE_PICTURES, "true");
        }

        if (parameters.configuration().cart().request().getUserGroup() != null) {
            requestBuilder.param(CheckouterClientParams.USER_GROUP,
                    String.valueOf(parameters.configuration().cart().request().getUserGroup()));
        }

        if (parameters.configuration().cart().request().getColor() != null) {
            requestBuilder.param(CheckouterClientParams.RGB,
                    parameters.configuration().cart().request().getColor().name());
        }

        if (parameters.configuration().cart().request().isMinifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.MINIFY_OUTLETS,
                    Boolean.toString(parameters.configuration().cart().request().isMinifyOutlets()));
        }

        if (parameters.configuration().cart().request().isSimplifyOutlets()) {
            requestBuilder.param(CheckouterClientParams.SIMPLIFY_OUTLETS,
                    Boolean.toString(parameters.configuration().cart().request().isSimplifyOutlets()));
        }

        if (StringUtils.isNotEmpty(parameters.configuration().cart().request().getExperiments())) {
            requestBuilder.header(X_EXPERIMENTS, parameters.configuration().cart().request().getExperiments());
        }

        if (StringUtils.isNotEmpty(parameters.configuration().cart().request().getTestBuckets())) {
            requestBuilder.header(X_TEST_BUCKETS, parameters.configuration().cart().request().getTestBuckets());
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isYandexEmployee())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_EMPLOYEE,
                    Boolean.toString(parameters.configuration().cart().request().isYandexEmployee()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().isYandexPlus())) {
            requestBuilder.param(CheckouterClientParams.YANDEX_PLUS,
                    Boolean.toString(parameters.configuration().cart().request().isYandexPlus()));
        }

        if (parameters.configuration().cart().request().getForceShipmentDay() != null) {
            requestBuilder.param(CheckouterClientParams.FORCE_SHIPMENT_DAY,
                    String.valueOf(parameters.configuration().cart().request().getForceShipmentDay()));
        }

        if (parameters.configuration().cart().request().getSkipDiscountCalculation() != null) {
            requestBuilder.param(CheckouterClientParams.SKIP_DISCOUNT_CALCULATION,
                    Boolean.toString(parameters.configuration().cart().request().getSkipDiscountCalculation()));
        }

        if (parameters.configuration().cart().request().getMockLoyaltyDegradation() != null) {
            requestBuilder.header(X_MOCK_LOYALTY_DEGRADATION,
                    Boolean.toString(parameters.configuration().cart().request().getMockLoyaltyDegradation()));
        }

        if (Boolean.TRUE.equals(parameters.configuration().cart().request().getShowMultiServiceIntervals())) {
            requestBuilder.param(CheckouterClientParams.SHOW_MULTI_SERVICE_INTERVALS, "true");
        }

        if (parameters.configuration().cart().request().getForceDeliveryId() != null) {
            requestBuilder.param(
                    CheckouterClientParams.FORCE_DELIVERY_ID,
                    parameters.configuration().cart().request().getForceDeliveryId().toString()
            );
        }

        requestBuilder.param(
                CheckouterClientParams.PERKS,
                parameters.configuration().cart().request().getPerks());

        if (parameters.configuration().cart().request().isShowCredits()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_CREDITS,
                    "true"
            );
        }

        if (parameters.configuration().cart().request().isShowCreditBroker()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_CREDIT_BROKER,
                    "true"
            );
        }

        if (parameters.configuration().cart().request().isShowInstallments()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_INSTALLMENTS,
                    "true"
            );
        }

        if (parameters.configuration().cart().request().isShowSbp()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_SBP,
                    "true"
            );
        }

        if (parameters.configuration().cart().request().isShowVat()) {
            requestBuilder.param(
                    CheckouterClientParams.SHOW_VAT,
                    "true"
            );
        }

        if (CollectionUtils.isNonEmpty(parameters.configuration().cart().request().getBnplFeatures())) {
            requestBuilder.param(CheckouterClientParams.BNPL_FEATURES,
                    parameters.configuration().cart().request().getBnplFeatures().toArray(new String[]{}));
        }

        if (parameters.configuration().cart().request().isCalculateOrdersSeparately()) {
            requestBuilder.param(CheckouterClientParams.CALCULATE_ORDERS_SEPARATELY,
                    Boolean.toString(parameters.configuration().cart().request().isCalculateOrdersSeparately()));
        }

        if (parameters.configuration().cart().request().isOptionalRulesEnabled()) {
            requestBuilder.param(CheckouterClientParams.IS_OPTIONAL_RULES_ENABLED,
                    Boolean.toString(parameters.configuration().cart().request().isOptionalRulesEnabled()));
        }

        parameters.configuration().checkout().request().getHeaders().forEach(requestBuilder::header);

        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(parameters.configuration().checkout().response()
                        .getExpectedCheckoutReturnCode()));

        if (parameters.configuration().checkout().response().checkOrderCreateErrors()) {
            resultActions
                    .andExpect(jsonPath("$.orders[*].changes").value(empty()))
                    .andExpect(jsonPath("$.orders[*].items[*].changes").value(empty()))
                    .andExpect(jsonPath("$.orders[*].validationErrors").value(empty()))
                    .andExpect(jsonPath("$.validationErrors").doesNotExist())
                    .andExpect(jsonPath("$.checkedOut").value(true));
        } else if (parameters.configuration().checkout().response().useErrorMatcher()) {
            if (parameters.configuration().checkout().response().getErrorMatcher() != null) {
                resultActions.andExpect(parameters.configuration().checkout().response().getErrorMatcher());
            } else {
                resultActions.andExpect(jsonPath("$.checkedOut").value(false));
            }
        }
        parameters.checkoutResultActions().propagateResultActions(resultActions);

        MvcResult mvcResult = resultActions.andReturn();

        if (parameters.configuration().checkout().response().isExceptionExpected()) {
            Class<? extends Exception> expected =
                    parameters.configuration().checkout().response().getExpectedException();
            Class<? extends Exception> actual = extractExceptionClass(mvcResult);
            assertEquals(expected, actual);
        }

        String responseJson = mvcResult.getResponse().getContentAsString();
        return testSerializationService.deserializeCheckouterObject(responseJson, MultiOrder.class);
    }

    public void setupShopsMetadata(Parameters parameters) {
        if (parameters.getShopMetaData().isEmpty()) {
            for (Order order : parameters.getOrders()) {
                shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());
                setupFulfillmentShops(order, parameters);
                setupNotFulfilmentShops(order, parameters);
                setUpShipmentDateCalculationRules(order);
            }
        } else {
            parameters.getShopMetaData()
                    .forEach((shopId, shopMetaData) -> shopService.updateMeta(shopId, shopMetaData));
        }
    }

    private void setupFulfillmentShops(Order order, Parameters parameters) {
        order.getItems().stream()
                .map(item -> avoidNull(
                        parameters.configuration().cart().mocks(order.getLabel())
                                .getReportParameters()
                                .overrideItemInfo(item.getFeedOfferId())
                                .getFulfilment()
                                .supplierId,
                        item.getSupplierId()
                ))
                .forEach(supplierId -> shopService.updateMeta(supplierId,
                        ShopSettingsHelper.createCustomNewPrepayMeta(supplierId.intValue())));
    }

    private void setupNotFulfilmentShops(Order order, Parameters parameters) {
        if (parameters.configuration().cart().request().getColor() == Color.BLUE
                && Boolean.FALSE.equals(order.isFulfilment())) {
            Long shopId = order.getShopId();
            Long businessId = order.getBusinessId();
            shopService.updateMeta(shopId,
                    ShopSettingsHelper.createCustomNotFulfilmentMeta(shopId.intValue(), businessId));
        }
    }

    private void setUpShipmentDateCalculationRules(Order order) {
        if (order.isSelfDelivery()) {
            shipmentDateCalculationRuleService.save(order.getShopId(),
                    ShipmentDateCalculationRule.builder()
                            .withHourBefore(13)
                            .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                                    .withDaysToAdd(1)
                                    .withBaseDateForCalculation(ORDER_CREATION_DATE)
                                    .build())
                            .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                                    .withDaysToAdd(-1)
                                    .withBaseDateForCalculation(DELIVERY_DATE)
                                    .build())
                            .withHolidays(singletonList(LocalDate.of(2020, 5, 13)))
                            .build());
        }
    }

    public MultiOrder mapCartToOrder(MultiCart multiCart, Parameters parameters) {
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyer(parameters.getBuyer());
        multiOrder.setBuyerCurrency(parameters.getBuiltMultiCart().getBuyerCurrency());
        multiOrder.setBuyerRegionId(parameters.getBuiltMultiCart().getBuyerRegionId());
        if (multiCart.getCarts().stream().anyMatch(c -> c.getPaymentMethod() == null)) {
            multiOrder.setPaymentMethod(parameters.getBuiltMultiCart().getPaymentMethod());
            multiOrder.setPaymentType(parameters.getBuiltMultiCart().getPaymentType());
            multiOrder.setBnplInfo(parameters.getBuiltMultiCart().getBnplInfo());
            multiOrder.setInstallmentsInfo(parameters.getBuiltMultiCart().getInstallmentsInfo());
            multiOrder.setCreditInformation(parameters.getBuiltMultiCart().getCreditInformation());
        }
        if (multiOrder.getBnplInfo() != multiCart.getBnplInfo()) {
            multiOrder.setBnplInfo(multiCart.getBnplInfo());
        }
        if (multiOrder.getCardInfo() != multiCart.getCardInfo()) {
            multiOrder.setCardInfo(multiCart.getCardInfo());
        }
        if (multiCart.getCarts() != null) {
            multiOrder.setOrders(multiCart.getCarts());
            for (Order cart : multiCart.getCarts()) {
                var originalCart = parameters.getBuiltMultiCart().getCartByLabel(cart.getLabel());
                cart.setBusinessId(originalCart.get().getBusinessId());
            }
        }
        multiOrder.setPromoCode(parameters.getPromoCode());
        multiOrder.useInternalPromoCode(multiCart.isUseInternalPromoCode());
        if (parameters.configuration().cart().request().getUnfreezeStocksTime() != null) {
            multiOrder.getOrders().forEach(
                    order -> {
                        order.setProperty(UNFREEZE_STOCKS_TIME,
                                parameters.configuration().cart().request().getUnfreezeStocksTime());
                    }
            );
        }
        multiOrder.setSelectedCashbackOption(multiCart.getSelectedCashbackOption());

        mapItems(parameters, multiOrder);
        mapDeliveryOptions(multiCart, parameters, multiOrder);

        return multiOrder;
    }

    private void mapItems(Parameters parameters, MultiOrder multiOrder) {
        Map<String, Order> parametersOrderMap = parameters.getOrders().stream()
                .collect(Collectors.toMap(Order::getLabel, Function.identity()));

        multiOrder.getCarts().forEach(cart -> {
            Order paramOrder = requireNonNull(parametersOrderMap.get(cart.getLabel()));

            cart.getItems().forEach(orderItem -> {
                for (OrderItem builtItem : paramOrder.getItems(orderItem.getFeedOfferId())) {
                    orderItem.setShowInfo(builtItem.getShowInfo());
                    orderItem.setPrice(builtItem.getPrice());
                    orderItem.setQuantPrice(builtItem.getQuantPrice());
                    orderItem.setBuyerPrice(builtItem.getBuyerPrice());
                    orderItem.setWareMd5(builtItem.getWareMd5());
                    orderItem.setVat(builtItem.getVat());
                }
            });
        });
    }

    /**
     * @param sourceMultiCart ответ на /cart
     * @param parameters      параметры
     * @param targetCart      targetCart
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public void mapDeliveryOptions(MultiCart sourceMultiCart, Parameters parameters, MultiCart targetCart) {
        for (Order cart : sourceMultiCart.getCarts()) {
            List<? extends Delivery> deliveryOptions = cart.getDeliveryOptions();
            Order order = targetCart.getCarts().stream()
                    .filter(o -> Objects.equals(o.getLabel(), cart.getLabel()))
                    .findFirst()
                    .orElse(null);

            Order paramOrder = parameters.getOrders().stream()
                    .filter(o -> Objects.equals(o.getLabel(), cart.getLabel()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unknown order " + cart));
            Optional<? extends Delivery> delivery = getSuitableDeliveryOptions(
                    parameters.configuration().checkout().orderOption(cart.getLabel()),
                    deliveryOptions
            ).findFirst();
            if (delivery.isEmpty()) {
                throw new RuntimeException(
                        String.format("No delivery options matching: type=%s, " +
                                        "getDeliveryPartnerType=%s, getDeliveryServiceId=%s, isFree=%s, " +
                                        "paymentMethod=%s, fromDate=%s, leaveAtTheDoor=%s",
                                parameters.configuration().checkout().orderOption(cart.getLabel()).getDeliveryType(),
                                parameters.configuration().checkout().orderOption(cart.getLabel())
                                        .getDeliveryPartnerType(),
                                parameters.configuration().checkout().orderOption(cart.getLabel())
                                        .getDeliveryServiceId(),
                                parameters.configuration().checkout().orderOption(cart.getLabel()).isFreeDelivery(),
                                parameters.getBuiltMultiCart().getPaymentMethod(),
                                parameters.configuration().checkout().orderOption(cart.getLabel()).getFromDate(),
                                parameters.configuration().checkout().orderOption(cart.getLabel()).getLeaveAtTheDoor()
                        )
                );
            }
            order.setDelivery(delivery.get());
            order.getDelivery().setRecipient(paramOrder.getDelivery().getRecipient());
            order.getDelivery().setBusinessRecipient(paramOrder.getDelivery().getBusinessRecipient());
            switch (order.getDelivery().getType()) {
                case DELIVERY:
                case POST:
                    order.getDelivery().setBuyerAddress(paramOrder.getDelivery().getBuyerAddress());
                    break;
                case PICKUP:
                    if (parameters.configuration().checkout().orderOption(cart.getLabel()).getOutletId() == null) {
                        if (parameters.configuration().cart().request().isSimplifyOutlets()) {
                            order.getDelivery().setOutletId(order.getDelivery().getOutletIdsSet().stream().findFirst()
                                    .get());
                        } else {
                            order.getDelivery().setOutletId(order.getDelivery().getOutlets().get(0).getId());
                        }
                    } else if (parameters.configuration().checkout().orderOption(cart.getLabel())
                            .getOutletId() != null) {
                        order.getDelivery().setOutletId(parameters.configuration().checkout()
                                .orderOption(cart.getLabel()).getOutletId());
                    }
                    break;
            }
            order.getDelivery().setRegionId(targetCart.getBuyerRegionId());
        }
    }

    public Stream<? extends Delivery> getSuitableDeliveryOptions(
            CheckoutOptionParameters parameters,
            List<? extends Delivery> deliveryOptions
    ) {
        return deliveryOptions.stream()
                .filter(option -> isSameType(parameters, option))
                .filter(option -> isSameDeliveryPartnerType(parameters, option))
                .filter(option -> isSameDeliveryServiceId(parameters, option))
                .filter(option -> isSameFree(parameters, option))
                .filter(option -> isSameDateFrom(parameters, option))
                .filter(option -> isSameLeaveAtTheDoor(parameters, option))
                .peek(option -> {
                    if (!option.getRawDeliveryIntervals().isEmpty()) {
                        Set<RawDeliveryInterval> intervalEntry = Iterables.get(
                                option.getRawDeliveryIntervals().getCollection().values(),
                                0
                        );
                        RawDeliveryInterval interval = Iterables.get(intervalEntry, 0);
                        option.getDeliveryDates().setFromDate(interval.getDate());
                        option.getDeliveryDates().setToDate(interval.getDate());
                        option.getDeliveryDates().setFromTime(interval.getFromTime());
                        option.getDeliveryDates().setToTime(interval.getToTime());
                    }
                });
    }

    private boolean isSamePaymentType(PaymentMethod paymentMethod, Delivery option) {
        return option.getPaymentOptions().contains(paymentMethod);
    }

    private boolean isSameFree(CheckoutOptionParameters parameters, Delivery option) {
        return option.isFree() == avoidNull(parameters.isFreeDelivery(), option.isFree());
    }

    private boolean isSameDeliveryServiceId(CheckoutOptionParameters parameters, Delivery option) {
        return parameters.getDeliveryServiceId() == null || // нет требования на конкретную СД
                option.getDeliveryServiceId() == null || // СД скрыты (репорт не вернул СД)
                option.getDeliveryServiceId().equals(parameters.getDeliveryServiceId());
    }

    private boolean isSameDeliveryPartnerType(CheckoutOptionParameters parameters, Delivery option) {
        return option.getDeliveryPartnerType() == avoidNull(parameters.getDeliveryPartnerType(), option
                .getDeliveryPartnerType());
    }

    private boolean isSameType(CheckoutOptionParameters parameters, Delivery option) {
        return option.getType() == avoidNull(parameters.getDeliveryType(), option.getType());
    }

    private boolean isSameDateFrom(CheckoutOptionParameters parameters, Delivery option) {
        return parameters.getFromDate() == null ||
                option.getDeliveryDates().getFromDate().equals(parameters.getFromDate());
    }

    private boolean isSameLeaveAtTheDoor(CheckoutOptionParameters parameters, Delivery option) {
        return parameters.getLeaveAtTheDoor() == null ||
                option.isLeaveAtTheDoor() == null ||
                option.isLeaveAtTheDoor().equals(parameters.getLeaveAtTheDoor());
    }

    private HttpHeaders buildHeaders(Parameters parameters) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(X_HIT_RATE_GROUP, avoidNull(parameters.configuration().cart().request().getHitRateGroup(),
                HitRateGroup.UNLIMIT).name());
        if (parameters.configuration().cart().request().getMetaInfo() != null) {
            headers.set(X_ORDER_META, parameters.configuration().cart().request().getMetaInfo());
        }
        if (parameters.configuration().cart().request().getMarketRequestId() != null) {
            headers.set(X_MARKET_REQUEST_ID, parameters.configuration().cart().request().getMarketRequestId());
        }
        if (parameters.configuration().cart().request().getICookie() != null) {
            headers.set(X_YANDEX_ICOOKIE, parameters.configuration().cart().request().getICookie());
        }
        return headers;
    }

    private Class<? extends Exception> extractExceptionClass(@Nonnull MvcResult result) {
        return Optional.ofNullable(result.getResolvedException())
                .map(Exception::getClass)
                .orElse(null);
    }

}
