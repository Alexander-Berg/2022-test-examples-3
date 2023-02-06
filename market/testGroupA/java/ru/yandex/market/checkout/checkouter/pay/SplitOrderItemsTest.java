package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BalanceOrderParams;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketLine;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.model.RefundLine;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.CreateRefundRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.TrustRestApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.archive.EndPoint;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.loyalty.AbstractLoyaltyBundleResponseTransformer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ORDER_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_SPLIT_ORDER_ITEMS_WHEN_CONTAINS_SUBSIDY;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.CREATE_VIRTUAL_PAYMENT;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketSplitOrderItemsForClearTask;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplOrderParametersWithCashBackSpent;

public class SplitOrderItemsTest extends AbstractWebTestBase {

    /**
     * Это число зависит от дефолтного ответа лоялти, который конфигурится в
     * {@link AbstractLoyaltyBundleResponseTransformer}
     */
    private static final BigDecimal ITEM_CASHBACK = new BigDecimal("30.00");
    private static final String PROMO_CODE = "promo";

    private static final String CARD = PaymentAgent.DEFAULT.getTrustPaymentMethod();
    private static final String YANDEX_ACCOUNT = PaymentAgent.YANDEX_CASHBACK.getTrustPaymentMethod();


    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TrustMockConfigurer trustMockConfigurer;
    @Autowired
    private TrustRestApi trustRestApiSpy;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private ReceiptItemsDivider receiptItemsDivider;
    @Autowired
    private ReceiptService receiptService;

    @BeforeEach
    public void setUp() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        personalMockConfigurer.mockV1EmailsRetrieve();
        checkouterFeatureWriter.writeValue(ENABLE_SPLIT_ORDER_ITEMS_WHEN_CONTAINS_SUBSIDY, true);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, 0);
    }

    @AfterEach
    public void tearDown() {
        Mockito.clearInvocations(trustRestApiSpy);
    }

    @Test
    public void testWithCashback() {
        // given
        double cashbackAmount = 151.0;
        trustMockConfigurer.mockListWalletBalanceResponse(cashbackAmount);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setMockLoyalty(true);
        parameters.setupPromo(null);
        parameters.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice = new BigDecimal(1065);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        orderPayHelper.payForOrderWithoutNotification(order);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        var balanceOrdersId = List.of(
                orderId + "-item-" + itemId + "-1",
                orderId + "-item-" + itemId + "-2",
                orderId + "-item-" + itemId + "-3",
                orderId + "-delivery"
        );
        Assertions.assertEquals(4, balanceOrders.size());
        balanceOrders.forEach(o -> Assertions.assertTrue(balanceOrdersId.contains(o.getServiceOrderId())));

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1065.00"),
                balanceOrdersId.get(1), new BigDecimal("1065.00"),
                balanceOrdersId.get(2), new BigDecimal("1065.00"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getPricePerItem());
        }

        var cardMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1015.00"),
                balanceOrdersId.get(1), new BigDecimal("1015.00"),
                balanceOrdersId.get(2), new BigDecimal("1014.00"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(cardMarkup.size(), request.getMarkup().getBasketLinesMarkup().size());
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            Assertions.assertEquals(cardMarkup.get(balanceOrderId), markup.getLineMarkup().get(CARD));
        });

        var cashbackMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("50.00"),
                balanceOrdersId.get(1), new BigDecimal("50.00"),
                balanceOrdersId.get(2), new BigDecimal("51.00")
        );
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            if (Objects.equals(balanceOrderId, orderId + "-delivery")) {
                Assertions.assertFalse(markup.getLineMarkup().containsKey(YANDEX_ACCOUNT));
                return;
            }
            Assertions.assertEquals(
                    cashbackMarkup.get(balanceOrderId),
                    markup.getLineMarkup().get(YANDEX_ACCOUNT).setScale(2, RoundingMode.HALF_UP)
            );
        });

        boolean sumsEquals = Stream.concat(cardMarkup.values().stream(), cashbackMarkup.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .equals(linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertTrue(sumsEquals);
    }

    @Test
    public void testWithCashbackAndPromo() {
        // given
        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        orderPayHelper.payForOrderWithoutNotification(order);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        var balanceOrdersId = List.of(
                orderId + "-item-" + itemId + "-1",
                orderId + "-item-" + itemId + "-2",
                orderId + "-item-" + itemId + "-3",
                orderId + "-delivery"
        );
        Assertions.assertEquals(4, balanceOrders.size());
        balanceOrders.forEach(o -> Assertions.assertTrue(balanceOrdersId.contains(o.getServiceOrderId())));

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("150.01"),
                balanceOrdersId.get(1), new BigDecimal("150.01"),
                balanceOrdersId.get(2), new BigDecimal("150.01"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getPricePerItem());
        }

        var cardMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("140.01"),
                balanceOrdersId.get(1), new BigDecimal("140.01"),
                balanceOrdersId.get(2), new BigDecimal("140.01"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(cardMarkup.size(), request.getMarkup().getBasketLinesMarkup().size());
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            Assertions.assertEquals(cardMarkup.get(balanceOrderId), markup.getLineMarkup().get(CARD));
        });

        var cashbackMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("10.00"),
                balanceOrdersId.get(1), new BigDecimal("10.00"),
                balanceOrdersId.get(2), new BigDecimal("10.00")
        );
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            if (Objects.equals(balanceOrderId, orderId + "-delivery")) {
                Assertions.assertFalse(markup.getLineMarkup().containsKey(YANDEX_ACCOUNT));
                return;
            }
            Assertions.assertEquals(
                    cashbackMarkup.get(balanceOrderId),
                    markup.getLineMarkup().get(YANDEX_ACCOUNT).setScale(2, RoundingMode.HALF_UP)
            );
        });

        boolean sumsEquals = Stream.concat(cardMarkup.values().stream(), cashbackMarkup.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .equals(linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertTrue(sumsEquals);
    }

    @Test
    public void testWithoutSubsidy() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);

        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        orderPayHelper.payForOrderWithoutNotification(order);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        Assertions.assertEquals(2, balanceOrders.size());

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                orderId + "-item-" + itemId, new BigDecimal("250.00"),
                orderId + "-delivery", new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(
                    linesPrice.get(line.getBalanceOrderId()),
                    line.getPricePerItem().setScale(2, RoundingMode.HALF_UP)
            );
        }

        Assertions.assertNull(request.getMarkup());
    }

    @Test
    public void testWithPromo() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        //чтобы были субсидии за офферы и за доставку
        parameters.setupPromo(PROMO_CODE);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, LoyaltyDiscount.builder()
                        .discount(BigDecimal.valueOf(50L))
                        .promoKey(PROMO_CODE)
                        .promoType(PromoType.MARKET_COUPON).build());

        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        orderPayHelper.payForOrderWithoutNotification(order);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        Assertions.assertEquals(4, balanceOrders.size());

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                orderId + "-item-" + itemId + "-1", new BigDecimal("150.01"),
                orderId + "-item-" + itemId + "-2", new BigDecimal("150.01"),
                orderId + "-item-" + itemId + "-3", new BigDecimal("150.01"),
                orderId + "-delivery", new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(
                    linesPrice.get(line.getBalanceOrderId()),
                    line.getPricePerItem().setScale(2, RoundingMode.HALF_UP)
            );
        }

        Assertions.assertNull(request.getMarkup());
    }

    @Test
    public void testRefundWithCashback() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(4.0);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setMockLoyalty(true);
        parameters.setupPromo(null);
        parameters.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice = new BigDecimal(101);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // do
        Order order = orderCreateHelper.createOrder(parameters);

        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // check
        var balanceOrdersId = List.of(
                orderId + "-item-" + itemId + "-1",
                orderId + "-item-" + itemId + "-2",
                orderId + "-item-" + itemId + "-3",
                orderId + "-delivery"
        );

        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        var createRefund = serveEvents.get(11).getRequest().getBodyAsString();
        System.out.println(createRefund);

        ArgumentCaptor<CreateRefundRequest> captor = ArgumentCaptor.forClass(CreateRefundRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createRefund(any(), any(), captor.capture(), any());
        CreateRefundRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("101.00"),
                balanceOrdersId.get(1), new BigDecimal("101.00"),
                balanceOrdersId.get(2), new BigDecimal("101.00"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (RefundLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getAmountDelta());
        }

        var cardMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("100.00"),
                balanceOrdersId.get(1), new BigDecimal("100.00"),
                balanceOrdersId.get(2), new BigDecimal("99.00"),
                balanceOrdersId.get(3), new BigDecimal("100.00")
        );
        Assertions.assertEquals(cardMarkup.size(), request.getBasketMarkup().getBasketLinesMarkup().size());
        request.getBasketMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            Assertions.assertEquals(cardMarkup.get(balanceOrderId), markup.getLineMarkup().get(CARD));
        });

        var cashbackMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1.00"),
                balanceOrdersId.get(1), new BigDecimal("1.00"),
                balanceOrdersId.get(2), new BigDecimal("2.00")
        );
        request.getBasketMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            if (Objects.equals(balanceOrderId, orderId + "-delivery")) {
                Assertions.assertFalse(markup.getLineMarkup().containsKey(YANDEX_ACCOUNT));
                return;
            }
            Assertions.assertEquals(
                    cashbackMarkup.get(balanceOrderId),
                    markup.getLineMarkup().get(YANDEX_ACCOUNT).setScale(2, RoundingMode.HALF_UP)
            );
        });

        boolean sumsEquals = Stream.concat(cardMarkup.values().stream(), cashbackMarkup.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .equals(linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertTrue(sumsEquals);
    }

    @Test
    public void testRefundWithPromo() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();

        BigDecimal itemPrice = new BigDecimal(101);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // do
        Order order = orderCreateHelper.createOrder(parameters);

        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        var createRefund = serveEvents.get(11).getRequest().getBodyAsString();
        System.out.println(createRefund);

        ArgumentCaptor<CreateRefundRequest> captor = ArgumentCaptor.forClass(CreateRefundRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createRefund(any(), any(), captor.capture(), any());
        CreateRefundRequest request = captor.getValue();

        var linesPrice = Map.of(
                orderId + "-item-" + itemId + "-1", new BigDecimal("1.01"),
                orderId + "-item-" + itemId + "-2", new BigDecimal("1.01"),
                orderId + "-item-" + itemId + "-3", new BigDecimal("1.01"),
                orderId + "-delivery", new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (RefundLine line : request.getLines()) {
            Assertions.assertEquals(
                    linesPrice.get(line.getBalanceOrderId()),
                    line.getAmountDelta().setScale(2, RoundingMode.HALF_UP)
            );
        }

        Assertions.assertNull(request.getBasketMarkup());
    }

    @Test
    public void testRefundWithoutSubsidy() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);

        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        var createRefund = serveEvents.get(11).getRequest().getBodyAsString();
        System.out.println(createRefund);

        ArgumentCaptor<CreateRefundRequest> captor = ArgumentCaptor.forClass(CreateRefundRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createRefund(any(), any(), captor.capture(), any());
        CreateRefundRequest request = captor.getValue();

        var linesPrice = Map.of(
                orderId + "-item-" + itemId, new BigDecimal("750.00"),
                orderId + "-delivery", new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (RefundLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getAmountDelta());
        }

        Assertions.assertNull(request.getBasketMarkup());
    }

    @Test
    public void testUnholdWithoutSubsidy() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);

        BigDecimal itemPrice = new BigDecimal(1065);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrder(order);

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, payment.getStatus());

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        paymentService.clearOrCancelHeldPayment(payment.getId(), Collections.singleton(order.getId()));

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        ServeEvent unholdEvent = serveEvents.get(9);
        Assertions.assertTrue(unholdEvent.getRequest().getUrl().endsWith("/unhold"));
        Assertions.assertEquals(200, unholdEvent.getResponse().getStatus());
        Assertions.assertTrue(unholdEvent.getResponse().getBodyAsString().contains("success"));
    }

    @Test
    public void testPartialUnholdWithoutSubsidy() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);

        Parameters parameters1 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters1.setCheckCartErrors(false);

        BigDecimal itemPrice1 = new BigDecimal(1065);
        OrderItem item1 = parameters1.getItems().iterator().next();
        item1.setCount(3);
        item1.setQuantity(new BigDecimal(3));
        item1.setBuyerPrice(itemPrice1);
        item1.setPrice(itemPrice1);
        item1.setQuantPrice(itemPrice1);

        Parameters parameters2 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters1.setCheckCartErrors(false);

        BigDecimal itemPrice2 = new BigDecimal(500);
        OrderItem item2 = parameters1.getItems().iterator().next();
        item2.setCount(2);
        item2.setQuantity(new BigDecimal(2));
        item2.setBuyerPrice(itemPrice2);
        item2.setPrice(itemPrice2);
        item2.setQuantPrice(itemPrice2);

        // do
        Order order1 = orderCreateHelper.createOrder(parameters1);
        var orderId1 = order1.getId().toString();
        var itemId1 = order1.getItems().iterator().next().getId().toString();

        Order order2 = orderCreateHelper.createOrder(parameters2);
        var orderId2 = order2.getId().toString();
        var itemId2 = order2.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrders(List.of(order1, order2));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, payment.getStatus());

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.CANCELLED);
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(order1, order2)));

        paymentService.clearOrCancelHeldPayment(payment.getId(), Set.of(order1.getId(), order2.getId()));

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        List.of(serveEvents.get(11), serveEvents.get(12)).forEach(unholdEvent -> {
            LoggedRequest request = unholdEvent.getRequest();
            if (request.getUrl().contains(orderId1 + "-delivery/unhold") ||
                    request.getUrl().contains(orderId1 + "-item-" + itemId1 + "/unhold")
            ) {
                Assertions.assertEquals(200, unholdEvent.getResponse().getStatus());
                Assertions.assertTrue(unholdEvent.getResponse().getBodyAsString().contains("success"));
            } else {
                Assertions.fail();
            }
        });
    }

    @Test
    public void testPartialUnholdWithCashback() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(151.0);

        Parameters parameters1 = CashbackTestProvider.defaultCashbackParameters();
        parameters1.setMockLoyalty(true);
        parameters1.setupPromo(null);
        parameters1.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters1.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters1.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice1 = new BigDecimal(1065);
        OrderItem item1 = parameters1.getItems().iterator().next();
        item1.setCount(3);
        item1.setQuantity(new BigDecimal(3));
        item1.setBuyerPrice(itemPrice1);
        item1.setPrice(itemPrice1);
        item1.setQuantPrice(itemPrice1);

        Parameters parameters2 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters2.setCheckCartErrors(false);

        BigDecimal itemPrice2 = new BigDecimal(500);
        OrderItem item2 = parameters2.getItems().iterator().next();
        item2.setCount(2);
        item2.setQuantity(new BigDecimal(2));
        item2.setBuyerPrice(itemPrice2);
        item2.setPrice(itemPrice2);
        item2.setQuantPrice(itemPrice2);

        // do
        Order order1 = orderCreateHelper.createOrder(parameters1);
        var orderId1 = order1.getId().toString();
        var itemId1 = order1.getItems().iterator().next().getId().toString();

        Order order2 = orderCreateHelper.createOrder(parameters2);
        var orderId2 = order2.getId().toString();
        var itemId2 = order2.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrders(List.of(order1, order2));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, payment.getStatus());

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.CANCELLED);

        CheckBasketParams checkBasketMock = checkBasketSplitOrderItemsForClearTask(ImmutableList.of(order1, order2));
        trustMockConfigurer.mockCheckBasket(checkBasketMock);

        paymentService.clearOrCancelHeldPayment(payment.getId(), Set.of(order1.getId(), order2.getId()));

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        List<ServeEvent> unholdEvents = List.of(
                serveEvents.get(11), serveEvents.get(12), serveEvents.get(13), serveEvents.get(14));
        unholdEvents.forEach(unholdEvent -> {
            LoggedRequest request = unholdEvent.getRequest();
            if (request.getUrl().contains(orderId1 + "-delivery/unhold") ||
                    request.getUrl().contains(orderId1 + "-item-" + itemId1 + "-1/unhold") ||
                    request.getUrl().contains(orderId1 + "-item-" + itemId1 + "-2/unhold") ||
                    request.getUrl().contains(orderId1 + "-item-" + itemId1 + "-3/unhold")
            ) {
                Assertions.assertEquals(200, unholdEvent.getResponse().getStatus());
                Assertions.assertTrue(unholdEvent.getResponse().getBodyAsString().contains("success"));
            } else {
                Assertions.fail();
            }
        });
        Assertions.assertTrue(serveEvents.stream().noneMatch(it -> it.getRequest().getUrl().contains("resize")));
    }

    @Test
    public void testMultiOrderWithCashback() {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(151.0);

        Parameters parameters1 = CashbackTestProvider.defaultCashbackParameters();
        parameters1.setMockLoyalty(true);
        parameters1.setupPromo(null);
        parameters1.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters1.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters1.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice1 = new BigDecimal(1065);
        OrderItem item1 = parameters1.getItems().iterator().next();
        item1.setCount(3);
        item1.setQuantity(new BigDecimal(3));
        item1.setBuyerPrice(itemPrice1);
        item1.setPrice(itemPrice1);
        item1.setQuantPrice(itemPrice1);

        Parameters parameters2 = CashbackTestProvider.defaultCashbackParameters();
        parameters2.setMockLoyalty(true);
        parameters2.setupPromo(null);
        parameters2.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters2.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(5000)),
                CashbackType.SPEND)
        );
        parameters2.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice2 = new BigDecimal(500);
        OrderItem item2 = parameters2.getItems().iterator().next();
        item2.setCount(2);
        item2.setQuantity(new BigDecimal(2));
        item2.setBuyerPrice(itemPrice2);
        item2.setPrice(itemPrice2);
        item2.setQuantPrice(itemPrice2);

        Order order1 = orderCreateHelper.createOrder(parameters1);
        var orderId1 = order1.getId().toString();
        var itemId1 = order1.getItems().iterator().next().getId().toString();

        Order order2 = orderCreateHelper.createOrder(parameters2);
        var orderId2 = order2.getId().toString();
        var itemId2 = order2.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrders(List.of(order1, order2));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, payment.getStatus());

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        var balanceOrdersId = List.of(
                orderId1 + "-item-" + itemId1 + "-1",
                orderId1 + "-item-" + itemId1 + "-2",
                orderId1 + "-item-" + itemId1 + "-3",
                orderId1 + "-delivery",
                orderId2 + "-item-" + itemId2 + "-1",
                orderId2 + "-item-" + itemId2 + "-2",
                orderId2 + "-delivery"
        );
        Assertions.assertEquals(balanceOrdersId.size(), balanceOrders.size());
        balanceOrders.forEach(o -> Assertions.assertTrue(balanceOrdersId.contains(o.getServiceOrderId())));

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1065.00"),
                balanceOrdersId.get(1), new BigDecimal("1065.00"),
                balanceOrdersId.get(2), new BigDecimal("1065.00"),
                balanceOrdersId.get(3), new BigDecimal("100.00"),
                balanceOrdersId.get(4), new BigDecimal("500.00"),
                balanceOrdersId.get(5), new BigDecimal("500.00"),
                balanceOrdersId.get(6), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getPricePerItem());
        }

        BigDecimal linesSum = linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal markupSum = request.getMarkup().getBasketLinesMarkup().values()
                .stream().flatMap(it -> it.getLineMarkup().values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(linesSum, markupSum);
    }

    @Test
    public void testRefundWithCashbackBackwardCompability() {
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, Integer.MAX_VALUE - 2);
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(4.0);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setMockLoyalty(true);
        parameters.setupPromo(null);
        parameters.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice = new BigDecimal(101);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId().toString();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // check
        var balanceOrdersId = List.of(
                orderId + "-item-" + itemId,
                orderId + "-delivery"
        );

        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        var createRefund = serveEvents.get(11).getRequest().getBodyAsString();
        System.out.println(createRefund);

        ArgumentCaptor<CreateRefundRequest> captor = ArgumentCaptor.forClass(CreateRefundRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createRefund(any(), any(), captor.capture(), any());
        CreateRefundRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("303.00"),
                balanceOrdersId.get(1), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (RefundLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getAmountDelta());
        }

        var cardMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("299.00"),
                balanceOrdersId.get(1), new BigDecimal("100.00")
        );
        Assertions.assertEquals(cardMarkup.size(), request.getBasketMarkup().getBasketLinesMarkup().size());
        request.getBasketMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            Assertions.assertEquals(cardMarkup.get(balanceOrderId), markup.getLineMarkup().get(CARD));
        });

        var cashbackMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("4.00")
        );
        request.getBasketMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            if (Objects.equals(balanceOrderId, orderId + "-delivery")) {
                Assertions.assertFalse(markup.getLineMarkup().containsKey(YANDEX_ACCOUNT));
                return;
            }
            Assertions.assertEquals(
                    cashbackMarkup.get(balanceOrderId),
                    markup.getLineMarkup().get(YANDEX_ACCOUNT).setScale(2, RoundingMode.HALF_UP)
            );
        });

        boolean sumsEquals = Stream.concat(cardMarkup.values().stream(), cashbackMarkup.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .equals(linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertTrue(sumsEquals);
    }

    @Test
    void virtualPaymentWithCashbackCorrectDivide() throws IOException {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);

        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();

        BigDecimal itemPrice = new BigDecimal(1065);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        basePayment = paymentService.getPayment(basePayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));
        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = paymentService.getPayments(
                order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL).get(0);
        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        virtualBnplPayment = paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM);
        assertThat(virtualBnplPayment.getStatus(), is(PaymentStatus.CLEARED));

        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();

        var createOrders = trustMockConfigurer.servedEvents().stream()
                .filter(serveEvent -> serveEvent.getStubMapping()
                        .getName().equals(TrustMockConfigurer.CREATE_ORDERS_STUB))
                .findFirst()
                .get();

        var createBasket = trustMockConfigurer.servedEvents().stream()
                .filter(serveEvent -> serveEvent.getStubMapping()
                        .getName().equals(TrustMockConfigurer.CREATE_BASKET_STUB))
                .findFirst()
                .get();

        ObjectMapper objectMapper = new ObjectMapper();

        Set<String> createOrdersOrderId = StreamEx.of(objectMapper.readTree(createOrders.getRequest().getBodyAsString())
                        .get("orders")
                        .iterator())
                .map(jsonNode -> jsonNode.get("order_id").asText())
                .collect(Collectors.toUnmodifiableSet());

        Set<String> createBasketOrderId = StreamEx.of(objectMapper.readTree(createBasket.getRequest().getBodyAsString())
                        .get("orders")
                        .iterator())
                .map(jsonNode -> jsonNode.get("order_id").asText())
                .collect(Collectors.toUnmodifiableSet());

        Assertions.assertEquals(4, createOrdersOrderId.size());
        Assertions.assertEquals(createOrdersOrderId, createBasketOrderId,
                "Список order_id в запросах должен быть одинаковым");
    }

    @Test
    public void testGetOrderReceiptsDivided() throws Exception {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.DONT_DIVIDE_RECEIPT_ITEMS_WHERE_ORDERS_BEFORE, -1);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, -1);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();

        BigDecimal itemPrice = new BigDecimal(101);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // prepare
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream().map(QueuedCall::getCallType)
                .forEach(queuedCallService::executeQueuedCallBatch);

        // do
        var request = get(EndPoint.ORDERS_RECEIPTS_DIVIDED.getEndPointName())
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CLIENT_ID, "0")
                .param(ORDER_ID, orderId);
        MvcResult result = getMockMvc().perform(request)
                .andExpect(status().isOk())
                .andReturn();

        // check
        Map<String, Integer> receiptTypes = new HashMap<>();

        var response = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
        ArrayNode receipts = (ArrayNode) response.get("receipts");
        Assertions.assertEquals(2, receipts.size());

        for (JsonNode receipt : receipts) {
            String type = receipt.get("type").asText();
            int count = receiptTypes.getOrDefault(type, 0);
            receiptTypes.put(type, ++count);

            ArrayNode items = (ArrayNode) receipt.get("items");
            Assertions.assertEquals(4, items.size());

            Map<Long, Integer> itemsCount = new HashMap<>();
            for (JsonNode ri : items) {
                long riId = ri.get("itemId").asLong();
                int itemCount = itemsCount.getOrDefault(riId, 0);
                itemsCount.put(riId, ++itemCount);
            }
            Assertions.assertEquals(3, itemsCount.get(itemId));
        }

        Assertions.assertEquals(1, receiptTypes.get("INCOME"));
        Assertions.assertEquals(1, receiptTypes.get("INCOME_RETURN"));
    }

    @Test
    public void testGetOrderReceiptsDividedDelivery() throws Exception {
        // given
        trustMockConfigurer.mockListWalletBalanceResponse(0.0);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.DONT_DIVIDE_RECEIPT_ITEMS_WHERE_ORDERS_BEFORE, -1);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, -1);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();

        BigDecimal itemPrice = new BigDecimal(101);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // prepare
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream().map(QueuedCall::getCallType)
                .forEach(queuedCallService::executeQueuedCallBatch);

        order = orderService.getOrder(order.getId());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream().map(QueuedCall::getCallType)
                .forEach(queuedCallService::executeQueuedCallBatch);

        // do
        var request = get(EndPoint.ORDERS_RECEIPTS_DIVIDED.getEndPointName())
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CLIENT_ID, "0")
                .param(ORDER_ID, orderId);
        MvcResult result = getMockMvc().perform(request)
                .andExpect(status().isOk())
                .andReturn();

        // check
        Map<String, Integer> receiptTypes = new HashMap<>();

        var response = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
        ArrayNode receipts = (ArrayNode) response.get("receipts");
        Assertions.assertEquals(3, receipts.size());

        for (JsonNode receipt : receipts) {
            String type = receipt.get("type").asText();
            int count = receiptTypes.getOrDefault(type, 0);
            receiptTypes.put(type, ++count);

            ArrayNode items = (ArrayNode) receipt.get("items");

            Map<Long, Integer> itemsCount = new HashMap<>();
            for (JsonNode ri : items) {
                long riId = ri.get("itemId").asLong();
                int itemCount = itemsCount.getOrDefault(riId, 0);
                itemsCount.put(riId, ++itemCount);
            }
            Assertions.assertEquals(3, itemsCount.get(itemId));
        }

        Assertions.assertEquals(2, receiptTypes.get("INCOME"));
        Assertions.assertEquals(1, receiptTypes.get("OFFSET_ADVANCE_ON_DELIVERED"));
    }

    @Test
    public void testGetOrderReceiptsDividedCashback() throws Exception {
        // given
        double cashbackAmount = 151.0;
        trustMockConfigurer.mockListWalletBalanceResponse(cashbackAmount);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.DONT_DIVIDE_RECEIPT_ITEMS_WHERE_ORDERS_BEFORE, -1);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, -1);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setMockLoyalty(true);
        parameters.setupPromo(null);
        parameters.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        BigDecimal itemPrice = new BigDecimal(1065);
        OrderItem item = parameters.getItems().iterator().next();
        item.setCount(3);
        item.setQuantity(new BigDecimal(3));
        item.setBuyerPrice(itemPrice);
        item.setPrice(itemPrice);
        item.setQuantPrice(itemPrice);

        // prepare
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        var itemId = order.getItems().iterator().next().getId();

        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream().map(QueuedCall::getCallType)
                .forEach(queuedCallService::executeQueuedCallBatch);

        // do
        var request = get(EndPoint.ORDERS_RECEIPTS_DIVIDED.getEndPointName())
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CLIENT_ID, "0")
                .param(ORDER_ID, orderId);
        MvcResult result = getMockMvc().perform(request)
                .andExpect(status().isOk())
                .andReturn();

        // check
        Map<String, Integer> receiptTypes = new HashMap<>();

        var response = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
        ArrayNode receipts = (ArrayNode) response.get("receipts");
        Assertions.assertEquals(2, receipts.size());

        for (JsonNode receipt : receipts) {
            String type = receipt.get("type").asText();
            int count = receiptTypes.getOrDefault(type, 0);
            receiptTypes.put(type, ++count);

            ArrayNode items = (ArrayNode) receipt.get("items");
            Assertions.assertEquals(4, items.size());

            Map<Long, Integer> itemsCount = new HashMap<>();
            for (JsonNode ri : items) {
                long riId = ri.get("itemId").asLong();
                int itemCount = itemsCount.getOrDefault(riId, 0);
                itemsCount.put(riId, ++itemCount);

                if (ri.hasNonNull("partitions")) {
                    ArrayNode partitions = (ArrayNode) ri.get("partitions");
                    for (JsonNode partition : partitions) {
                        String paymentAgent = partition.get("paymentAgent").asText();
                        double amount = partition.get("amount").asDouble();
                        if ("DEFAULT".equals(paymentAgent)) {
                            Assertions.assertTrue(amount == 1015 || amount == 1014);
                        } else if ("YANDEX_CASHBACK".equals(paymentAgent)) {
                            Assertions.assertTrue(amount == 50 || amount == 51);
                        }
                    }
                }
            }
            Assertions.assertEquals(3, itemsCount.get(itemId));
        }

        Assertions.assertEquals(1, receiptTypes.get("INCOME"));
        Assertions.assertEquals(1, receiptTypes.get("INCOME_RETURN"));
    }


    @Test
    @Disabled
    public void testWithCashbackItemWithCount3AndItemWithCount1() {
        // given
        double cashbackAmount = 151.0;
        trustMockConfigurer.mockListWalletBalanceResponse(cashbackAmount);

        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setMockLoyalty(true);
        parameters.setupPromo(null);
        parameters.getLoyaltyParameters().setExpectedPromoCode(null);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                null,
                CashbackOptions.allowed(BigDecimal.valueOf(500)),
                CashbackType.SPEND)
        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);

        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("1")
                .price(1065)
                .count(3)
                .build();
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("2")
                .price(200)
                .count(1)
                .build();
        parameters.getOrder().setItems(List.of(item1, item2));

        parameters.getOrder().getItems()
                .forEach(oi -> parameters.getReportParameters()
                        .overrideItemInfo(oi.getFeedOfferId()).getFulfilment().fulfilment = false);

        // do
        Order order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId().toString();
        Iterator<OrderItem> iterator = order.getItems().iterator();
        item1 = iterator.next();
        item2 = iterator.next();
        var item1Id = item1.getId().toString();
        var item2Id = item2.getId().toString();

        orderPayHelper.payForOrderWithoutNotification(order);

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents();
        String createBasket = serveEvents.get(4).getRequest().getBodyAsString();
        System.out.println(createBasket);

        ArgumentCaptor<List<BalanceOrderParams>> balanceOrdersCaptor = ArgumentCaptor.forClass((Class) List.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce())
                .batchCreateBalanceOrder(any(), any(), balanceOrdersCaptor.capture());
        List<BalanceOrderParams> balanceOrders = balanceOrdersCaptor.getValue();

        var balanceOrdersId = List.of(
                orderId + "-item-" + item1Id + "-1",
                orderId + "-item-" + item1Id + "-2",
                orderId + "-item-" + item1Id + "-3",
                orderId + "-item-" + item2Id,
                orderId + "-delivery"
        );
        Assertions.assertEquals(balanceOrdersId.size(), balanceOrders.size());
        balanceOrders.forEach(o -> Assertions.assertTrue(balanceOrdersId.contains(o.getServiceOrderId())));

        ArgumentCaptor<CreateBasketRequest> captor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        Mockito.verify(trustRestApiSpy, atLeastOnce()).createBasket(any(), captor.capture());

        CreateBasketRequest request = captor.getValue();

        var linesPrice = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1065.00"),
                balanceOrdersId.get(1), new BigDecimal("1065.00"),
                balanceOrdersId.get(2), new BigDecimal("1065.00"),
                balanceOrdersId.get(3), new BigDecimal("200.00"),
                balanceOrdersId.get(4), new BigDecimal("100.00")
        );
        Assertions.assertEquals(linesPrice.size(), request.getLines().size());
        for (CreateBasketLine line : request.getLines()) {
            Assertions.assertEquals(linesPrice.get(line.getBalanceOrderId()), line.getPricePerItem());
        }

        var cardMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("1040.00"),
                balanceOrdersId.get(1), new BigDecimal("1040.00"),
                balanceOrdersId.get(2), new BigDecimal("1040.00"),
                balanceOrdersId.get(3), new BigDecimal("124.00"),
                balanceOrdersId.get(4), new BigDecimal("100.00")
        );
        Assertions.assertEquals(cardMarkup.size(), request.getMarkup().getBasketLinesMarkup().size());
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            Assertions.assertEquals(cardMarkup.get(balanceOrderId), markup.getLineMarkup().get(CARD));
        });

        var cashbackMarkup = Map.of(
                balanceOrdersId.get(0), new BigDecimal("25.00"),
                balanceOrdersId.get(1), new BigDecimal("25.00"),
                balanceOrdersId.get(2), new BigDecimal("25.00"),
                balanceOrdersId.get(3), new BigDecimal("76.00")
        );
        request.getMarkup().getBasketLinesMarkup().forEach((balanceOrderId, markup) -> {
            if (Objects.equals(balanceOrderId, orderId + "-delivery")) {
                Assertions.assertFalse(markup.getLineMarkup().containsKey(YANDEX_ACCOUNT));
                return;
            }
            Assertions.assertEquals(
                    cashbackMarkup.get(balanceOrderId),
                    markup.getLineMarkup().get(YANDEX_ACCOUNT).setScale(2, RoundingMode.HALF_UP)
            );
        });

        boolean sumsEquals = Stream.concat(cardMarkup.values().stream(), cashbackMarkup.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .equals(linesPrice.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertTrue(sumsEquals);
    }

    @Configuration
    public static class TestConfig {

        @Bean
        @Primary
        public TrustRestApi trustRestApiSpy(TrustRestApi trustRestApi) {
            return Mockito.spy(trustRestApi);
        }
    }
}
