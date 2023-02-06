package ru.yandex.market.api.pers.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelQaCountParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        Integer count = parse("pers-qa_questions__count.json");

        Assert.assertEquals(3, count.intValue());
    }

    private Integer parse(String filename) {
        Parser<Integer> parser = new ModelQaCountParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
