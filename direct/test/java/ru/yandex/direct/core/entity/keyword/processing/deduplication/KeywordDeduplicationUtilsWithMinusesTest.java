package ru.yandex.direct.core.entity.keyword.processing.deduplication;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.processing.deduplication.KeywordDeduplicationUtils.checkDuplication;

@RunWith(Parameterized.class)
public class KeywordDeduplicationUtilsWithMinusesTest {

    @Parameterized.Parameter(0)
    public String keyword;

    @Parameterized.Parameter(1)
    public String anotherKeyword;

    @Parameterized.Parameter(2)
    public boolean expectedIsDuplicate;

    @Parameterized.Parameters(name = "фраза: {0}, другая фраза {1}, являются дубликатами: {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"купить слона -коня", "купить слона -коня", true},
                {"купить слона -лося", "купить коня -коня", false},
                //не сортированные минус фразы
                {"купить слона -коня -лося", "купить коня -лося -коня", false},
        });
    }

    @Test
    public void checkDuplicationForKeywords_WorksFine() {
        KeywordWithMinuses thisKeyword = KeywordParser.parseWithMinuses(keyword);
        KeywordWithMinuses otherKeyword = KeywordParser.parseWithMinuses(anotherKeyword);
        boolean isDuplicates = checkDuplication(thisKeyword, otherKeyword);
        assertThat("дубликация фраз не соответсвует ожидаемому", isDuplicates, is(expectedIsDuplicate));
    }
}
