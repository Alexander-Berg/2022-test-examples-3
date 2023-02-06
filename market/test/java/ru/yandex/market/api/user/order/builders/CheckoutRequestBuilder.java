package ru.yandex.market.api.user.order.builders;

import java.util.Arrays;

import com.google.common.collect.Lists;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.server.sec.UserDevice;
import ru.yandex.market.api.user.order.Buyer;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CheckoutRequestBuilder extends RandomBuilder<CheckoutRequest> {

    private CheckoutRequest request = new CheckoutRequest();

    @Override
    public CheckoutRequestBuilder random() {
        request.setRegionId(random.getInt(1000));
        request.setCurrency(random.from(Currency.class));

        request.setBuyer(new BuyerBuilder().random().build());

        return this;
    }

    public CheckoutRequestBuilder withOrder(CheckoutRequest.ShopOrder order) {
        request.setShopOrders(Lists.newArrayList(order));
        return this;
    }

    public CheckoutRequestBuilder withOrders(CheckoutRequest.ShopOrder... orders) {
        request.setShopOrders(Arrays.asList(orders));
        return this;
    }

    public CheckoutRequestBuilder withPaymentSystem(String paymentSystem) {
        request.setPaymentSystem(paymentSystem);
        return this;
    }

    public CheckoutRequestBuilder withBuyer(Buyer buyer) {
        request.setBuyer(buyer);
        return this;
    }

    public CheckoutRequestBuilder withUserDevice(UserDevice userDevice) {
        request.setUserDevice(userDevice);
        return this;
    }

    @Override
    public CheckoutRequest build() {
        return request;
    }
}
