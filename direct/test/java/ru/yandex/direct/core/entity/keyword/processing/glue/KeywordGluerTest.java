package ru.yandex.direct.core.entity.keyword.processing.glue;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.keyword.processing.NormalizedKeywordWithMinuses;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.processing.glue.KeywordGluer.glueKeywords;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class KeywordGluerTest {

    @Parameterized.Parameter
    public List<String> keywords;

    @Parameterized.Parameter(1)
    public List<String> expectedGluedKeywords;

    @Parameterized.Parameters(name = "{0} => {2}")
    public static Object[][] parameters() {
        return new Object[][] {
                {
                        List.of("A B -C", "A B -D"),
                        List.of("A B -C -D", "A B -C -D")
                },
                {
                        List.of("A B -C -E", "A B -D -E"),
                        List.of("A B -C -D -E", "A B -C -D -E")
                },
                {
                        List.of("конь", "конь"),
                        List.of("конь", "конь")
                },
                {
                        List.of("конь", "коня"),
                        List.of("конь", "конь")
                },
                {
                        List.of("коня", "конь"),
                        List.of("коня", "коня")
                },
                {
                        List.of("конь", "вася"),
                        List.of("конь", "вася")
                },
                {
                        List.of("ухи", "ухо"),
                        List.of("ухи", "ухо")
                },

                // фразы с несколькими леммами в словах
                {
                        List.of("уха", "ухо"),
                        List.of("уха", "уха")
                },
                {
                        List.of("ухо", "уха"),
                        List.of("уха", "уха")
                },
                {
                        List.of("дешево купить холодильник", "дешевый холодильник купить"),
                        List.of("дешево купить холодильник", "дешево купить холодильник")
                },
                {
                        List.of("андроид игра новый", "андроид игра нова"),
                        List.of("андроид игра нова", "андроид игра нова")
                },
                {
                        List.of("андроид игра новый -раз", "андроид игра нова -два", "андроид игра новый -три"),
                        List.of("андроид игра нова -два -раз -три",
                                "андроид игра нова -два -раз -три",
                                "андроид игра нова -два -раз -три")
                },
        };
    }

    @Test
    public void glueKeywords_twoWithMinusWords_minusWordsAreGlued() {
        List<KeywordWithMinuses> keywordsWithMinuses = mapList(keywords, KeywordParser::parseWithMinuses);

        List<NormalizedKeywordWithMinuses> actual = glueKeywords(new SingleKeywordsCache(),
                mapList(keywordsWithMinuses, NormalizedKeywordWithMinuses::from));

        NormalizedKeywordWithMinuses[] expected = StreamEx.of(expectedGluedKeywords)
                .map(KeywordParser::parseWithMinuses)
                .map(NormalizedKeywordWithMinuses::from)
                .toArray(NormalizedKeywordWithMinuses.class);
        assertThat(actual).containsExactly(expected);
    }
}
