package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PaymentGoalUtils;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;

import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.DEFAULT_MARKET_INN;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.DEFAULT_SUPPLIER_INN;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.DELIVERY_SERVICE_ORDER_ID;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.MARKET_PARTNER_ID;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.BalanceOrderToCreate;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;

/**
 * @author : poluektov
 * date: 2019-05-21.
 */
public final class TrustCallsParamsProvider {

    private TrustCallsParamsProvider() {
    }

    public static BalanceOrderToCreate balanceOrderForItem(OrderItem orderItem) {
        return new BalanceOrderToCreate(
                orderItem.getBalanceOrderId(),
                orderItem.getQuantityIfExistsOrCount(),
                orderItem.getQuantPriceIfExistsOrBuyerPrice(),
                orderItem.getOfferName(),
                orderItem.getVat().getTrustId(),
                DEFAULT_SUPPLIER_INN,
                null
        );
    }

    public static BalanceOrderToCreate balanceOrderForBlueDelivery(Delivery orderDelivery) {
        return new BalanceOrderToCreate(
                orderDelivery.getBalanceOrderId(),
                BigDecimal.ONE,
                orderDelivery.getBuyerPriceWithLift(),
                "Доставка",
                orderDelivery.getVat().getTrustId(),
                DEFAULT_MARKET_INN,
                null
        );
    }

    public static BalanceOrderToCreate balanceOrderForShopDelivery(Delivery orderDelivery) {
        return new BalanceOrderToCreate(
                orderDelivery.getBalanceOrderId(),
                BigDecimal.ONE,
                orderDelivery.getBuyerPriceWithLift(), // в чеке стоимость доставки = доставка + подъем
                "Доставка",
                orderDelivery.getVat().getTrustId(),
                DEFAULT_SUPPLIER_INN,
                null
        );
    }

    @Deprecated
    public static CreateBasketParams createBasketParamsForShopDeliveryOrder(Order order, long paymentId) {
        CreateBasketParams createBasket = createBasket()
                .withBackUrl(Matchers.equalTo("/payments/" + paymentId + "/notify-basket"))
                .withUid(order.isNoAuth() ? null : order.getUid())
                .withCurrency(Currency.RUR)
                .withPaymentTimeout(Matchers.equalTo("0"))
                .withUserEmail(order.getBuyer().getEmail())
                .withYandexUid(order.getBuyer().getYandexUid())
                .withUserIp("127.0.0.1");
        order.getItems().forEach(orderItem -> createBasket.withOrder(balanceOrderForItem(orderItem)));
        createBasket.withOrder(balanceOrderForShopDelivery(order.getDelivery()));
        //FIXME Почему это здесь а не отдельно - очень не очевидная логика и абсолютно неприменимая к белым заказам ?
        if (PaymentGoalUtils.getPaymentGoal(order) == PaymentGoal.ORDER_PREPAY) {
            if (order.getRgb() == Color.BLUE) {
                createBasket.withSpasiboOrderMap(order.getItems().stream()
                        .collect(Collectors.toMap(OrderItem::getBalanceOrderId, OrderItem::getShopSku)));
            }
        }
        return createBasket;
    }

    public static CreateBasketParams createBasketFulfilmentParams(Order order, long paymentId) {
        CreateBasketParams createBasket = createBasket()
                .withBackUrl(Matchers.equalTo("/payments/" + paymentId + "/notify-basket"))
                .withUid(order.isNoAuth() ? null : order.getUid())
                .withCurrency(Currency.RUR)
                .withPaymentTimeout(Matchers.equalTo("0"))
                .withUserEmail(order.getBuyer().getEmail())
                .withYandexUid(order.getBuyer().getYandexUid())
                .withUserIp("127.0.0.1");
        order.getItems().forEach(orderItem -> createBasket.withOrder(balanceOrderForItem(orderItem)));
        createBasket.withOrder(balanceOrderForBlueDelivery(order.getDelivery()));
        return createBasket;
    }

    public static CreateProductParams productFrom3PItem(OrderItem orderItem) {
        Long shopId = orderItem.getSupplierId();
        String serviceProductId = shopId + "_" + shopId;
        String name = shopId + "-" + shopId;
        return product(shopId, name, serviceProductId);
    }

    public static CreateProductParams productFromBlueDelivery() {
        return product(MARKET_PARTNER_ID, DELIVERY_SERVICE_ORDER_ID, DELIVERY_SERVICE_ORDER_ID, 1);
    }
}
