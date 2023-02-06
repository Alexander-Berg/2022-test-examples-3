package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.TrustServiceFee;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.ff.OrderFulfilmentService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateProductParams;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.FREE_DELIVERY_THRESHOLD;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.YANDEX_UID;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;


public class ShopDeliverySubsidyTest extends AbstractWebTestBase {

    private final Instant payTime = ZonedDateTime.of(2027, 5, 3, 19, 32, 23, 0, ZoneId.systemDefault())
            .toInstant();
    @Autowired
    private OrderFulfilmentService orderFulfilmentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReceiptService receiptService;
    @Value("${market.checkouter.payments.clear.hours:167}")
    private int clearHours;

    @Test
    public void shouldPaySubsidiesForShopDeliveryCorrectly() throws Exception {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setMockLoyalty(true);
        whiteParameters.setFreeDelivery(true);
        whiteParameters.setCheckCartErrors(false);
        whiteParameters.getLoyaltyParameters()
                .addDeliveryDiscount(LoyaltyDiscount.discountFor(10, FREE_DELIVERY_THRESHOLD));
        MultiCart cart = orderCreateHelper.cart(whiteParameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(cart, whiteParameters);
        Order order = multiOrder.getOrders().iterator().next();
        assertThat(order.getPromos(), not(empty()));

        setFixedTime(payTime);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());

        setFixedTime(payTime.plus(clearHours, ChronoUnit.HOURS).plusSeconds(1));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        Order orderFromDB = orderService.getOrder(order.getId());
        assertThat(orderFromDB.getPayment().getStatus(), is(PaymentStatus.CLEARED));

        //смотрим что субсидия выплатится по статусу
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        validateOrderSubsidies(order);
    }

    private void validateOrderSubsidies(Order order) {
        //смортрим что у ордера еще нет субсидий
        List<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY);
        assertThat(payments, hasSize(0));

        //запускаем таску, смотрим что выполнится
        trustMockConfigurer.resetRequests();
        Collection<QueuedCall> queuedCalls =
                queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        assertEquals(1, queuedCalls.size());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        List<Payment> subsidies = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY);
        Order orderReloaded = orderService.getOrder(order.getId());
        List<Receipt> receipts = receiptService.findByOrder(order.getId(), ReceiptStatus.GENERATED);

        assertThat(subsidies, hasSize(1));
        Payment subsidy = subsidies.get(0);
        assertThat(subsidy.getTotalAmount(), equalTo(order.getBuyerSubsidyTotal()));
        assertThat(subsidy.getStatus(), equalTo(PaymentStatus.IN_PROGRESS));
        validateTrustCalls(subsidy, orderReloaded);

        assertThat(receipts, hasSize(1));
        List<ReceiptItem> receiptItems = receipts.get(0).getItems();
        assertThat(receiptItems, hasSize(1));
        ReceiptItem receiptItem = receiptItems.get(0);
        assertThat(receiptItem.getPrice(), equalTo(order.getDelivery().getPrices().getSubsidy()));
        assertThat(receiptItem.getAmount(), equalTo(order.getDelivery().getPrices().getSubsidy()));
    }

    private void validateTrustCalls(Payment subsidy, Order order) {
        OneElementBackIterator<ServeEvent> callIterator = trustMockConfigurer.eventsIterator();
        ShopMetaData shop = orderFulfilmentService.getShopMetaData(order);

        checkLoadPartnerCall(callIterator, shop.getClientId());
        String serviceProductId = avoidNull(shop.getYaMoneyId(), shop.getCampaignId() + "_" + shop.getClientId() + "_"
                + colorConfig.getFor(order).getServiceFeeId(TrustServiceFee.DELIVERY_SUBSIDY));
        checkOptionalCreateServiceProductCall(callIterator, CreateProductParams.product(
                shop.getClientId(),
                serviceProductId,
                serviceProductId,
                colorConfig.getFor(order).getServiceFeeId(TrustServiceFee.DELIVERY_SUBSIDY)
        ));


        checkBatchServiceOrderCreationCall(callIterator, subsidy.getUid());
        CreateBasketParams basketParams = createBasket()
                .withPayMethodId("subsidy")
                .withBackUrl(endsWith("/payments/" + subsidy.getId() + "/notify-basket"))
                .withUid(subsidy.getUid())
                .withYandexUid(YANDEX_UID)
                .withCurrency(Currency.RUR)
                .withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
        basketParams.withOrder(
                order.getDelivery().getSubsidyBalanceOrderId(),
                BigDecimal.ONE,
                order.getDelivery().getPrices().getBuyerSubsidy()
        );
        checkCreateBasketCall(callIterator, basketParams);
        checkPayBasketCall(callIterator, subsidy.getUid(), subsidy.getBasketKey());
        assertFalse(callIterator.hasNext());
    }
}
