package ru.yandex.market.checkout.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider.OrderBuilder;

import static ru.yandex.common.util.currency.Currency.RUR;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 07/07/2017
 */
public final class MultiCartProvider {

    public static final long DEFAULT_REGION = 213L;
    private static final AtomicLong LABEL_COUNTER = new AtomicLong();

    private MultiCartProvider() {
    }

    public static MultiCart buildMultiCart(List<Order> orders) {
        return createBuilder()
                .orders(orders)
                .build();
    }

    @Deprecated
    public static MultiCart buildMultiCart(Order order, long buyerRegionId, Currency buyerCurrency) {
        return createBuilder()
                .currency(buyerCurrency)
                .regionId(buyerRegionId)
                .order(order)
                .build();
    }

    @Deprecated
    public static MultiCart buildMultiCart(List<Order> orders, long buyerRegionId, Currency buyerCurrency) {
        return createBuilder()
                .currency(buyerCurrency)
                .regionId(buyerRegionId)
                .orders(orders)
                .build();
    }

    public static MultiCartBuilder createBuilder() {
        return new MultiCartBuilder();
    }

    public static MultiCart single(OrderBuilder builder) {
        return createBuilder()
                .orderBuilder(builder)
                .build();
    }

    public static MultiCart single(Order order) {
        return createBuilder()
                .order(order)
                .build();
    }

    public static void autoLabelCarts(List<Order> orders) {
        if (orders.size() > 1) {
            orders.forEach(o -> {
                if (o.getLabel() == null) {
                    o.setLabel("auto-label-" + LABEL_COUNTER.getAndIncrement());
                }
            });
        }
    }

    public static MultiCart empty() {
        return createBuilder().build();
    }

    public static class MultiCartBuilder {

        private Currency currency;
        private long regionId;
        private Buyer buyer;
        private List<Order> orders = new ArrayList<>();
        private final List<OrderBuilder> orderBuilders = new ArrayList<>();

        private MultiCartBuilder() {
            currency(RUR);
            regionId(DEFAULT_REGION);
            buyer(BuyerProvider.getBuyer());
        }

        public MultiCartBuilder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public MultiCartBuilder regionId(long regionId) {
            this.regionId = regionId;
            return this;
        }

        public MultiCartBuilder order(Order order) {
            orders.add(order);
            return this;
        }

        public MultiCartBuilder orders(List<Order> orders) {
            this.orders = orders;
            return this;
        }

        public MultiCartBuilder orderBuilder(OrderBuilder orderBuilder) {
            orderBuilders.add(orderBuilder);
            return this;
        }

        public MultiCartBuilder buyer(Buyer buyer) {
            this.buyer = buyer;
            return this;
        }

        public MultiCartBuilder yuid(String yuid) {
            buyer.setYandexUid(yuid);
            return this;
        }

        public MultiCartBuilder muid(Long muid) {
            buyer.setMuid(muid);
            return this;
        }

        public MultiCartBuilder uuid(String uuid) {
            buyer.setUuid(uuid);
            return this;
        }

        public MultiCartBuilder uid(Long uid) {
            buyer.setUid(uid);
            return this;
        }

        public MultiCart build() {
            if (CollectionUtils.isEmpty(orders)) {
                orders = orderBuilders.stream()
                        .map(OrderBuilder::build)
                        .collect(Collectors.toList());
            }
            buyer.setRegionId(regionId);
            MultiCart cart = new MultiCart();
            cart.setBuyer(buyer);
            var method = orders.stream()
                    .findFirst()
                    .map(Order::getPaymentMethod)
                    .orElse(PaymentMethod.CASH_ON_DELIVERY);
            cart.setPaymentMethod(method);
            cart.setPaymentType(method.getPaymentType());
            cart.setBuyerRegionId(regionId);
            cart.setBuyerCurrency(currency);
            cart.setCarts(orders);
            autoLabelCarts(orders);
            return cart;
        }
    }
}
