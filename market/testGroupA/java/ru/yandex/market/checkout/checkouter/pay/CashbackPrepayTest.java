package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.loyalty.AbstractLoyaltyBundleResponseTransformer;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author : poluektov
 * date: 2020-09-21.
 */
public class CashbackPrepayTest extends AbstractWebTestBase {

    /**
     * Это число зависит от дефолтного ответа лоялти, который конфигурится в
     * {@link AbstractLoyaltyBundleResponseTransformer}
     */
    private static final BigDecimal ITEM_CASHBACK = new BigDecimal("30.00");
    private static final BigDecimal ITEM_RUB = new BigDecimal("120.01");
    private static final BigDecimal DELIVERY_RUB = new BigDecimal("100");
    private static final BigDecimal TOTAL_RUB = ITEM_RUB.add(DELIVERY_RUB);

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderService orderService;

    private Order order1;

    @BeforeEach
    public void createOrder() throws Exception {
        Parameters params = CashbackTestProvider.defaultCashbackParameters();
        params.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiCart cart = orderCreateHelper.cart(params);
        MultiOrder checkout = orderCreateHelper.checkout(cart, params);
        //Заказ с кэшбэком
        order1 = orderService.getOrder(checkout.getOrders().get(0).getId());
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
    }

    @Test
    public void testPaySingleOrderWithCashback() {
        Long uid = order1.getUid();

        //Check Payment
        Payment payment = orderPayHelper.pay(order1.getId());
        assertEquals(2, payment.getPartitions().size());
        assertEquals(ITEM_CASHBACK, payment.amountByAgent(PaymentAgent.YANDEX_CASHBACK).setScale(2));
        assertEquals(TOTAL_RUB, payment.amountByAgent(PaymentAgent.DEFAULT));

        //Check Receipt
        List<Receipt> receipts =
                receiptService.findAllPaymentReceipts(payment.getId());
        assertEquals(1, receipts.size());
        Receipt receipt = receipts.iterator().next();
        ReceiptItem itemReceiptLine =
                receipt.getItems().stream().filter(ReceiptItem::isOrderItem).findAny().get();
        assertEquals(ITEM_CASHBACK, itemReceiptLine.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(ITEM_RUB, itemReceiptLine.amountByAgent(PaymentAgent.DEFAULT));

        //CheckTrustCalls
        OneElementBackIterator<ServeEvent> iterator = trustMockConfigurer.eventsIterator();
        checkCommonTrustCallsForPaymentCreation(iterator, uid);
        TrustCallsChecker.checkListWalletBalanceMethodCall(trustMockConfigurer.eventsGatewayIterator());
        checkCreateBasketMarkup(iterator);
        TrustCallsChecker.checkPayBasketCall(iterator, uid, payment.getBasketKey());
    }

    @Test
    public void testPayMultiOrderWithCashback() {
        //Создаем второй закза без кэшбека, и оплачиваем его вместе с 1ым.
        //Итоговое разбиение должно остаться таким же.
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        //Check Payment
        Payment payment = orderPayHelper.payForOrders(List.of(order1, order2));
        assertEquals(2, payment.getPartitions().size());
        assertEquals(ITEM_CASHBACK, payment.amountByAgent(PaymentAgent.YANDEX_CASHBACK).setScale(2));
        assertEquals(TOTAL_RUB.add(order2.getBuyerTotal()), payment.amountByAgent(PaymentAgent.DEFAULT));

        //Check Receipt
        List<Receipt> receipts =
                receiptService.findAllPaymentReceipts(payment.getId());
        assertEquals(1, receipts.size());
        Receipt receipt = receipts.iterator().next();
        ReceiptItem itemReceiptLine =
                receipt.getItems().stream()
                        .filter(i -> i.isOrderItem() && i.getOrderId().longValue() == order1.getId().longValue())
                        .findAny().get();
        assertEquals(ITEM_CASHBACK, itemReceiptLine.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(ITEM_RUB, itemReceiptLine.amountByAgent(PaymentAgent.DEFAULT));
    }

    @Test
    public void testGerOrdersPartitions() {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);
        Payment payment = orderPayHelper.payForOrder(order1);
        Order getOrderResult = orderService.getOrder(
                order1.getId(),
                ClientInfo.SYSTEM,
                null,
                null
        );
        Payment paymentFromGetOrders = getOrderResult.getPayment();
        assertEquals(payment.amountByAgent(PaymentAgent.DEFAULT).setScale(2),
                paymentFromGetOrders.amountByAgent(PaymentAgent.DEFAULT));
        assertEquals(payment.amountByAgent(PaymentAgent.YANDEX_CASHBACK).setScale(2),
                paymentFromGetOrders.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
    }

    private void checkCommonTrustCallsForPaymentCreation(OneElementBackIterator<ServeEvent> iterator, Long uid) {
        ShopMetaData shop = shopService.getMeta(order1.getShopId());
        TrustCallsChecker.checkOptionalLoadPartnerCall(iterator, shop.getClientId());
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator, null);
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator, null);
        TrustCallsChecker.checkBatchServiceOrderCreationCall(iterator, uid);
    }

    private void checkCreateBasketMarkup(OneElementBackIterator<ServeEvent> iterator) {
        OrderItem orderItem = order1.getItems().iterator().next();
        LoggedRequest request = iterator.next().getRequest();
        JsonTest.checkJsonMatcher(request.getBodyAsString(),
                "$.paymethod_markup." + order1.getId() + "-item-" + orderItem.getId(),
                hasEntry("yandex_account", ITEM_CASHBACK.setScale(2).toString()));
        JsonTest.checkJsonMatcher(request.getBodyAsString(),
                "$.paymethod_markup." + order1.getId() + "-item-" + orderItem.getId(),
                hasEntry("card", ITEM_RUB.setScale(2).toString()));
        JsonTest.checkJsonMatcher(request.getBodyAsString(), "$.paymethod_markup." + order1.getId() + "-delivery",
                hasEntry("card", DELIVERY_RUB.setScale(2).toString()));
        assertTrue(request.getBodyAsString().contains("spasibo_order_map"));
        assertFalse(request.getBodyAsString().contains("max_spasibo_amount"));
        assertFalse(request.getBodyAsString().contains("min_spasibo_amount"));
    }
}
