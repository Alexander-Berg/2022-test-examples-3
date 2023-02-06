package ru.yandex.market.api.opinion;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

public class OpinionAddResultParserTest extends UnitTestBase {
    private OpinionAddResultParser parser;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new OpinionAddResultParser();
    }

    @Test
    public void parseOk() {
        PublishResult result = parse("add-opinion-without-error.json");
        Assert.assertEquals(result.getResult(), "ok");
    }

    @Test
    public void parseError() {
        PublishResult result = parse("add-opinion-with-error.json");
        Assert.assertEquals(result.getResult(), "error");
    }

    private PublishResult parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
