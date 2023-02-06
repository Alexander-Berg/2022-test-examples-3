package ru.yandex.market.fulfillment.wrap.marschroute.model.base;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.exception.FulfillmentWrapException;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteItems.item;

class MarschrouteItemCostCalculationTest extends BaseIntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        MarschrouteItem item = item(100, 10);

        softly.assertThat(item.calculateCost())
                .as("Asserting total cost")
                .isEqualTo(1000);
    }

    @Test
    void testQuantityIsNull() throws Exception {
        MarschrouteItem item = item(100, null);

        softly.assertThatThrownBy(item::calculateCost).isInstanceOf(FulfillmentWrapException.class);
    }


    @Test
    void testPriceIsNull() throws Exception {
        MarschrouteItem item = item(null, 10);
        item.setPrice(null);

        softly.assertThatThrownBy(item::calculateCost).isInstanceOf(FulfillmentWrapException.class);
    }
}
