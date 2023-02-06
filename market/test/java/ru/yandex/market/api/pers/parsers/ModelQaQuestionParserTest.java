package ru.yandex.market.api.pers.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.pers.data.ModelQaQuestion;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelQaQuestionParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        ModelQaQuestion q = parse("pers-qa_questions__question.json");

        Assert.assertEquals("kto-nibud-polzoval-kak-u-nego-kamera-zvuk-ekran", q.getSlug());
        Assert.assertEquals(4, q.getAnswersCount());
    }

    private ModelQaQuestion parse(String filename) {
        Parser<ModelQaQuestion> parser = new ModelQaQuestionParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
