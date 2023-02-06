package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.OrderSortingType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DEFAULT_ORDERS_SORTING_BY_IMPORTANCE;

public class GetOrderByUidByImportanceWithPartialsSortingTest extends AbstractGetOrderByUidSortingTest {

    @DisplayName("Should sort by importance with partials on BY_IMPORTANCE param + exp + feature")
    @RepeatedTest(value = 5, name = "{displayName} - repetition {currentRepetition} of {totalRepetitions}")
    public void shouldSortByImportanceWithPartials() throws Exception {
        createShuffledOrders();

        checkouterFeatureWriter.writeValue(ENABLE_DEFAULT_ORDERS_SORTING_BY_IMPORTANCE, true);
        Experiments exps = Experiments.empty();
        exps.addExperiment(
                Experiments.USER_ORDERS_SORTING_BY_IMPORTANCE_EXP,
                Experiments.USER_ORDERS_SORTING_BY_IMPORTANCE_EXP_VALUE);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/orders/by-uid/{userId}", BuyerProvider.UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.PAGE_SIZE, "20")
                .param(CheckouterClientParams.DISABLE_DEFAULT_DATE_RANGE, "true")
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.CHANGE_REQUEST.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.CASHBACK_EMIT_INFO.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.ITEM_SERVICES.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name())
                .param(CheckouterClientParams.SORT, OrderSortingType.BY_IMPORTANCE.name())
                .header(X_EXPERIMENTS, exps.toExperimentString());

        checkByImportanceOrderSorting(requestBuilder);
    }

    @DisplayName("Should sort recent by importance with partials on BY_IMPORTANCE param + feature")
    @RepeatedTest(value = 5, name = "{displayName} - repetition {currentRepetition} of {totalRepetitions}")
    public void shouldSortRecentByDateWithPartials() throws Exception {
        createShuffledOrders();

        checkouterFeatureWriter.writeValue(ENABLE_DEFAULT_ORDERS_SORTING_BY_IMPORTANCE, true);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/orders/by-uid/{userId}/recent",
                BuyerProvider.UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.PAGE_SIZE, "20")
                .param(CheckouterClientParams.STATUS, OrderStatus.UNPAID.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PROCESSING.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERY.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PENDING.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.PICKUP.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERED.name())
                .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.ITEMS.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY_PARCELS.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.ITEM_SERVICES.name())
                .param(CheckouterClientParams.SORT, OrderSortingType.BY_IMPORTANCE.name());

        checkByImportanceOrderSortingRecent(requestBuilder);
    }
}
