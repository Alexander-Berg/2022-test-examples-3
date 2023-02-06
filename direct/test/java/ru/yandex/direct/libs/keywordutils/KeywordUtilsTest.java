package ru.yandex.direct.libs.keywordutils;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class KeywordUtilsTest {
    @Parameterized.Parameter(0)
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameter(2)
    public Set<String> stopWords;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Object[][] params() {
        ImmutableSet<String> stopwords = ImmutableSet.of("of", "or", "a", "the");
        return new Object[][]{
                {"moscow Times", "moscow Times", stopwords},
                {"time of Moscow", "time Moscow", stopwords},
                {"time +of Moscow", "time +of Moscow", stopwords},
                {"time !of Moscow", "time !of Moscow", stopwords},
                {"time of [Moscow or NYC and SF]", "time [Moscow NYC and SF]", stopwords},
                {"time of [Moscow +or NYC and SF]", "time [Moscow +or NYC and SF]", stopwords},
        };
    }

    @Test
    public void test() {
        assertThat(
                KeywordUtils.stripStopWords(KeywordParser.parse(input), stopWords::contains).toString(),
                Matchers.equalTo(KeywordParser.parse(expected).toString())
        );
    }
}
