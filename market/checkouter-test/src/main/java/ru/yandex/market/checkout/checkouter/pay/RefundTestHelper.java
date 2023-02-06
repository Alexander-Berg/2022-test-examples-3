package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.CoreMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams;

import static java.math.BigDecimal.ZERO;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.SHOP_UID;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.ACCEPTED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.DRAFT;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.RETURNED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.WAIT_FOR_MANUAL_APPROVAL;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalWithRefundConfig;
import static ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams.refund;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkDoRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.skipCheckBasketCalls;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * @author mkasumov
 * @deprecated use {@link RefundHelper} instead
 */
@Deprecated
public class RefundTestHelper extends AbstractPaymentTestHelper {

    private static final RefundReason DEFAULT_REFUND_REASON = RefundReason.ORDER_CHANGED;
    @Autowired
    private RefundService refundService;
    @Autowired
    protected CheckouterProperties checkouterProperties;
    @Autowired
    protected CheckouterFeatureReader checkouterFeatureReader;
    @Autowired
    protected RefundHelper refundHelper;

    private ReceiptTestHelper receiptTestHelper;


    public RefundTestHelper(AbstractWebTestBase test, ReceiptTestHelper receiptTestHelper,
                            Has<Order> order, Has<ShopMetaData> shopMetaData) {
        super(test, order, shopMetaData);
        this.receiptTestHelper = receiptTestHelper;
    }

    public void checkRefundableItems(RefundableItems expectRefItems) throws Exception {
        ResultActions result = mockMvc.perform(withUserRole(get("/orders/{orderId}/refundable-items",
                order().getId()
        ), order()))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRefundAmount").value(is(expectRefItems.canRefundAmount())));

        for (RefundableItem i : expectRefItems.getItems()) {
            String itemPath = "$.items[?(@.feedId==" + i.getFeedId() + " && @.offerId=='" + i.getOfferId() + "')]";
            result.andExpect(jsonPath(itemPath + ".feedCategoryId").value(hasItem(is(i.getFeedCategoryId()))))
                    .andExpect(jsonPath(itemPath + ".categoryId").value(hasItem(is(i.getCategoryId()))))
                    .andExpect(jsonPath(itemPath + ".wareMd5").value(hasItem(is(i.getWareMd5()))))
                    .andExpect(jsonPath(itemPath + ".offerName").value(hasItem(is(i.getOfferName()))))
                    .andExpect(jsonPath(itemPath + ".description").value(hasItem(is(i.getDescription()))))
                    .andExpect(jsonPath(itemPath + ".buyerPrice").value(hasItem(numberEqualsTo(i.getBuyerPrice()))))
                    .andExpect(jsonPath(itemPath + ".price").value(hasItem(numberEqualsTo(i.getPrice()))))
                    .andExpect(jsonPath(itemPath + ".count").value(hasItem(is(i.getCount()))))
                    .andExpect(jsonPath(itemPath + ".modelId").value(hasItem(numberEqualsTo(i.getModelId()))))
                    .andExpect(jsonPath(itemPath + ".refundableCount").value(hasItem(is(i.getRefundableCount()))))
                    .andExpect(jsonPath(itemPath + ".id").value(hasItem(is(i.getId().intValue()))))
                    .andExpect(jsonPath(itemPath + ".quantity")
                            .value(hasItem(numberEqualsTo(i.getQuantityIfExistsOrCount()))))
                    .andExpect(jsonPath(itemPath + ".quantPrice")
                            .value(hasItem(numberEqualsTo(i.getQuantPriceIfExistsOrBuyerPrice()))))
                    .andExpect(jsonPath(itemPath + ".refundableQuantity")
                            .value(hasItem(numberEqualsTo(i.getRefundableQuantityIfExistsOrRefundableCount()))));
        }

        RefundableDelivery dlv = expectRefItems.getDelivery();
        String dlvPath = "$.delivery";
        result.andExpect(jsonPath(dlvPath + ".type").value(is(dlv.getType().name())))
                .andExpect(jsonPath(dlvPath + ".serviceName").value(is(dlv.getServiceName())))
                .andExpect(jsonPath(dlvPath + ".price").value(numberEqualsTo(dlv.getPrice())))
                .andExpect(jsonPath(dlvPath + ".buyerPrice").value(numberEqualsTo(dlv.getBuyerPrice())))
                .andExpect(jsonPath(dlvPath + ".refundable").value(is(dlv.isRefundable())))
                .andExpect(jsonPath(dlvPath + ".length()").value(is(6)));
    }

    public RefundableItems checkRefundableItems() throws Exception {
        RefundableItems refItems = refundableItemsFromOrder(order());
        checkRefundableItems(refItems);
        return refItems;
    }

    @Nonnull
    public static RefundableItems refundableItemsFromOrder(Order order) {
        RefundableItems refItems = new RefundableItems();
        refItems.setCanRefundAmount(!isNewPrepayType(order.getPayment()));
        refItems.setItems(order.getItems().stream().map(RefundableItem::new).collect(toList()));
        List<RefundableItemService> refundableItemServices = order.getItems()
                .stream()
                .map(OrderItem::getServices)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(itemService -> itemService.getPaymentType() == PaymentType.PREPAID)
                .map(RefundableItemService::new)
                .collect(toList());
        refItems.setItemServices(refundableItemServices);
        refItems.setDelivery(new RefundableDelivery(order.getDelivery()));
        refItems.getDelivery().setRefundable(!order.getDelivery().isFree());
        return refItems;
    }

    public void checkRefunds(Return ret) {
        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds, CoreMatchers.notNullValue());
        assertThat(refunds, not(empty()));
        BigDecimal expectedTotal = calcTotalRefundAmount(ret);
        BigDecimal actualTotal = refunds.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(actualTotal, comparesEqualTo(expectedTotal));
    }

    private BigDecimal calcTotalRefundAmount(Return ret) {
        BigDecimal totalRefund = BigDecimal.ZERO;
        for (ReturnItem retItem : ret.getItems()) {
            if (retItem.isDeliveryService()) {
                totalRefund = totalRefund.add(order().getDelivery().getPrice());
            } else {
                BigDecimal totalPrice = order().getItem(retItem.getItemId()).getQuantPriceIfExistsOrBuyerPrice()
                        .multiply(retItem.getQuantityIfExistsOrCount());
                totalRefund = totalRefund.add(totalPrice);
            }
        }
        return totalRefund;
    }

    public void checkPostpaidRefundsSuccess(Return ret) {
        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds, CoreMatchers.notNullValue());
        assertThat(refunds, hasSize(1));
        assertThat(refunds.iterator().next().getStatus(), equalTo(RefundStatus.SUCCESS));
    }

    public long makeFullRefund() throws Exception {
        if (isNewPrepayType()) {
            return makeRefundForAllItems();
        } else {
            return makeRefundForWholeAmount();
        }
    }

    private long makeRefundForAllItems() throws Exception {
        return makeRefundForItems(refundableItemsFromOrder(order()));
    }

    private long makeRefundForWholeAmount() throws Exception {
        BigDecimal refundableAmount = order().getBuyerTotal()
                .subtract(avoidNull(order().getRefundActual(), ZERO))
                .subtract(avoidNull(order().getRefundPlanned(), ZERO));
        return makeRefundForAmount(refundableAmount);
    }

    long makeRefundForItems(RefundableItems itemsToRefund) throws Exception {
        return tryMakeRefundForItems(itemsToRefund, SC_OK);
    }

    long makeRefundForAmount(BigDecimal refundAmount) throws Exception {
        return tryMakeRefundForAmount(refundAmount, SC_OK);
    }

    long tryMakeRefundForItems(int expectedResponseStatus) throws Exception {
        return tryMakeRefundForItems(refundableItemsFromOrder(order()), expectedResponseStatus);
    }

    long tryMakeRefundForItems(RefundableItems itemsToRefund, int expectedResponseStatus) throws Exception {
        return tryMakeRefund(itemsToRefund, ZERO, expectedResponseStatus, false);
    }

    @Deprecated
    long tryMakeRefundForAmount(BigDecimal refundAmount, int expectedResponseStatus) throws Exception {
        return tryMakeRefund(null, refundAmount, expectedResponseStatus, false);
    }

    long tryMakeRefund(@Nullable RefundableItems itemsToRefund,
                       @Nonnull BigDecimal refundAmount,
                       int expectedResponseStatus,
                       boolean isPartialRefund) throws Exception {
        ResultActions result = createRefund(itemsToRefund, refundAmount, expectedResponseStatus, isPartialRefund);
        if (expectedResponseStatus != SC_OK) {
            return 0;
        }
        ExpectedRefund expectedRefund = expectRefund(order().getPayment(), null, null, ACCEPTED,
                DEFAULT_REFUND_REASON,
                itemsToRefund, refundAmount, ClientRole.SHOP, order().getShopId(), SHOP_UID);
        expectedRefund.setPartialRefund(isPartialRefund);
        if (refundHelper.isAsyncRefundStrategyEnabled(order().getPayment().getType())) {
            ExpectedRefund expectedAsyncRefund = expectRefund(order().getPayment(), null, null, DRAFT,
                    DEFAULT_REFUND_REASON, itemsToRefund, refundAmount, ClientRole.SHOP, order().getShopId(), SHOP_UID);
            expectedRefund.setPartialRefund(isPartialRefund);
            checkRefundResponse(result, expectedAsyncRefund);

            expectedRefund.setId(JsonPath
                    .parse(result.andReturn().getResponse().getContentAsString())
                    .read("$.id", Long.class));
            refundHelper.proceedAsyncRefund(expectedRefund);
            result = getRefund(expectedRefund.getId());
        }
        expectedRefund.setTrustRefundKey(checkBalanceCallsAfterRefund(expectedRefund));
        long refundId = checkRefundResponse(result, expectedRefund);

        expectedRefund.setId(refundId);
        expectedRefund.setStatus(SUCCESS);
        checkAfterRefund(expectedRefund);

        return refundId;
    }

    private ResultActions createRefund(@Nullable RefundableItems itemsToRefund,
                                       @Nonnull BigDecimal refundAmount,
                                       int expectedResponseStatus,
                                       boolean isPartialRefund) throws Exception {
        trustMockConfigurer.resetRequests();
        final MockHttpServletRequestBuilder request = RefundRequests.refundRequest(
                itemsToRefund, refundAmount, order(), RefundReason.ORDER_CHANGED, isPartialRefund);
        return mockMvc.perform(request)
                .andExpect(status().is(expectedResponseStatus));
    }

    public void checkRefundError(MockHttpServletRequestBuilder request,
                                 int expectedResponseCode,
                                 String message)
            throws Exception {
        trustMockConfigurer.resetRequests();
        mockMvc.perform(request)
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(expectedResponseCode))
                .andExpect(content().string(containsString(message)));
    }

    public void checkAfterRefund(ExpectedRefund expectedRefund) throws Exception {
        if (isNewPrepayType()) {
            receiptTestHelper.checkNewReceiptForRefund(ReceiptType.INCOME_RETURN, expectedRefund.getId(),
                    expectedRefund.getRefundedItems(), ReceiptStatus.NEW, false);
        } else {
            if (checkouterFeatureReader.getBoolean(NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS)) {
                receiptTestHelper.checkNewReceiptForRefund(ReceiptType.INCOME_RETURN, expectedRefund.getId(),
                        expectedRefund.getRefundedItems(), ReceiptStatus.GENERATED, expectedRefund.isPartialRefund());
            }
        }
        notifyAndCheckRefund(BasketStatus.success, expectedRefund);
        checkRefundAmountsInOrder();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public ExpectedRefund expectRefund(Payment payment, Long refundId, String trustRefundId,
                                       RefundStatus expectedRefundStatus, RefundReason expectedReason,
                                       RefundableItems itemsToRefund, BigDecimal refundAmount,
                                       ClientRole createdByRole, Long createdBy, Long shopManagerId) {
        ExpectedRefund refund = new ExpectedRefund();
        refund.setId(refundId);
        refund.setPayment(payment);
        refund.setTrustRefundKey(new TrustRefundKey(trustRefundId));
        refund.setStatus(expectedRefundStatus);
        refund.setReason(expectedReason);
        refund.setCreatedByRole(createdByRole);
        refund.setCreatedBy(createdBy);
        refund.setShopManagerId(shopManagerId);
        refund.setRefundedItems(itemsToRefund);
        refund.setAmount(refundAmount);
        return refund;
    }

    private long checkRefundResponse(ResultActions response, ExpectedRefund expectedRefund) throws Exception {
        refreshOrder();
        BigDecimal refundedAmount = isNewPrepayType()
                ? expectedRefund.getRefundedItems().getRefundableAmount()
                : expectedRefund.getAmount();
        Long expectedRefundId = expectedRefund.getId();
        response.andExpect(jsonPath("$.id", (expectedRefundId != null ? numberEqualsTo(expectedRefundId) :
                notNullValue(Number.class))))
                .andExpect(jsonPath("$.orderId", numberEqualsTo(order().getId())))
                .andExpect(jsonPath("$.paymentId", numberEqualsTo(expectedRefund.getPayment().getId())))
                .andExpect(jsonPath("$.fake", is(false)))
                .andExpect(jsonPath("$.currency", is(order().getBuyerCurrency().name())))
                .andExpect(jsonPath("$.amount", numberEqualsTo(refundedAmount)))
                .andExpect(jsonPath("$.reason", is(expectedRefund.getReason().name())))
                .andExpect(jsonPath("$.status", is(expectedRefund.getStatus().name())))
                .andExpect(jsonPath("$.createdByRole", is(expectedRefund.getCreatedByRole().name())));
        if (expectedRefund.getTrustRefundId() != null) {
            response.andExpect(jsonPath("$.trustRefundId", is(expectedRefund.getTrustRefundId())));
        } else {
            response.andExpect(jsonPath("$.trustRefundId").doesNotExist());
        }
        if (expectedRefund.getCreatedBy() != null) {
            response.andExpect(jsonPath("$.createdBy", numberEqualsTo(expectedRefund.getCreatedBy())));
        }
        String responseBody = response.andReturn().getResponse().getContentAsString();
        return (((Number) JsonPath.read(responseBody, "$.id")).longValue());
    }

    public TrustRefundKey checkBalanceCallsAfterRefund(ExpectedRefund expectedRefund) throws Exception {
        Iterator<ServeEvent> callIter = skipCheckBasketCalls(trustMockConfigurer.servedEvents());

        CreateRefundParams refundCheckerConfig = refund(expectedRefund.getPayment().getBasketKey())
                .withUserIp("127.0.0.1")
                .withReason(expectedRefund.getComment() != null ?
                        expectedRefund.getComment() : "Reason: " + expectedRefund.getReason().name()
                );

        if (isNewPrepayType(expectedRefund.getPayment()) &&
                receiptTestHelper.paymentHasReceipt(expectedRefund.getPayment())) {
            expectedRefund.getRefundedItems().getItems().forEach(i ->
                    refundCheckerConfig.withRefundLine(
                            order().getItem(i.getId()).getBalanceOrderId(),
                            i.getRefundableQuantityIfExistsOrRefundableCount(),
                            i.getQuantPriceIfExistsOrBuyerPrice()
                                    .multiply(i.getRefundableQuantityIfExistsOrRefundableCount())
                    )
            );
            expectedRefund.getRefundedItems().getItemServices().forEach(service ->
                    refundCheckerConfig.withRefundLine(
                            order().getItemServicesMapById().get(service.getId()).getBalanceOrderId(),
                            BigDecimal.valueOf(service.getRefundableCount()),
                            service.getPrice().multiply(new BigDecimal(service.getRefundableCount()))
                    )
            );

            if (expectedRefund.getRefundedItems().getDelivery() != null
                    && expectedRefund.getRefundedItems().getDelivery().isRefundable()) {
                refundCheckerConfig.withRefundLine(
                        order().getDelivery().getBalanceOrderId(),
                        BigDecimal.ONE,
                        expectedRefund.getRefundedItems().getDelivery().getBuyerPrice());
            }
        } else {
            if (expectedRefund.isPartialRefund()) {
                expectedRefund.getRefundedItems().getItems().forEach(i ->
                        refundCheckerConfig.withRefundLine(
                                order().getItem(i.getId()).getBalanceOrderId(),
                                ZERO,
                                expectedRefund.getAmount()
                        ));
            } else {
                if (expectedRefund.getAmount() == null && expectedRefund.getRefundedItems() != null) {
                    expectedRefund.setAmount(expectedRefund.getRefundedItems().getRefundableAmount());
                }
                refundCheckerConfig.withRefundLine(order().getBalanceOrderId(), ZERO,
                        expectedRefund.getAmount());
            }
        }

        String trustRefundId = checkCreateRefundCall(callIter, expectedRefund.getPayment().getUid(),
                refundCheckerConfig);
        var isAsyncRefund = refundHelper.isAsyncRefundStrategyEnabled(order().getPayment().getType());
        checkDoRefundCall(callIter, trustRefundId, expectedRefund.getPayment().getUid());

        return new TrustRefundKey(trustRefundId);
    }

    public void notifyAndCheckRefund(BasketStatus basketStatus, ExpectedRefund expectedRefund) throws Exception {
        Refund refund = refundService.getRefund(expectedRefund.getId());
        notifyRefund(basketStatus, refund, buildReversalWithRefundConfig(refund.getTrustRefundId()));
        checkRefundResponse(expectedRefund);
    }

    public void checkRefundResponse(ExpectedRefund expectedRefund) throws Exception {
        ResultActions result = getRefund(expectedRefund.getId());
        checkRefundResponse(result, expectedRefund);
    }

    @Nonnull
    private ResultActions getRefund(long refundId) throws Exception {
        return mockMvc.perform(withUserRole(get("/orders/{orderId}/refunds/{refundId}",
                order().getId(), refundId), order()))
                .andExpect(status().isOk());
    }

    public void notifyRefund(BasketStatus basketStatus, Refund refund, CheckBasketParams mockConfig)
            throws Exception {
        trustMockConfigurer.mockCheckBasket(mockConfig);
        trustMockConfigurer.mockStatusBasket(mockConfig, null);
        mockMvc.perform(post("/payments/{paymentId}/notify?status={status}" +
                        "&trust_refund_id={refundId}&mode={mode}",
                order().getPaymentId(), basketStatus, refund.getTrustRefundId(),
                NotificationMode.refund_result))
                .andExpect(status().isOk());
    }

    public void checkRefundAmountsInOrder() {
        Collection<Refund> refunds = refundService.getRefunds(order().getId());
        refunds =
                refunds.stream().filter(r -> Objects.equals(r.getPaymentId(), order().getPayment().getId()))
                        .collect(toList());

        BigDecimal succeededAmount = refunds.stream().filter(r -> r.getStatus() == SUCCESS).map(Refund::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal plannedAmount =
                refunds.stream().filter(r -> r.getStatus() == RETURNED || r.getStatus() == ACCEPTED
                        || r.getStatus() == WAIT_FOR_MANUAL_APPROVAL).map(Refund::getAmount)
                        .reduce(ZERO, BigDecimal::add);
        Order freshOrder = orderService.getOrder(order().getId());
        assertThat(avoidNull(freshOrder.getRefundPlanned(), ZERO), numberEqualsTo(plannedAmount));
        assertThat(avoidNull(freshOrder.getRefundActual(), ZERO), numberEqualsTo(succeededAmount));
    }

    public Refund getLastRefund() {
        return refundService.getRefunds(order().getId()).stream().max(comparingLong(Refund::getId)).orElse(null);
    }
}
