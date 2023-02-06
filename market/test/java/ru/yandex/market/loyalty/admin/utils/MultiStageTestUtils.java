package ru.yandex.market.loyalty.admin.utils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.returns.PagedReturns;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.loyalty.admin.config.BlackboxYandexTeam;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.cashback.CashbackService;
import ru.yandex.market.loyalty.core.service.discount.DiscountAntifraudService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.perks.PerkService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Component
public class MultiStageTestUtils {
    @Autowired
    private PerkService perkService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private CashbackService cashbackService;
    @Autowired
    private DiscountAntifraudService discountAntifraudService;
    @Autowired
    @Blackbox
    private RestTemplate blackboxRestTemplate;
    @Autowired
    @BlackboxYandexTeam
    private RestTemplate blackboxYandexTeamTemplate;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private TriggerEventService triggerEventService;

    public MultiCartWithBundlesDiscountResponse spendRequest(MultiCartWithBundlesDiscountRequest discountRequest) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(discountRequest, applicabilityPolicy, "");
    }

    public void setUpCashback() {
        List.of(blackboxRestTemplate, blackboxYandexTeamTemplate)
                .forEach(brt -> BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, brt));
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
    }

    public static CheckouterUtils.OrderBuilder buildOrder(
            OrderStatus orderStatus, Long orderId, String multiOrder, ItemKey itemKey,
            BigDecimal itemPrice, Integer ordersCount
    ) {
        return buildOrder(orderStatus, orderId, multiOrder, itemKey, itemPrice, BigDecimal.ONE, ordersCount);
    }

    public static CheckouterUtils.OrderBuilder buildOrder(
            OrderStatus orderStatus, Long orderId, String multiOrder, ItemKey itemKey,
            BigDecimal itemPrice, BigDecimal itemCount, Integer ordersCount
    ) {
        return buildOrder(orderStatus, orderId, multiOrder, itemKey, itemPrice, itemCount, ordersCount,
                DeliveryType.DELIVERY, DEFAULT_UID);
    }

    public static CheckouterUtils.OrderBuilder buildOrder(
            OrderStatus orderStatus, Long orderId, String multiOrder, ItemKey itemKey,
            BigDecimal itemPrice, BigDecimal itemCount, Integer ordersCount, long uid
    ) {
        return buildOrder(orderStatus, orderId, multiOrder, itemKey, itemPrice, itemCount, ordersCount,
                DeliveryType.DELIVERY, uid);
    }

    public static CheckouterUtils.OrderBuilder buildOrder(
            OrderStatus orderStatus, Long orderId, String multiOrder, ItemKey itemKey,
            BigDecimal itemPrice, BigDecimal itemCount, Integer ordersCount, DeliveryType deliveryType,
            long uid
    ) {
        return buildOrder(orderStatus, orderId, multiOrder, itemKey, itemPrice, itemCount,
                ordersCount, deliveryType, CashbackOption.EMIT, uid);
    }

    public static CheckouterUtils.OrderBuilder buildOrder(
            OrderStatus orderStatus, Long orderId, String multiOrder, ItemKey itemKey,
            BigDecimal itemPrice, BigDecimal itemCount, Integer ordersCount, DeliveryType deliveryType,
            CashbackOption cashbackOption, long uid
    ) {
        CheckouterUtils.OrderBuilder order = CheckouterUtils.defaultOrder(orderStatus)
                .setOrdersCount(ordersCount)
                .setOrderId(orderId)
                .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                .setPaymentType(ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID)
                .setDeliveryType(deliveryType)
                .setNoAuth(false)
                .setUid(uid)
                .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "MasterCard")
                .setProperty(OrderPropertyType.SELECTED_CASHBACK_OPTION, cashbackOption)
                .addItem(defaultOrderItem()
                        .setWareId(String.valueOf(MARKET_WAREHOUSE_ID))
                        .setCount(itemCount)
                        .setPrice(itemPrice)
                        .setItemKey(itemKey)
                        .setId(itemKey.getFeedId())
                        .build());
        if (multiOrder != null) {
            order = order
                    .setMultiOrderId(multiOrder);
        }
        return order;
    }

    public void checkCalculations(int size, ResolvingState state, Boolean everyItem) {
        var calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(size));
        if (size > 0) {
            assertThat(
                    calculations,
                    everyItem
                            ? everyItem(hasProperty("result", equalTo(state)))
                            : hasItem(hasProperty("result", equalTo(state)))
            );
        }
    }

    public void checkYandexWalletTransactions(
            int size, YandexWalletTransactionStatus status, Long amount, Boolean everyItem
    ) {
        var walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(size));
        if (size > 0) {
            assertThat(
                    walletTransactions,
                    everyItem
                            ? everyItem(allOf(
                            hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(amount))),
                            hasProperty("status", equalTo(status))
                    ))
                            : hasItem(allOf(
                            hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(amount))),
                            hasProperty("status", equalTo(status))
                    ))
            );
        }
    }

    public void processTriggerEventsUntilAllFinished() {
        do {
            triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        } while (!triggerEventService.getNotProcessed(Duration.ZERO, null).isEmpty());
    }

    public static PagedReturns emptyReturns() {
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setPager(new Pager(0, 0, 0, 10, 0, 1));
        pagedReturns.setItems(List.of());
        return pagedReturns;
    }

    public static PagedReturns preparePagedReturns(Order order, Integer returnCount) {
        return preparePagedReturns(returnCount, order.getItems());
    }

    public static PagedReturns preparePagedReturns(Integer returnCount, Collection<OrderItem> orderItems) {
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setPager(new Pager(1, 1, 1, 10, 1, 1));
        List<ReturnItem> returnedItems = orderItems.stream()
                .map(orderItem -> new ReturnItem(orderItem.getId(),
                        returnCount != null ? returnCount : orderItem.getCount(),
                        false, BigDecimal.ONE))
                .collect(Collectors.toList());
        Return returnElem = new Return();
        returnElem.setItems(returnedItems);
        pagedReturns.setItems(List.of(returnElem));
        return pagedReturns;
    }

    public static OrderItems prepareOrderItems(Order orderDelivered) {
        return new OrderItems(orderDelivered.getItems());
    }
}
