package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.domain.v2.loyalty.CoinsCount;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @authror dimkarp93
 */
public class CoinsCountParserTest extends UnitTestBase {
    @Test
    public void fromNumber() {
        CoinsCount result = parse("coins-count-number.json");
        Assert.assertEquals(result.getTotal(), 10);
    }

    @Test
    public void fromText() {
        CoinsCount result = parse("coins-count-text.json");
        Assert.assertEquals(result.getTotal(), 5);
    }
    @Test
    public void empty() {
        CoinsCount result = parse("coins-count-empty.json");
        Assert.assertEquals(result.getTotal(), 0);
    }

    private CoinsCount parse(String filename) {
        CoinsCountParser parser = new CoinsCountParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
