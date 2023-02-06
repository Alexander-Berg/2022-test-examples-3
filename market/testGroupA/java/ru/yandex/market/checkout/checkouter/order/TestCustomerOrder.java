package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.FeedOfferId;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
@Deprecated
public class TestCustomerOrder {

    public static final int DEFAULT_SHOP_ID = 21;
    public static final long DEFAULT_CUSTOMER_ID = 1L;
    public static final BigDecimal DEFAULT_EXCHANGE_RATE = BigDecimal.ONE;
    public static final PaymentMethod DEFAULT_ORDER_PAYMENT_METHOD = PaymentMethod.BANK_CARD;
    public static final boolean DEFAULT_FAKE = false;
    public static final FeedOfferId FEED_OFFER_ID = new FeedOfferId("123", 1L);
    private static final Recipient RECIPIENT = new Recipient(
            new RecipientPerson("a", null, "b"),
            "cf637afe00d8be951c245162a93b7d84",
            "+79999999999",
            "0123456789abcdef0123456789abcdef",
            "leo@ya.ru",
            "fedcba9876543210fedcba9876543210");

    private final long shopId;
    private final List<OrderItem> items = new ArrayList<>();
    private final Delivery delivery = new Delivery();
    private final Buyer buyer = new Buyer();
    private final PaymentMethod orderPaymentMethod;
    private final Set<PaymentMethod> orderPaymentOptions;
    private final BigDecimal exchangeRate;
    private List<Delivery> deliveryOptions;

    private final boolean fake;
    private final Context context;

    public TestCustomerOrder() {
        shopId = DEFAULT_SHOP_ID;
        exchangeRate = DEFAULT_EXCHANGE_RATE;

        buyer.setUid(DEFAULT_CUSTOMER_ID);
        delivery.setBuyerAddress(new AddressImpl());
        delivery.setShopAddress(new AddressImpl());
        delivery.setRecipient(RECIPIENT);
        delivery.setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.BANK_CARD)));
        orderPaymentOptions = new HashSet<>(Arrays.asList(PaymentMethod.BANK_CARD));
        orderPaymentMethod = DEFAULT_ORDER_PAYMENT_METHOD;
        fake = DEFAULT_FAKE;
        context = Context.MARKET;
    }

    public TestCustomerOrder withDeliveryOption(List<Delivery> deliveryOptions) {
        this.deliveryOptions = deliveryOptions;
        return this;
    }

    public Order build() {
        Order order = new Order();
        order.setRgb(Color.BLUE);
        order.setFake(fake);
        order.setContext(context);
        order.setShopId(shopId);
        order.setExchangeRate(exchangeRate);
        order.setPaymentOptions(orderPaymentOptions);
        order.setPaymentMethod(orderPaymentMethod);
        order.setDeliveryOptions(deliveryOptions);
        order.setBuyer(buyer);
        items.add(new OrderItem(FEED_OFFER_ID, BigDecimal.ONE, 1));
        order.setItems(items);
        order.setDelivery(delivery);
        order.setNoAuth(false);
        return order;
    }

}
