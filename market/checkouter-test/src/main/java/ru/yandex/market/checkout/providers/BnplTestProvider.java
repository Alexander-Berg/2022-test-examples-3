package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;

import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static ru.yandex.market.checkout.checkouter.json.Names.Order.PROMO_CODE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;

/**
 * @author : poluektov
 * date: 2021-07-21.
 */
public final class BnplTestProvider {

    public static final BigDecimal CASHBACK_AMOUNT = BigDecimal.valueOf(1337);

    private BnplTestProvider() {
    }

    public static Parameters defaultBnplParameters() {
        Parameters parameters = prepaidBlueOrderParameters();
        initBnplParameters(parameters);
        return parameters;
    }

    public static Parameters defaultBnplParameters(OrderItem... items) {
        Parameters parameters = defaultBlueOrderParametersWithItems(items);
        initBnplParameters(parameters);
        return parameters;
    }

    public static Parameters defaultBnplOrderParametersWithCashBackSpent() {
        var parameters = defaultBnplParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(
                new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));
        return parameters;
    }

    public static Parameters defaultBnplNoAuthParameters(Long uid) {
        Parameters parameters = defaultBnplParameters();
        parameters.getOrder().setUid(uid);
        parameters.getBuyer().setUid(uid);
        parameters.getBuiltMultiCart().setNoAuth(true);
        return parameters;
    }

    public static Parameters bnplAndCashbackParameters() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.setCheckCartErrors(false);
        parameters.setupPromo(PROMO_CODE);
        parameters.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse());
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        return parameters;
    }

    private static void initBnplParameters(Parameters parameters) {
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.getItems().forEach(item -> {
            item.setId(null);
            item.setCategoryId(90864);
        });
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
    }
}
