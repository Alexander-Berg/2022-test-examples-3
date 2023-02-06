package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Maps;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.db.LongRowMapper;
import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.TrustServiceFee;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus;
import ru.yandex.market.checkout.checkouter.balance.trust.model.FiscalAgentType;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PaymentGoalUtils;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.validation.SpasiboValidator;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItemPartition;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.TmsTaskHelper;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CreateBalanceOrderParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.MarkupBasketLineParams;
import ru.yandex.market.checkout.util.balance.checkers.MarkupBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsParamsProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus.authorized;
import static ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus.cleared;
import static ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus.not_authorized;
import static ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus.refunded;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;
import static ru.yandex.market.checkout.checkouter.pay.strategies.prepay.PrepayStrategyImpl.isNeedDivideBasketByItemsInBalance;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketConfig;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalWithRefundConfig;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;
import static ru.yandex.market.checkout.util.balance.checkers.MarkupBasketLineParams.line;
import static ru.yandex.market.checkout.util.balance.checkers.MarkupBasketParams.markup;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBasketCancelCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBasketClearCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.skipCheckBasketCalls;

/**
 * @author mkasumov
 */
public class PaymentTestHelper extends AbstractPaymentTestHelper {

    public static final Long MARKET_PARTNER_ID = 57656868L;
    public static final String DEFAULT_SUPPLIER_INN = "1234567890";
    public static final String DEFAULT_MARKET_INN = "7704357909";

    public static final String DELIVERY_SERVICE_ORDER_ID = "BLUE_MARKET_DELIVERY";
    static final String PAY_RETURN_PATH = "https://market.yandex.ru/page/after/payment";
    @Autowired
    protected JdbcTemplate masterJdbcTemplate;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ColorConfig colorConfig;
    @Autowired
    private TmsTaskHelper tmsTaskHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private CheckouterProperties checkouterProperties;
    @Autowired
    private CheckouterFeatureReader checkouterFeatureReader;
    @Autowired
    private TestSerializationService serializationService;
    @Autowired
    private SpasiboValidator spasiboValidator;
    @Autowired
    private OrderFinancialService financialService;
    @Autowired
    private RefundHelper refundHelper;

    private ReceiptTestHelper receiptTestHelper;
    private RefundTestHelper refundTestHelper;


    public PaymentTestHelper(AbstractWebTestBase test,
                             ReceiptTestHelper receiptTestHelper, RefundTestHelper refundTestHelper,
                             Has<Order> order, Has<ShopMetaData> shopMetaData) {
        super(test, order, shopMetaData);
        this.receiptTestHelper = receiptTestHelper;
        this.refundTestHelper = refundTestHelper;
    }

    public Long initPayment() throws Exception {
        return initPayment(null);
    }

    Long initPayment(@Nullable ShopMetaData shopMetaData) throws Exception {
        trustMockConfigurer.resetRequests();
        Order orderBeforePay = order();

        // Инициируем платеж
        initPaymentInner();

        Long uid = !Boolean.TRUE.equals(order().isNoAuth()) ? order().getBuyer().getUid() : null;
        checkBalanceCallsAfterPay(
                orderBeforePay,
                createCreateBasketBalanceCallParams(null, shopMetaData),
                uid
        );

        return checkReceipt();
    }

    private Long checkReceipt() throws Exception {
        if (isNewPrepayType()) {
            return receiptTestHelper.checkNewReceiptForPayment(ReceiptType.INCOME, ReceiptStatus.NEW);
        } else {
            if (checkouterFeatureReader.getBoolean(NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS)) {
                return receiptTestHelper.checkNewReceiptForPayment(ReceiptType.INCOME, ReceiptStatus.GENERATED);
            }
            return 0L;
        }
    }

    void initPaymentInner() throws Exception {
        String result = checkPaymentResponse(
                mockMvc.perform(post("/orders/{orderId}/payment?&returnPath={retPath}",
                        order().getId(), PAY_RETURN_PATH)
                        .param("uid", order().getBuyer().getUid().toString()))
                        .andExpect(status().isOk()), order(), null)
                .andDo(log())
                .andExpect(jsonPath("$.status").value(PaymentStatus.INIT.name()))
                .andReturn().getResponse().getContentAsString();
        long paymentId = (((Number) JsonPath.read(result, "$.id")).longValue());

        order.set(orderService.getOrder(order().getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES)));
        assertThat(order().getPaymentId(), is(paymentId));
    }

    void initPaymentWithBindedCard(@Nullable ShopMetaData shopMetaData) throws Exception {
        trustMockConfigurer.resetRequests();
        Order orderBeforePay = order();
        String cardId = "card-6644";

        // Инициируем платеж
        String result =
                checkPaymentResponse(
                        mockMvc.perform(post("/orders/{orderId}/payment?&returnPath={retPath}",
                                        order().getId(), PAY_RETURN_PATH)
                                        .param("uid", order().getBuyer().getUid().toString())
                                        .param("cardId", cardId))
                                .andExpect(status().isOk()), order(), null)
                        .andDo(log())
                        .andExpect(jsonPath("$.status").value(PaymentStatus.INIT.name()))
                        .andReturn().getResponse().getContentAsString();
        long paymentId = (((Number) JsonPath.read(result, "$.id")).longValue());

        order.set(orderService.getOrder(order().getId()));
        assertThat(order().getPaymentId(), is(paymentId));

        Long uid = !Boolean.TRUE.equals(order().isNoAuth()) ? order().getBuyer().getUid() : null;
        checkBalanceCallsAfterPay(orderBeforePay,
                createCreateBasketBalanceCallParams(cardId, shopMetaData),
                uid);

        checkReceipt();
    }

    public void initAndHoldPayment() throws Exception {
        initAndHoldPayment(false);
    }

    public void initAndHoldPayment(boolean moneyWallet) throws Exception {
        Long receiptId = initPayment();
        notifyPaymentSucceeded(receiptId, moneyWallet);
    }

    void cancelPayment() throws Exception {
        cancelPayment(false);
    }

    void cancelPayment(boolean moneyWallet) throws Exception {
        final boolean wasntNotified = !refreshOrder().getPayment().isHeld() && !refreshOrder().getPayment().isCleared();
        final boolean wasAlreadyFailed = refreshOrder().getPayment().isCancelled();
        final boolean wasFullyRefunded = order().getPayment().getRemainder(order()).compareTo(BigDecimal.ZERO) == 0;

        order.set(orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.CANCELLED,
                wasAlreadyFailed ? OrderSubstatus.USER_NOT_PAID : OrderSubstatus.USER_CHANGED_MIND,
                wasAlreadyFailed || wasFullyRefunded ? ClientInfo.SYSTEM : new ClientInfo(ClientRole.USER, order()
                        .getBuyer().getUid())));

        refreshOrder();
        assertThat(order().getStatus(), is(OrderStatus.CANCELLED));

        if (wasAlreadyFailed) {
            assertThat(order().getPayment().getStatus(), is(PaymentStatus.CANCELLED));
            receiptTestHelper.checkNoNewReceipts();
            return;
        }
        if (wasFullyRefunded) {
            trustMockConfigurer.resetRequests();
            mockBalanceCheckBasket(moneyWallet, refunded);
            tmsTaskHelper.runProcessHeldPaymentsTaskV2();

            assertThat(order().getPayment().getStatus(), is(PaymentStatus.CLEARED));
            receiptTestHelper.checkNoNewReceipts();
            return;
        }
        if (wasntNotified) {
            receiptTestHelper.checkNoNewReceipts();
            return;
        }

        trustMockConfigurer.resetRequests();
        mockBalanceCheckBasket(moneyWallet, order().getPayment().isCleared() ? cleared : authorized);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        refreshOrder();

        // если платеж был заклирен, то он так и остается, иначе должен отмениться
        assertThat(
                order().getPayment().getStatus(),
                is((moneyWallet) ? PaymentStatus.CLEARED : PaymentStatus.CANCELLED)
        );
        assertThat(order().getPayment().getSubstatus(), is((moneyWallet) ? null : PaymentSubstatus.UNHELD));

        boolean newPrepayType = isNewPrepayType();
        if (!moneyWallet) {
            refundHelper.proceedAsyncRefunds(order().getId());
            checkCompleteOrCancel(false, true, order().getPayment(), getUidForOrder());

            if (newPrepayType) {
                long receiptId = receiptTestHelper.checkNewReceiptForPayment(
                        ReceiptType.INCOME_RETURN, ReceiptStatus.NEW);

                trustMockConfigurer.mockCheckBasket(buildReversalWithRefundConfig(null));
                trustMockConfigurer.mockStatusBasket(buildReversalWithRefundConfig(null), null);
                receiptTestHelper.repairReceipts();
                receiptTestHelper.checkLastReceiptForPayment(
                        receiptId,
                        ReceiptType.INCOME_RETURN,
                        ReceiptStatus.PRINTED
                );
            }
        } else {
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
            Refund reversalRefund = refundHelper.proceedAsyncRefund(refundTestHelper.getLastRefund());
            assertThat(reversalRefund, is(notNullValue()));

            ExpectedRefund expectedRefund = refundTestHelper.expectRefund(order().getPayment(), reversalRefund.getId(),
                    reversalRefund.getTrustRefundId(), RefundStatus.SUCCESS, RefundReason.ORDER_CANCELLED,
                    newPrepayType ? refundableItemsFromOrder(order()) : null,
                    newPrepayType ? null : order().getBuyerTotal(),
                    ClientRole.SYSTEM, null, null);
            expectedRefund.setComment("Возврат денег при отмене заказа");
            refundTestHelper.checkBalanceCallsAfterRefund(expectedRefund);
            refundTestHelper.checkAfterRefund(expectedRefund);
        }
    }

    void mockBalanceCheckBasket(boolean moneyWallet,
                                BalancePaymentStatus basketStatus) {
        trustMockConfigurer.mockCheckBasket(buildCheckBasketConfig(basketStatus, moneyWallet));
        trustMockConfigurer.mockStatusBasket(buildCheckBasketConfig(basketStatus, moneyWallet), null);
    }

    /**
     * @param actionClear  признак того, проверяем ли мы что заказ был заклирен, или что по нему была отмена
     * @param expectUpdate true если ожидаем что отмену или клир, false - если ожидаем что не было вызовлв
     */
    void checkCompleteOrCancel(boolean actionClear, boolean expectUpdate, Payment payment, Long uid) {
        Iterator<ServeEvent> callIter = skipCheckBasketCalls(trustMockConfigurer.servedEvents());

        checkCompleteOrCancel(callIter, actionClear, expectUpdate, payment, uid);
    }

    void checkCompleteOrCancel(Iterator<ServeEvent> callIter, boolean actionClear,
                               boolean expectUpdate, Payment payment, Long uid) {
        if (expectUpdate) {
            if (actionClear) {
                checkBasketClearCall(callIter, uid, payment.getBasketKey());
            } else {
                checkBasketCancelCall(callIter, uid, payment.getBasketKey());
            }
        }
    }

    /**
     * @param ordersToDelivery    список заказов для переаода в деливери
     * @param ordersToBeCancelled список заказов для отмены
     */
    public void tryClearMultipayment(Collection<Order> ordersToDelivery, Collection<Order> ordersToBeCancelled) {
        //Отменяем один из заказов
        List<Order> cancelledOrders = ordersToBeCancelled.stream().map(order -> {
            Order cancelled = orderUpdateService.updateOrderStatus(
                    order.getId(),
                    OrderStatus.CANCELLED,
                    OrderSubstatus.CUSTOM);
            Assertions.assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
            return cancelled;
        }).collect(Collectors.toList());

        List<Order> deliveredOrders = ordersToDelivery.stream().map(
                o -> orderUpdateService.updateOrderStatus(o.getId(), OrderStatus.DELIVERY)
        ).collect(Collectors.toList());


        List<Order> allOrders = Stream.concat(
                cancelledOrders.stream(),
                deliveredOrders.stream()
        ).collect(Collectors.toList());
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(allOrders));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(allOrders), null);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        ordersToDelivery = orderService.getOrders(
                ordersToDelivery.stream().map(Order::getId).collect(Collectors.toList())
        ).values();
        ordersToDelivery.forEach(
                o -> {
                    assertThat(o.getStatus(), is(OrderStatus.DELIVERY));
                    assertThat(o.getPayment().getStatus(), is(PaymentStatus.CLEARED));
                }
        );
    }

    public void clearPayment() throws Exception {
        boolean wasClearedBefore = order().getPayment().isCleared();

        trustMockConfigurer.resetRequests();
        order.set(orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.DELIVERY));

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        checkCompleteOrCancel(true, !wasClearedBefore, order().getPayment(), getUidForOrder());

        refreshOrder();
        assertThat(order().getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order().getPayment().getStatus(), is(PaymentStatus.CLEARED));
    }

    void notifyPaymentSucceeded(Long receiptId, boolean moneyWallet) throws Exception {
        notifyPaymentSucceeded(receiptId, moneyWallet, true);
    }

    void notifyPaymentSucceeded(Long receiptId, boolean moneyWallet, boolean checkLastReceipt) throws Exception {
        notifyPaymentSucceeded(receiptId, moneyWallet, checkLastReceipt,
                (moneyWallet) ? PaymentStatus.CLEARED : PaymentStatus.HOLD);
    }

    void notifyPaymentSucceeded(Long receiptId,
                                boolean moneyWallet,
                                boolean checkLastReceipt,
                                PaymentStatus expectedPaymentStatus) throws Exception {
        notifyPaymentSucceeded(receiptId, moneyWallet, checkLastReceipt, expectedPaymentStatus, OrderStatus.PROCESSING);
    }

    void notifyPaymentSucceeded(Long receiptId, boolean moneyWallet, boolean checkLastReceipt,
                                PaymentStatus expectedPaymentStatus, OrderStatus expectedOrderStatus) throws
            Exception {
        notifyAndCheckPaymentNew(moneyWallet ? cleared : authorized,
                moneyWallet, expectedPaymentStatus, expectedOrderStatus);
        if (isNewPrepayType() && checkLastReceipt) {
            receiptTestHelper.checkLastReceiptForPayment(
                    receiptId,
                    ReceiptType.INCOME,
                    ReceiptStatus.PRINTED
            );
        }
    }

    public void notifyPaymentFailed(Long receiptId) throws Exception {
        notifyAndCheckPaymentNew(not_authorized, false, PaymentStatus.CANCELLED, OrderStatus.UNPAID);
        if (isNewPrepayType()) {
            receiptTestHelper.checkLastReceiptForPayment(
                    receiptId,
                    ReceiptType.INCOME,
                    ReceiptStatus.FAILED
            );
        }
    }

    public PagedPayments getPagedPayments(long orderId, PaymentGoal paymentGoal) throws Exception {
        MvcResult result = mockMvc.perform(get("/orders/{orderId}/payments", orderId)
                .param(CheckouterClientParams.PAYMENT_GOAL, paymentGoal.name())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
        ).andDo(log())
                .andReturn();

        return serializationService
                .deserializeCheckouterObject(result.getResponse().getContentAsString(), PagedPayments.class);
    }

    void checkBalanceCallsAfterPay(Order orderBeforePay,
                                   CreateBasketParams createBasketParams,
                                   Long uid) {
        checkBalanceCallsAfterPay(
                Collections.singletonList(orderBeforePay),
                Collections.singletonList(order()),
                createBasketParams,
                uid,
                Maps.newHashMap(orderBeforePay.getShopId(), shopMetaData())
        );
    }

    /**
     * Проверка вызовов Баланса.
     * - при оплате одного заказа
     * - мультиоплата нескольких заказов
     */
    void checkBalanceCallsAfterPay(Collection<Order> ordersBeforePay,
                                   Collection<Order> orders,
                                   CreateBasketParams createBasketParams,
                                   Long uid,
                                   Map<Long, ShopMetaData> shopMetaDataMap) {
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();

        Order firstOrder = ordersBeforePay.iterator().next();
        Order firstAfterPay = orders.stream().filter(o -> o.getId().equals(firstOrder.getId()))
                .findFirst().orElseThrow(() -> new OrderNotFoundException(firstOrder.getId()));

        PaymentGoal paymentGoal = firstAfterPay.getPayment().getType();
        final boolean needDivideByItems = isNeedDivideBasketByItemsInBalance(
                firstAfterPay, firstAfterPay.getPayment().getPrepayType(), checkouterFeatureReader);

        //NB: clientId = shopId = campaignId см. ShopSettingsHelper.createShopSettings

        List<CreateBalanceOrderParams> createOrdersBatchCallParams = new ArrayList<>();
        if (needDivideByItems) {
            for (Order orderBeforePay : ordersBeforePay.stream()
                    .sorted(Comparator.comparingLong(BasicOrder::getId)).collect(Collectors.toList())) {
                Order orderAfterPay = orders.stream().filter(o -> o.getId().equals(orderBeforePay.getId()))
                        .findFirst().orElse(null);
                ShopMetaData shopMetaData = shopMetaDataMap.get(orderBeforePay.getShopId());

                createOrdersBatchCallParams.addAll(checkBalanceCallsForFFItems(orderBeforePay, orderAfterPay,
                        callIter));
                checkBalanceCallsForFFDelivery(shopMetaData, orderBeforePay, orderAfterPay, callIter)
                        .ifPresent(createOrdersBatchCallParams::add);

            }
        } else {
            for (Order orderBeforePay : ordersBeforePay.stream()
                    .sorted(Comparator.comparingLong(BasicOrder::getId)).collect(Collectors.toList())) {
                Order orderAfterPay = orders.stream().filter(o -> o.getId().equals(orderBeforePay.getId()))
                        .findFirst().orElse(null);
                ShopMetaData shopMetaData = shopMetaDataMap.get(orderBeforePay.getShopId());

                if (orderBeforePay.getBalanceOrderId() != null) {
                    continue;
                }

                checkLoadPartnerCall(callIter, shopMetaData.getClientId());

                String name = String.valueOf(shopMetaData.getClientId());
                final String serviceProductId = shopMetaData.getCampaignId() + "_" + shopMetaData.getClientId();
                name += "-" + shopMetaData.getCampaignId();

                checkOptionalCreateServiceProductCall(
                        callIter,
                        product(shopMetaData.getClientId(), name, serviceProductId)
                );
                checkCreateServiceProductCached(serviceProductId, name, null, shopMetaData.getClientId());

                String expectedServiceOrderId = orderAfterPay.getId() + "-" + paymentGoal.name().toLowerCase();
                createOrdersBatchCallParams.add(new CreateBalanceOrderParams(
                        null, serviceProductId, orderAfterPay.getDisplayOrderId(),
                        notNullValue(PassParams.class), expectedServiceOrderId,
                        orderAfterPay.getCreationDate()
                ));

                assertThat(orderAfterPay.getBalanceOrderId(), is(expectedServiceOrderId));
            }
        }

        if (!createOrdersBatchCallParams.isEmpty()) {
            checkBatchServiceOrderCreationCall(callIter, uid(), createOrdersBatchCallParams);
        }
        TrustBasketKey basketKey = checkCreateBasketCall(callIter, createBasketParams);

        assertThat(firstAfterPay.getPayment().getBasketKey(), equalTo(basketKey));
        checkPayBasketCall(callIter, uid, basketKey);
        assertThat("Detected unexpected calls to Balance", callIter.hasNext(), is(false));
    }

    public void checkCreateServiceProductCached(String serviceProductId, String name, Integer fee, Long clientId) {
        StringBuilder queryBuilder = new StringBuilder("SELECT count(*) FROM service_product_cache " +
                "WHERE service_product_id = ? " +
                "  AND name = ? "
        );
        List<Object> args = new ArrayList<>() {{
            add(serviceProductId);
            add(name);
        }};
        if (fee != null) {
            queryBuilder = queryBuilder.append("  AND service_fee = ?");
            args.add(fee);
        } else {
            queryBuilder = queryBuilder.append("  AND service_fee IS NULL");
        }
        if (clientId != null) {
            queryBuilder = queryBuilder.append("  AND partner_id = ?");
            args.add(clientId);
        } else {
            queryBuilder = queryBuilder.append("  AND partner_id IS NULL");
        }
        // Не учитываем токен при поиске, т.к. в тестах он не известен
        long count = masterJdbcTemplate.query(queryBuilder.toString(), new LongRowMapper(), args.toArray(new Object[0]))
                .stream()
                .findFirst()
                .orElse(0L);

        assertThat("Cached calls count", count, greaterThan(0L));
    }

    CreateBasketParams createCreateBasketBalanceCallParams(String payMethodId,
                                                           @Nullable ShopMetaData shopMetaData) {
        return createCreateBasketBalanceCallParams(
                payMethodId,
                PAY_RETURN_PATH,
                Collections.singletonList(order()),
                shopMetaData
        );
    }

    CreateBasketParams createCreateBasketBalanceCallParams(
            String payMethodId, String returnPath, Collection<Order> orders, @Nullable ShopMetaData shopMetaData
    ) {
        Order firstAfterPay = orders.iterator().next();
        final CreateBasketParams createBasket = generateBasketParamsBuilder(payMethodId,
                firstAfterPay, returnPath, true);

        if (shopMetaData != null) {
            createBasket
                    .withSupplierInn(shopMetaData.getInn())
                    .withDeliveryInn(shopMetaData.getInn());
        }

        final boolean needDivideBasketByItems = firstAfterPay.getPayment() != null
                && firstAfterPay.getPayment().getPrepayType() != null
                && firstAfterPay.getPayment().getPrepayType().isNeedToMapItemToBalance()
                || checkouterFeatureReader.getBoolean(NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS);
        SwitchWithWhitelist<Long> fiscalAgentTypeList =
                checkouterFeatureReader.getAsTargetType(FISCAL_AGENT_TYPE_ENABLED, new TypeReference<>() { });
        boolean fiscalAgentTypeEnabled = fiscalAgentTypeList.enabledFor(firstAfterPay.getBuyer().getUid());
        if (fiscalAgentTypeEnabled) {
            createBasket.withFiscalAgentType(FiscalAgentType.NON_AGENT.getTrustAgentType());
        }
        for (Order orderAfterPay : orders) {
            if (needDivideBasketByItems) {
                createBasket.withOrdersByItemsAndDelivery(orderAfterPay, fiscalAgentTypeEnabled);
            } else {
                createBasket.withOrder(orderAfterPay.getBalanceOrderId(), BigDecimal.ONE,
                        orderAfterPay.getBuyerTotal());
            }
            if (orderAfterPay.getProperty(OrderPropertyType.FORCE_THREE_DS) == null) {
                createBasket.withPassParams(containsString("\"market_blue_3ds_policy\":\"UNKNOWN\""));
            }
        }


        createBasket.withUserEmail(firstAfterPay.getBuyer().getEmail());
        return createBasket;
    }

    CreateBasketParams createCreateCashBasketBalanceCallParams() {
        CreateBasketParams createBasket = generateBasketParamsBuilder("cash-12344321", null, false);

        if (isNewPrepayType()) {
            order().getItems().forEach(i -> createBasket.withOrder(i.getBalanceOrderId(),
                    i.getQuantityIfExistsOrCount(),
                    i.getQuantPriceIfExistsOrBuyerPrice(),
                    i.getOfferName(),
                    i.getVat().getTrustId(),
                    DEFAULT_SUPPLIER_INN,
                    i.getSupplierType() == SupplierType.FIRST_PARTY ? "none_agent" : "agent"));

            if (!order().getDelivery().isFree()) {
                Delivery delivery = order().getDelivery();

                createBasket.withOrder(
                        delivery.getBalanceOrderId(),
                        BigDecimal.ONE,
                        financialService.getDeliveryBuyerPrice(delivery),
                        "Доставка",
                        delivery.getVat().getTrustId(),
                        DEFAULT_MARKET_INN,
                        "none_agent");
            }
        } else {
            createBasket.withOrder(order().getBalanceOrderId(), BigDecimal.ONE, order().getBuyerTotal());
        }

        return createBasket;
    }

    void checkAgencyCommission() {
        Order resultOrder = orderService.getOrder(order.get().getId());
        resultOrder.getItems().forEach(item -> assertThat(item.getAgencyCommission(), not(nullValue()))
        );
    }

    private List<CreateBalanceOrderParams> checkBalanceCallsForFFItems(
            Order orderBefore, Order orderAfter, OneElementBackIterator<ServeEvent> callIter) {
        List<CreateBalanceOrderParams> result = new ArrayList<>();
        for (OrderItem item : orderAfter.getItems()) {
            OrderItem itemBefore = orderBefore.getItem(item.getId());

            if (itemBefore.getBalanceOrderId() != null) { // балансовый ордер уже создался, не создаем повторно
                continue;
            }

            //NB: clientId = shopId = campaignId см. ShopSettingsHelper.createCustomNewPrepayMeta
            Long shopId = item.getSupplierId();
            if (item.getSupplierType() != SupplierType.FIRST_PARTY) {
                checkLoadPartnerCall(callIter, shopId);

                String serviceProductId = shopId + "_" + shopId;
                checkOptionalCreateServiceProductCall(callIter, TrustCallsParamsProvider.productFrom3PItem(item));
                result.add(createServiceOrderCreateCallParams(orderBefore, item, serviceProductId));
            } else {
                TrustServiceFee fee = TrustServiceFee.SERVICE_FEE;
                String serviceProductId = colorConfig.getFor(order()).get1PProductId(fee);
                int serviceFeeId = colorConfig.getFor(order()).getServiceFeeId(fee);

                checkOptionalCreateServiceProductCall(
                        callIter,
                        product(MARKET_PARTNER_ID, serviceProductId, serviceProductId, serviceFeeId)
                );
                checkCreateServiceProductCached(serviceProductId, serviceProductId, serviceFeeId, MARKET_PARTNER_ID);
                result.add(createServiceOrderCreateCallParams(orderBefore, item, serviceProductId));
            }

            result.addAll(checkBalanceCallsForItemServices(orderBefore, itemBefore, item, callIter));
        }

        return result;
    }

    private List<CreateBalanceOrderParams> checkBalanceCallsForItemServices(
            Order orderBefore, OrderItem itemBefore,
            OrderItem itemAfter,
            OneElementBackIterator<ServeEvent> callIter) {
        List<CreateBalanceOrderParams> result = new ArrayList<>();
        for (ItemService serviceAfter : itemAfter.getServices()) {
            ItemService serviceBefore = itemBefore.getServices().stream()
                    .filter(it -> Objects.equals(it.getId(), serviceAfter.getId())).findFirst().orElse(null);

            if (serviceBefore.getBalanceOrderId() != null) { // балансовый ордер уже создался, не создаем повторно
                continue;
            }

            String serviceProductId = colorConfig.getFor(order()).getMarketplaceServicesProductId();
            checkOptionalCreateServiceProductCall(
                    callIter,
                    product(MARKET_PARTNER_ID, serviceProductId, serviceProductId, 1)
            );
            result.add(createServiceOrderCreateCallParams(orderBefore, itemBefore, serviceBefore, serviceProductId));
        }
        return result;
    }

    private Optional<CreateBalanceOrderParams> checkBalanceCallsForFFDelivery(
            ShopMetaData shopMetaData, Order orderBeforePay, Order orderAfter,
            OneElementBackIterator<ServeEvent> callIter) {
        Delivery delivery = orderBeforePay.getDelivery();

        if (delivery.isFree() || delivery.getBalanceOrderId() != null) {
            return Optional.empty();
        }

        String name;
        String serviceProductId;
        //TODODSBS do after MARKETCHECKOUT-15260
        if (orderBeforePay.getRgb() != Color.BLUE) {
            serviceProductId = orderBeforePay.getShopId() + "_" + orderBeforePay.getShopId(); //bug - duplicated shop_id
            name = orderBeforePay.getShopId() + "-" + orderBeforePay.getShopId();

            checkLoadPartnerCall(callIter, orderBeforePay.getShopId());
            checkOptionalCreateServiceProductCall(
                    callIter,
                    product(orderBeforePay.getShopId(), name, serviceProductId)
            );
            checkCreateServiceProductCached(serviceProductId, name, null, orderBeforePay.getShopId());
        } else {
            serviceProductId = "BLUE_MARKET_DELIVERY";
            name = serviceProductId;
            checkOptionalCreateServiceProductCall(callIter, product(MARKET_PARTNER_ID, name, serviceProductId, 1));
            checkCreateServiceProductCached(serviceProductId, name, 1, MARKET_PARTNER_ID);
        }

        String balanceOrderId = orderBeforePay.getId() + "-delivery";
        assertThat(orderAfter.getDelivery().getBalanceOrderId(), is(balanceOrderId));

        return Optional.of(new CreateBalanceOrderParams(
                shopMetaData.getAgencyCommission(),
                serviceProductId,
                orderBeforePay.getDisplayOrderId(),
                notNullValue(PassParams.class),
                balanceOrderId,
                orderBeforePay.getCreationDate()
        ));
    }

    private CreateBalanceOrderParams createServiceOrderCreateCallParams(Order order, OrderItem item,
                                                                        String serviceProductId) {
        String balanceOrderId = order.getId() + "-item-" + item.getId();
        Long shopId = order.isFulfilment() ? item.getSupplierId() : order.getShopId();

        assertThat(item.getBalanceOrderId(), is(balanceOrderId));

        return new CreateBalanceOrderParams(
                shopService.getMeta(shopId).getAgencyCommission() != null ? 0 : null,
                serviceProductId,
                order.getDisplayOrderId(),
                notNullValue(PassParams.class),
                balanceOrderId,
                order.getCreationDate()
        );
    }

    private CreateBalanceOrderParams createServiceOrderCreateCallParams(Order order, OrderItem item,
                                                                        ItemService itemService,
                                                                        String serviceProductId) {
        String balanceOrderId = order.getId() + "-itemService-" + itemService.getId();
        return new CreateBalanceOrderParams(
                null,
                serviceProductId,
                order.getDisplayOrderId(),
                notNullValue(PassParams.class),
                balanceOrderId,
                order.getCreationDate()
        );
    }

    private void notifyAndCheckPaymentNew(BalancePaymentStatus balancePaymentStatus,
                                          boolean moneyWallet,
                                          PaymentStatus expectedPaymentStatus,
                                          OrderStatus expectedOrderStatus) throws Exception {
        String basketId = order().getPayment().getBasketKey().getBasketId();
        String purchaseToken = order().getPayment().getBasketKey().getPurchaseToken();
        mockBalanceCheckBasket(moneyWallet, balancePaymentStatus);
        mockMvc.perform(
                post(
                        "/payments/{paymentId}/notify-basket?status={status}&trust_payment_id={basketId" +
                                "}&purchase_token={purchaseToken}",
                        order().getPaymentId(),
                        balancePaymentStatus.name(),
                        basketId,
                        purchaseToken
                )
        ).andExpect(status().isOk());

        checkPaymentResponse(
                mockMvc.perform(withUserRole(get("/payments/{paymentId}", order().getPaymentId()), order()))
                        .andExpect(status().isOk()), order(), basketId)
                .andExpect(jsonPath("$.id").value(order().getPaymentId().intValue()))
                .andExpect(jsonPath("$.status").value(expectedPaymentStatus.name()));

        order.set(orderService.getOrder(order().getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES)));
        assertThat(order().getStatus(), is(expectedOrderStatus));

        if (balancePaymentStatus == authorized) {
            assertThat(order().getPayment().getBalancePayMethodType(), is(moneyWallet ? "yandex_money" : "card"));
            assertThat(order().getPayment().getCardNumber(), is(moneyWallet ? null : "500000****0009"));
        }
    }

    private ResultActions checkPaymentResponse(ResultActions resultActions,
                                               Order o,
                                               String expectedBasketId) throws Exception {
        ResultActions expectedActions = resultActions
                .andExpect(jsonPath("$.orderId").value(o.getId().intValue()))
                .andExpect(jsonPath("$.basketId").value((expectedBasketId != null) ? is(expectedBasketId) :
                        any(String.class)))
                .andExpect(jsonPath("$.totalAmount").value(o.getBuyerTotal().intValue()))
                .andExpect(jsonPath("$.prepayType").value(shopMetaData().getPrepayType().name()));
        if (Boolean.TRUE.equals(o.isNoAuth())) {
            expectedActions
                    .andExpect(jsonPath("$.uid").doesNotExist());
        } else {
            expectedActions
                    .andExpect(jsonPath("$.uid").value(o.getBuyer().getUid().intValue()));
        }
        return expectedActions;
    }

    private CreateBasketParams generateBasketParamsBuilder(
            String paymethodId, String returnPath, boolean addTimeout) {
        return generateBasketParamsBuilder(paymethodId, order(), returnPath, addTimeout);
    }

    CreateBasketParams generateBasketParamsBuilder(
            String paymethodId, Order order, String returnPath, boolean addTimeout
    ) {
        return generateBasketParamsBuilder(paymethodId, order, returnPath,
                endsWith("/payments/" + order.getPaymentId() + "/notify-basket"), true, addTimeout);
    }

    public CreateBasketParams generateBasketParamsBuilder(
            String paymethodId, Order order, String returnPath, Matcher<String> backUrl,
            boolean addYandexUid, boolean addTimeout) {
        CreateBasketParams basket = createBasket()
                .withPayMethodId(StringUtils.isNotBlank(paymethodId) ? paymethodId : "trust_web_page")
                .withBackUrl(backUrl)
                .withCurrency(order.getBuyerCurrency());
        if (addYandexUid) {
            basket.withYandexUid(order.getBuyer().getYandexUid());
        }
        if (!Boolean.TRUE.equals(order.isNoAuth())) {
            basket.withUid(order.getBuyer().getUid());
        }
        if (addTimeout) {
            basket.withPaymentTimeout(anything());
        }
        if (returnPath != null) {
            basket.withReturnPath(returnPath);
        }
        if (spasiboValidator.availableForOrder(order)
                && PaymentGoalUtils.getPaymentGoal(order) == PaymentGoal.ORDER_PREPAY) {
            Matcher<String> maxMinSpasiboMatcher = allOf(
                    containsString("max_spasibo_amount"),
                    containsString("min_spasibo_amount"));

            basket.withDeveloperPayload(maxMinSpasiboMatcher);
            basket.withPassParams(notNullValue(String.class));

        }
        if (PaymentGoalUtils.getPaymentGoal(order) == PaymentGoal.ORDER_PREPAY) {
            basket.withSpasiboOrderMap(order.getItems().stream()
                    .collect(Collectors.toMap(OrderItem::getBalanceOrderId, OrderItem::getShopSku)));
        }
        if (order.getProperty(OrderPropertyType.FORCE_THREE_DS) == null) {
            basket.withPassParams(containsString("\"market_blue_3ds_policy\":\"UNKNOWN\""));
        }

        return basket;
    }

    private Long getUidForOrder() {
        return Boolean.TRUE.equals(order().isNoAuth()) ? null : order().getBuyer().getUid();
    }

    PaymentMarkup markupPayment(PaymentPartitions paymentPartition) throws Exception {
        trustMockConfigurer.resetRequests();
        Payment payment = order().getPayment();
        PaymentMarkup markup = serializationService.deserializeCheckouterObject(
                mockMvc.perform(post("/payments/{purchaseToken}/markup",
                        payment.getBasketKey().getPurchaseToken())
                        .content(serializationService.serializeCheckouterObject(paymentPartition))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                PaymentMarkup.class);
        checkMarkupResponse(markup, payment.getId());
        return markup;
    }

    private void checkMarkupResponse(PaymentMarkup markup, Long paymentId) {
        BigDecimal partitionsSum = markup.getContent().stream().map(partition ->
                partition.getPartitions()
                        .stream()
                        .map(ReceiptItemPartition::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal buyerTotal = orderService.getOrdersByPayment(paymentId, ClientInfo.SYSTEM)
                .stream()
                .map(Order::getBuyerTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(partitionsSum, equalTo(buyerTotal));
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();
        Collection<BasketItemPartition> basketPartitions = markup.getContent();
        MarkupBasketParams params = markup(
                equalTo(order().getPayment().getBasketKey().getPurchaseToken()),
                order().getUid()
        );
        basketPartitions.forEach(basketPartition -> {
            params.withLine(
                    basketPartition.getOrderItemServiceOrderId(),
                    getMarkupBasketLineParamsFromPartition(basketPartition));
        });
        TrustCallsChecker.checkMarkupBasketCall(callIter, params);
    }

    private MarkupBasketLineParams getMarkupBasketLineParamsFromPartition(BasketItemPartition basketPartition) {
        MarkupBasketLineParams result = line();
        basketPartition.getPartitions()
                .forEach(p -> result.withPaymethod(p.getPaymentAgent().getTrustPaymentMethod(), p.getAmount()));
        return result;
    }

    public SwitchWithWhitelist<Long> updateFiscalAgentTypeEnabled(Boolean enabled, Set<Long> whitelist) {
        SwitchWithWhitelist<Long> fiscalAgentTypeList =
                checkouterFeatureReader.getAsTargetType(FISCAL_AGENT_TYPE_ENABLED, new TypeReference<>() { });

        var value = new SwitchWithWhitelist<>(enabled, whitelist);
        return Optional.ofNullable(fiscalAgentTypeList)
                .map(v -> v.patch(value))
                .orElse(value);
    }
}
