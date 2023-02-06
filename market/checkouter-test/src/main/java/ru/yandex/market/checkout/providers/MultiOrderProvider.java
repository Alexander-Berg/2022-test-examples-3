package ru.yandex.market.checkout.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static ru.yandex.common.util.currency.Currency.RUR;

/**
 * @author jkt on 01/03/2022.
 */
public class MultiOrderProvider {

    public static final long DEFAULT_REGION = 213L;
    private static final AtomicLong LABEL_COUNTER = new AtomicLong();

    private MultiOrderProvider() {
    }

    public static MultiOrder buildMultiOrder(List<Order> orders) {
        return createBuilder()
                .orders(orders)
                .build();
    }

    public static MultiOrderProvider.MultiOrderBuilder createBuilder() {
        return new MultiOrderProvider.MultiOrderBuilder();
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

    public static MultiOrder empty() {
        return createBuilder().build();
    }

    public static class MultiOrderBuilder {

        private Currency currency;
        private long regionId;
        private Buyer buyer;
        private List<Order> orders = new ArrayList<>();
        private List<OrderProvider.OrderBuilder> orderBuilders = new ArrayList<>();

        private MultiOrderBuilder() {
            currency(RUR);
            regionId(DEFAULT_REGION);
            buyer(BuyerProvider.getBuyer());
        }

        public MultiOrderProvider.MultiOrderBuilder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder regionId(long regionId) {
            this.regionId = regionId;
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder order(Order order) {
            orders.add(order);
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder orders(List<Order> orders) {
            this.orders = orders;
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder orderBuilder(OrderProvider.OrderBuilder orderBuilder) {
            orderBuilders.add(orderBuilder);
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder buyer(Buyer buyer) {
            this.buyer = buyer;
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder muid(Long muid) {
            buyer.setMuid(muid);
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder uuid(String uuid) {
            buyer.setUuid(uuid);
            return this;
        }

        public MultiOrderProvider.MultiOrderBuilder uid(Long uid) {
            buyer.setUid(uid);
            return this;
        }

        public MultiOrder build() {
            if (CollectionUtils.isEmpty(orders)) {
                orders = orderBuilders.stream()
                        .map(OrderProvider.OrderBuilder::build)
                        .collect(Collectors.toList());
            }
            buyer.setRegionId(regionId);
            MultiOrder multiOrder = new MultiOrder();
            multiOrder.setBuyer(buyer);
            var method = orders.stream()
                    .findFirst()
                    .map(Order::getPaymentMethod)
                    .orElse(PaymentMethod.CASH_ON_DELIVERY);
            multiOrder.setPaymentMethod(method);
            multiOrder.setPaymentType(method.getPaymentType());
            multiOrder.setBuyerRegionId(regionId);
            multiOrder.setBuyerCurrency(currency);
            multiOrder.setCarts(orders);
            autoLabelCarts(orders);
            return multiOrder;
        }
    }
}
