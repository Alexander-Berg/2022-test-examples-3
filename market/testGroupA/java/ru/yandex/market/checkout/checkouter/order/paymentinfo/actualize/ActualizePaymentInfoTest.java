package ru.yandex.market.checkout.checkouter.order.paymentinfo.actualize;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.HelpingHandStatus;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderService;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplServiceType;
import ru.yandex.market.checkout.helpers.ActualizePaymentInfoHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.perseypayments.PerseyMockConfigurer;
import ru.yandex.market.checkout.util.report.BnplFactory;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_ACTUALIZE_PAYMENT_INFO_MOCK;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_BNPL;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_HELPING_HAND_ESTIMATE;
import static ru.yandex.market.checkout.checkouter.order.Platform.DESKTOP;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.TINKOFF_CREDIT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ActualizePaymentInfoTest extends AbstractWebTestBase {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();

    @Autowired
    private ActualizePaymentInfoHelper actualizePaymentInfoHelper;
    @Autowired
    private PerseyMockConfigurer perseyMockConfigurer;
    @Autowired
    private WireMockServer perseyPaymentsMock;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    public void setup() {
        checkouterFeatureWriter.writeValue(ENABLE_ACTUALIZE_PAYMENT_INFO_MOCK, true);
    }

    @Test
    public void shouldCreateOrderPaymentOptionsProperty() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = orderCreateHelper.checkout(cart, parameters).getCarts().get(0);
        Order orderFromDB = orderService.getOrder(order.getId());
        assertEquals(
                cart.getCarts().get(0).getPaymentOptions(),
                orderFromDB.getProperty(OrderPropertyType.ORDER_PAYMENT_OPTIONS)
        );
    }

    @Test
    public void shouldNotEmptyMultiOrderFields() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        MultiOrder multiOrder = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, parameters);
        assertNotNull(multiOrder.getCashback());
        assertNotNull(multiOrder.getTotals());
        for (Order order : multiOrder.getOrders()) {
            assertFalse(order.getPaymentOptions().isEmpty());
        }
    }

    @Test
    @DisplayName("Проверяем, что возвращаются те же опции оплаты, что при чекауте")
    public void shouldReturnPaymentParameters() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getOrders().get(0);
        PaymentMethod checkoutPaymentMethod = order.getPaymentMethod();
        PaymentType checkoutPaymentType = order.getPaymentType();
        order.setPaymentMethod(null);
        order.setPaymentType(null);
        MultiOrder multiOrder = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, parameters);
        assertThat(multiOrder.getOrders().get(0).getPaymentMethod(), equalTo(checkoutPaymentMethod));
        assertThat(multiOrder.getOrders().get(0).getPaymentType(), equalTo(checkoutPaymentType));
    }


    @Test
    public void shouldReturnNewCashbackPromos() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
//      check default promo
        Set<ItemPromo> checkoutPromos = checkout.getOrders().get(0).getItems().iterator().next().getPromos();
        assertThat(checkoutPromos, hasSize(1));
        ItemPromo checkoutPromo = checkoutPromos.iterator().next();
        assertThat(BigDecimal.valueOf(100), comparesEqualTo(checkoutPromo.getCashbackAccrualAmount()));
        assertThat("promoKey", equalTo(checkoutPromo.getPromoDefinition().getMarketPromoId()));
//        change promos
        CashbackResponse cashBackResponse = createCashbackResponse();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(cashBackResponse);
        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);
        MultiOrder paymentInfo = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, parameters);
//        check new promo
        Set<ItemPromo> paymentInfoPromos = paymentInfo.getOrders().get(0).getItems().iterator().next().getPromos();
        assertThat(paymentInfoPromos, hasSize(1));
        ItemPromo paymentInfoPromo = paymentInfoPromos.iterator().next();
        assertThat(BigDecimal.ONE, comparesEqualTo(paymentInfoPromo.getCashbackAccrualAmount()));
        assertThat("newPromoKey", equalTo(paymentInfoPromo.getPromoDefinition().getMarketPromoId()));
    }

    @Test
    void shouldReturnNewCashbackPromosFromDB() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
//      check default promo
        Set<ItemPromo> checkoutPromos = checkout.getOrders().get(0).getItems().iterator().next().getPromos();
        assertThat(checkoutPromos, hasSize(1));
        ItemPromo checkoutPromo = checkoutPromos.iterator().next();
        assertThat(BigDecimal.valueOf(100), comparesEqualTo(checkoutPromo.getCashbackAccrualAmount()));
        assertThat("promoKey", equalTo(checkoutPromo.getPromoDefinition().getMarketPromoId()));
//        change promos
        CashbackResponse cashBackResponse = createCashbackResponse();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(cashBackResponse);
        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);

        MultiOrder paymentInfo = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, parameters);
        actualizePaymentInfoHelper.changePayment(paymentInfo, parameters);
//        check new promo
        var order = orderService.getOrder(checkout.getOrders().get(0).getId());
        var orderPromos = order.getItems().iterator().next().getPromos();
        assertThat(orderPromos, hasSize(1));

        var orderPromo = orderPromos.iterator().next();
        assertThat(BigDecimal.ONE, comparesEqualTo(orderPromo.getCashbackAccrualAmount()));
        assertThat("newPromoKey", equalTo(orderPromo.getPromoDefinition().getMarketPromoId()));
    }

    @Test
    void shouldFillCreditInfo() throws Exception {
        Parameters params = getParametersWithCreditInfoInformation();
        MultiCart cart = orderCreateHelper.cart(params);
        MultiOrder checkout = orderCreateHelper.checkout(cart, params);
        checkCreditInformation(checkout);
        MultiOrder paymentInfo = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, params);
        checkCreditInformation(paymentInfo);
    }

    @Test
    public void testCreditInfoRequest() throws Exception {
        Parameters params = getParametersWithCreditInfoInformation();
        MultiCart cart = orderCreateHelper.cart(params);
        MultiOrder checkout = orderCreateHelper.checkout(cart, params);
        actualizePaymentInfoHelper.actualizePaymentInfo(checkout, params);
        List<LoggedRequest> creditInfoRequests = reportMock.getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.queryParameter("place")
                        .containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                .sorted(Comparator.comparing(LoggedRequest::getLoggedDate).reversed())
                .collect(Collectors.toList());
        LoggedRequest paymentInfoCreditInfoRequest = creditInfoRequests.get(0);
        LoggedRequest checkoutInfoCreditInfoRequest = creditInfoRequests.get(1);
        Map<String, QueryParameter> checkoutQueryParams = paymentInfoCreditInfoRequest.getQueryParams();
        Map<String, QueryParameter> paymentInfoQueryParams = checkoutInfoCreditInfoRequest.getQueryParams();
        assertEquals(checkoutQueryParams.size(), paymentInfoQueryParams.size());
        for (Map.Entry<String, QueryParameter> checkoutQueryParameterEntry : checkoutQueryParams.entrySet()) {
            QueryParameter paymentInfoQueryParameter = paymentInfoQueryParams.get(checkoutQueryParameterEntry.getKey());
            QueryParameter checkoutQueryParameter = checkoutQueryParameterEntry.getValue();
            assertEquals(paymentInfoQueryParameter.values().size(), checkoutQueryParameter.values().size());
            for (String value : checkoutQueryParameter.values()) {
                if (checkoutQueryParameter.key().equals("total-price")) {
                    assertThat(new BigDecimal(value),
                            comparesEqualTo(new BigDecimal(paymentInfoQueryParameter.firstValue())));
                } else {
                    assertTrue(paymentInfoQueryParameter.containsValue(value));
                }
            }
        }
    }

    @Test
    public void testHelpingHandEstimateEnabled() throws Exception {
        perseyPaymentsMock.resetRequests();
        perseyMockConfigurer.mockEstimateSuccess();
        checkouterFeatureWriter.writeValue(ENABLE_HELPING_HAND_ESTIMATE, true);
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getBuyer().setUid(1L);
        parameters.setPlatform(Platform.IOS);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        MultiOrder paymentInfo = actualizePaymentInfoHelper.actualizePaymentInfo(multiOrder, parameters);
        assertThat(paymentInfo.getHelpingHandStatus(), is(HelpingHandStatus.ENABLED));
        assertThat(paymentInfo.getHelpingHandDonationAmount(), is(5));
        LoggedRequest request = perseyPaymentsMock.getServeEvents().getRequests().stream()
                .map(ServeEvent::getRequest)
                .max(Comparator.comparing(LoggedRequest::getLoggedDate))
                .get();
        assertThat(request.getHeader("X-Yandex-UID"), is("1"));
        assertThat(request.getHeader("X-Application"), is(DESKTOP.toString()));
        assertThat(new BigDecimal(request.queryParameter("ride_cost").firstValue()),
                comparesEqualTo(BigDecimal.valueOf(350)));
        assertThat(request.queryParameter("currency_code").firstValue(), is("RUB"));
        assertThat(request.queryParameter("payment_tech_type").firstValue(), is("card"));
    }

    @Test
    public void testBnpl() throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_BNPL, true);
        reportConfigurer.mockDefaultCreditInfo();
        bnplMockConfigurer.mockWholeBnpl();
        Parameters params = defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(params);
        MultiOrder checkout = orderCreateHelper.checkout(cart, params);
        MultiOrder paymentInfo = actualizePaymentInfoHelper.actualizePaymentInfo(checkout, params);
        BnplInfo bnplInfo = paymentInfo.getBnplInfo();
        assertNotNull(bnplInfo);
        assertTrue(bnplInfo.isAvailable());
        assertNotNull(bnplInfo.getBnplPlanDetails());
        assertNotNull(bnplInfo.getPlans());
        List<ServeEvent> serveEvents = bnplMockConfigurer.servedEvents();
        assertThat(serveEvents, hasSize(2));
        List<BnplPlanCheckRequestBody> planCheckRequests = serveEvents.stream()
                .map(serveEvent -> GSON.fromJson(
                        serveEvent.getRequest().getBodyAsString(), BnplPlanCheckRequestBody.class
                )).collect(Collectors.toList());
        for (BnplPlanCheckRequestBody planCheckRequest : planCheckRequests) {
            assertThat(planCheckRequest.getServices(), hasSize(2));
            BnplOrderService bnplOrderService = planCheckRequest.getServices().get(0);
            if (bnplOrderService.getType() == BnplServiceType.PAYMENT) {
                checkPaymentBnplOrderService(bnplOrderService);
            } else if (bnplOrderService.getType() == BnplServiceType.LOAN) {
                checkLoanBnplOrderService(bnplOrderService);
            }
        }
    }

    private void checkLoanBnplOrderService(BnplOrderService loanBnplOrderService) {
        assertThat(loanBnplOrderService.getAmount(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(loanBnplOrderService.getCurrency(), equalTo("RUB"));
        List<BnplItem> loanBnplItems = loanBnplOrderService.getItems();
        assertThat(loanBnplItems, hasSize(1));
        BnplItem loanBnplItem = loanBnplItems.get(0);
        assertThat(loanBnplItem.getCount(), equalTo(1));
        assertThat(loanBnplItem.getPrice(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(loanBnplItem.getTotal(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(loanBnplItem.getTitle(), equalTo("Доставка"));
    }

    private void checkPaymentBnplOrderService(BnplOrderService paymentBnplOrderService) {
        assertThat(paymentBnplOrderService.getAmount(), comparesEqualTo(BigDecimal.valueOf(250)));
        assertThat(paymentBnplOrderService.getCurrency(), equalTo("RUB"));
        List<BnplItem> paymentBnplItems = paymentBnplOrderService.getItems();
        assertThat(paymentBnplItems, hasSize(1));
        BnplItem paymentBnplItem = paymentBnplItems.get(0);
        assertThat(paymentBnplItem.getCount(), equalTo(1));
        assertThat(paymentBnplItem.getPrice(), comparesEqualTo(BigDecimal.valueOf(250)));
        assertThat(paymentBnplItem.getTotal(), comparesEqualTo(BigDecimal.valueOf(250)));
        assertThat(paymentBnplItem.getShopId(), equalTo("667"));
        assertThat(paymentBnplItem.getTitle(), equalTo("OfferName"));
    }

    private void checkCreditInformation(MultiOrder paymentInfo) {
        assertThat(paymentInfo.getCarts().get(0).getPaymentMethod(), equalTo(TINKOFF_CREDIT));
        assertThat(paymentInfo.getCreditInformation(), notNullValue());
        assertThat(paymentInfo.getCreditInformation().getCreditMonthlyPayment(),
                comparesEqualTo(BigDecimal.valueOf(605)));
        assertThat(paymentInfo.getCreditInformation().getPriceForCreditAllowed(),
                comparesEqualTo(BigDecimal.valueOf(3500)));
    }

    private Parameters getParametersWithCreditInfoInformation() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));
        params.setShowCredits(true);
        params.setPaymentMethod(TINKOFF_CREDIT);
        return params;
    }

    private FoundOfferBuilder makeCreditInfo(FoundOfferBuilder offer) {
        return offer.bnpl(true, BnplFactory.installments(1, BnplFactory.payment(100)));
    }

    private CashbackResponse createCashbackResponse() {
        return new CashbackResponse(
                new CashbackOptions("newPromoKey", null, BigDecimal.ONE,
                        CashbackPermision.ALLOWED, null, null, null, null, null),
                new CashbackOptions("newPromoKey", null, BigDecimal.valueOf(30L),
                        CashbackPermision.ALLOWED, null, null, null, null, null),
                CashbackType.EMIT);
    }
}
