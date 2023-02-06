package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSortingType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.test.providers.BuyerProvider;


public class GetOrderByUidByDateWithParamSortingTest extends AbstractGetOrderByUidSortingTest {

    @DisplayName("Should sort by creation date on BY_DATE param without partials")
    @RepeatedTest(value = 5, name = "{displayName} - repetition {currentRepetition} of {totalRepetitions}")
    public void shouldSortByDateWithParamWithoutPartials() throws Exception {
        createShuffledOrders();


        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/orders/by-uid/{userId}", BuyerProvider.UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.PAGE_SIZE, "20")
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, "true")
                .param(CheckouterClientParams.SORT, OrderSortingType.BY_DATE.name());

        checkByDateOrderSorting(requestBuilder);
    }

    @DisplayName("Should sort recent by creation date on BY_DATE param without partials")
    @RepeatedTest(value = 5, name = "{displayName} - repetition {currentRepetition} of {totalRepetitions}")
    public void shouldSortRecentByDateWithParamWithoutPartials() throws Exception {
        createShuffledOrders();


        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/orders/by-uid/{userId}/recent", BuyerProvider.UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.PAGE_SIZE, "20")
                .param(CheckouterClientParams.STATUS, OrderStatus.UNPAID.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PROCESSING.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PICKUP.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERY.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PENDING.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERED.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                .param(CheckouterClientParams.SORT, OrderSortingType.BY_DATE.name());

        checkByDateOrderSortingRecent(requestBuilder);
    }

}
