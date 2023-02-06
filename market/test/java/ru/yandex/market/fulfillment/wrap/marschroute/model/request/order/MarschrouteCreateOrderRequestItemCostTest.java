package ru.yandex.market.fulfillment.wrap.marschroute.model.request.order;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.core.exception.FulfillmentWrapException;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import java.util.List;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteCreateOrderRequests.createOrderRequest;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteItems.item;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteItems.items;

class MarschrouteCreateOrderRequestItemCostTest extends BaseIntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        List<MarschrouteItem> items = items(
                item(100, 1),
                item(100, 10),
                item(100, 1)
        );

        MarschrouteCreateOrderRequest request = createOrderRequest(items);

        softly.assertThat(request.calculateItemsCost())
                .as("Asserting total request items cost is equal to sum of each individual item cost")
                .isEqualTo(1200);
    }

    @Test
    void testEmptyItemsScenario() throws Exception {
        MarschrouteCreateOrderRequest request = createOrderRequest();

        softly.assertThat(request.calculateItemsCost())
                .as("Asserting that total request items cost equal to 0 when there are no items")
                .isEqualTo(0);
    }

    @Test
    void testNegativeScenarioWhenOneOfTheItemsIsNull() throws Exception {
        MarschrouteCreateOrderRequest request = createOrderRequest(items(
                item(100, 1),
                null
        ));

        softly.assertThatThrownBy(() -> request.calculateItemsCost()).isInstanceOf(FulfillmentWrapException.class);
    }
}
