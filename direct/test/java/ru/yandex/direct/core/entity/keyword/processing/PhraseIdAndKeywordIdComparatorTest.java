package ru.yandex.direct.core.entity.keyword.processing;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.keyword.model.Keyword;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;

public class PhraseIdAndKeywordIdComparatorTest {

    private static final Comparator<Keyword> PHRASE_ID_KEYWORD_ID_COMPARATOR =
            new PhraseIdAndKeywordIdComparator();

    private static final Keyword kw1 = defaultKeyword().withPhraseBsId(BigInteger.ZERO).withId(1L);
    private static final Keyword kw2 = defaultKeyword().withPhraseBsId(BigInteger.valueOf(3L)).withId(2L);
    private static final Keyword kw3 = defaultKeyword().withPhraseBsId(BigInteger.valueOf(4L)).withId(3L);
    private static final Keyword kw4 = defaultKeyword().withPhraseBsId(BigInteger.ZERO).withId(4L);
    private static final Keyword kw5 = defaultKeyword().withPhraseBsId(BigInteger.valueOf(5L)).withId(5L);

    private List<Keyword> keywordList;

    @Test
    public void compare_FirstPhraseBsIdIsZero() {
        assertThat(PHRASE_ID_KEYWORD_ID_COMPARATOR.compare(kw1, kw2), equalTo(1));
    }

    @Test
    public void compare_SecondPhraseBsIdIsZero() {
        assertThat(PHRASE_ID_KEYWORD_ID_COMPARATOR.compare(kw2, kw1), equalTo(-1));
    }

    @Test
    public void compare_BothPhraseBsIdsAreZero() {
        assertThat(PHRASE_ID_KEYWORD_ID_COMPARATOR.compare(kw1, kw4), equalTo(-1));
    }

    @Test
    public void compare_FirstPhraseBsIdIsZeroFirstIdLessSecond() {
        keywordList = asList(kw1, kw2);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw2));
    }

    @Test
    public void compare_FirstPhraseBsIdIsZeroFirstIdBiggerSecond() {
        keywordList = asList(kw4, kw3);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw3));
    }

    @Test
    public void compare_SecondPhraseBsIdIsZeroFirstIdLessSecond() {
        keywordList = asList(kw3, kw4);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw3));
    }

    @Test
    public void compare_SecondPhraseBsIdIsZeroFirstIdBiggerSecond() {
        keywordList = asList(kw2, kw1);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw2));
    }

    @Test
    public void compare_BothPhraseBsIdIsZeroFirstIdLessSecond() {
        keywordList = asList(kw1, kw4);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw1));
    }

    @Test
    public void compare_BothPhraseBsIdIsZeroFirstIdBiggerSecond() {
        keywordList = asList(kw4, kw1);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw1));
    }

    @Test
    public void compare_BothPhraseBsIdIsNotZeroFirstIdLessSecond() {
        keywordList = asList(kw2, kw3);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw2));
    }

    @Test
    public void compare_BothPhraseBsIdIsNotZeroFirstIdBiggerSecond() {
        keywordList = asList(kw3, kw2);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList.get(0), equalTo(kw2));
    }

    @Test
    public void compare_Phrases() {
        List<Keyword> expectedList = asList(kw2, kw3, kw5, kw1, kw4);
        keywordList = asList(kw1, kw2, kw3, kw4, kw5);
        keywordList.sort(PHRASE_ID_KEYWORD_ID_COMPARATOR);
        assertThat("Список отсортирован верно", keywordList, equalTo(expectedList));
    }
}
