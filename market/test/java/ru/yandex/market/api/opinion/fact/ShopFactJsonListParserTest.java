package ru.yandex.market.api.opinion.fact;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.opinion.fact.ShopFact;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.FactMatcher;
import ru.yandex.market.api.opinion.Delivery;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

public class ShopFactJsonListParserTest extends UnitTestBase {
    private ShopFactJsonListParser parser;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new ShopFactJsonListParser();
    }

    @Test
    public void parseShopFactors() {
        List<ShopFact> factors = parse("shop-fact-list.json");
        Assert.assertThat(
            factors,
            Matchers.containsInAnyOrder(
                FactMatcher.shopFacts(
                    FactMatcher.id(0),
                    FactMatcher.title("Скорость обработки заказа"),
                    FactMatcher.description("Как быстро с вами связались для подтверждения заказа?"),
                    FactMatcher.deliveryType(Delivery.DELIVERY),
                    FactMatcher.value(null)
                )
                ,
                FactMatcher.shopFacts(
                    FactMatcher.id(0),
                    FactMatcher.title("Скорость обработки заказа"),
                    FactMatcher.description("Как быстро с вами связались для подтверждения заказа?"),
                    FactMatcher.deliveryType(Delivery.PICKUP),
                    FactMatcher.value(null)
                ),
                FactMatcher.shopFacts(
                    FactMatcher.id(5),
                    FactMatcher.title("Ассортимент"),
                    FactMatcher.description("Остались ли вы довольны ассортиментом?"),
                    FactMatcher.deliveryType(Delivery.INSTORE),
                    FactMatcher.value(null)
                )
            )
        );
    }

    private List<ShopFact> parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
