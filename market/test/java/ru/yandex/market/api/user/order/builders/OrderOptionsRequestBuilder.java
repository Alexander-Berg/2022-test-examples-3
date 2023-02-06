package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsRequestBuilder extends RandomBuilder<OrderOptionsRequest> {

    OrderOptionsRequest request = new OrderOptionsRequest();

    @Override
    public OrderOptionsRequestBuilder random() {
        request.setRegionId(random.getInt(1, 1000));
        request.setCurrency(random.from(Currency.class));
        return this;
    }

    public OrderOptionsRequestBuilder withRegionId(int regionId) {
        request.setRegionId(regionId);
        return this;
    }

    public OrderOptionsRequestBuilder withPaymentOptions(PaymentMethod ... options) {
        request.setPaymentOptions(Sets.newHashSet(options));
        return this;
    }

    public OrderOptionsRequestBuilder withCurrency(Currency currency) {
        request.setCurrency(currency);
        return this;
    }

    public OrderOptionsRequestBuilder withShopOrder(OrderOptionsRequest.ShopOrder shopOrder) {
        request.setShopOrders(Lists.newArrayList(shopOrder));
        return this;
    }

    public OrderOptionsRequestBuilder withShopOrders(OrderOptionsRequest.ShopOrder ... orders) {
        request.setShopOrders(Lists.newArrayList(orders));
        return this;
    }

    public OrderOptionsRequestBuilder withPaymentOptionsHiddenReasons(PaymentOptionHiddenReason ... knownReasons) {
        request.setPaymentOptionHiddenReasons(Sets.newHashSet(knownReasons));
        return this;
    }

    public OrderOptionsRequestBuilder withPresets(OrderOptionsRequest.Preset ... presets) {
        request.setPresets(Lists.newArrayList(presets));
        return this;
    }

    public OrderOptionsRequestBuilder withOptionalRules(Boolean isEnabled) {
        request.setOptionalRulesEnabled(isEnabled);
        return this;
    }

    public OrderOptionsRequestBuilder withSeparateCalculationForOrders(Boolean isEnabled) {
        request.setCalculateOrdersSeparately(isEnabled);
        return this;
    }

    @Override
    public OrderOptionsRequest build() {
        return request;
    }
}
