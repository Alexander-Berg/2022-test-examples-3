package ru.yandex.market.checkout.checkouter.controller;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.market.checkout.util.balance.ResponseVariable.BANK_CARD;

public class TakeoutTest extends AbstractWebTestBase {

    @Test
    public void getAllUserCards() {
        Parameters orderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(orderParameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Set<String> blueCards = client.getAllCardsByUid(order.getBuyer().getUid(), Color.BLUE);
        assertThat(blueCards, hasSize(1));
        assertThat(blueCards, hasItem(equalTo(BANK_CARD.defaultValue())));
        Set<String> redCards = client.getAllCardsByUid(order.getBuyer().getUid(), Color.RED);
        assertThat(redCards, empty());
    }
}
