package ru.yandex.direct.libs.keywordutils.parser;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.model.Keyword;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordParserLanguageSupportTest {
    @Parameterized.Parameter
    public String language;
    @Parameterized.Parameter(1)
    public String word;

    @Parameterized.Parameters(name = "{0} language support (check by word \"{1}\")")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"Russian", "русский"},
                {"Turkish", "türkçe"},
                {"Ukrainian", "український"},
        });
    }

    @Test
    public void testLanguageSupport() {
        // Expected no errors
        Keyword parsed = KeywordParser.parse(word);
        String actual = parsed.toString();
        assertThat(actual, is(word));
    }
}
