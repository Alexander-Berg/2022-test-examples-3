package ru.yandex.market.api.pers.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.pers.data.ModelQaQuestion;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelQaQuestionListParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        PagedResult<ModelQaQuestion> questionList = parse("pers-qa_questions__list.json");

        Assert.assertEquals(10, questionList.getElements().size());
    }

    private PagedResult<ModelQaQuestion> parse(String filename) {
        Parser<PagedResult<ModelQaQuestion>> parser = new ModelQaQuestionListParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
