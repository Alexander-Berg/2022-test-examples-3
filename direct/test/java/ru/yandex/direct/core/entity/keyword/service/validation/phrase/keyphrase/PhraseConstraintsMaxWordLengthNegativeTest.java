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
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.maxPlusWordLength;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

@RunWith(Parameterized.class)
public class PhraseConstraintsMaxWordLengthNegativeTest {

    private static final String MAX_LENGTH_WORD = StringUtils.leftPad("s", WORD_MAX_LENGTH, "o");

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"недопустимое по длине слово", MAX_LENGTH_WORD + "1"},
                {"сверхдлинное слово с коротким", "привет f" + MAX_LENGTH_WORD},
                {"два недопустимых по длине слова", MAX_LENGTH_WORD + "f f" + MAX_LENGTH_WORD},
                {"недопустимое по длине слово через точку с другим словом", MAX_LENGTH_WORD + "f.абв"},
                {"максимально длинное слово с точкой в конце", MAX_LENGTH_WORD + "."},
                {"два максимально длинных слова, разделенные точкой", MAX_LENGTH_WORD + "." + MAX_LENGTH_WORD},
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
        assertThat(maxPlusWordLength(keywordWithMinuses).apply(keyword), notNullValue());
    }
}
