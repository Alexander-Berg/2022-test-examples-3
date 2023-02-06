package ru.yandex.market.api.pers.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.pers.data.ModelQaMessage;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelQaMessageListParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        PagedResult<ModelQaMessage> qaAnswerList = parse("pers-qa_answers__list.json");

        Assert.assertEquals(2, qaAnswerList.getElements().size());
    }

    private PagedResult<ModelQaMessage> parse(String filename) {
        Parser<PagedResult<ModelQaMessage>> parser = new ModelQaAnswerListParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
