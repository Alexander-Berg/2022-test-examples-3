package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.parser.json.AbstractJsonParser;

/**
 * @author dimkarp93
 */
public class CommonListJsonParserTest {
    private static class IntParser extends AbstractJsonParser<Integer> {
        private int value;

        protected IntParser() {
            onValue("/", v -> value = v.asInt());
        }

        @Override
        public Integer getParsed() {
            return value;
        }
    }

    @Test
    public void parseAll() {
        byte[] data = ResourceHelpers.getResource("list.json");
        CommonListJsonParser<Integer> jsonParser = CommonListJsonParser.<Integer>builder()
                .withElementParser(new IntParser())
                .build();

        List<Integer> result = jsonParser.parse(data);
        Assert.assertEquals(Arrays.asList(1, 5, 3, 2, 4), result);
    }

    @Test
    public void parseLimit() {
        byte[] data = ResourceHelpers.getResource("list.json");
        CommonListJsonParser<Integer> jsonParser = CommonListJsonParser.<Integer>builder()
                .withElementParser(new IntParser())
                .withLimit(2)
                .build();

        List<Integer> result = jsonParser.parse(data);
        Assert.assertEquals(Arrays.asList(1, 5), result);
    }
}
