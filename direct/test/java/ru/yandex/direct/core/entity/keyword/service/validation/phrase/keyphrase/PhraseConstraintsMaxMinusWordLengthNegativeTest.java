package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.WORD_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.maxMinusWordLength;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

@RunWith(Parameterized.class)
public class PhraseConstraintsMaxMinusWordLengthNegativeTest {

    private static final String MAX_LENGTH_MINUS_WORD = "-" + StringUtils.leftPad("s", WORD_MAX_LENGTH, "o");

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"недопустимое по длине минус слово", "xxx " + MAX_LENGTH_MINUS_WORD + "1"},
                {"сверхдлинное минус слово с коротким", "привет -minus " + MAX_LENGTH_MINUS_WORD + "1"},
                {"два недопустимых по длине минус слова",
                        "привет " + MAX_LENGTH_MINUS_WORD + "f " + MAX_LENGTH_MINUS_WORD + "1"},
        });
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParameterized() {
        KeywordWithMinuses keywordWithMinuses = parseWithMinuses(keyword);
        assertThat(maxMinusWordLength(keywordWithMinuses).apply(keyword), notNullValue());
    }
}
