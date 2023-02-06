package ru.yandex.market.loyalty.core.service.generator;


import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class BadWordsFilterTest {

    @Test
    public void test() {
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("pidor"), Matchers.equalTo(true));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("PIDOR"), Matchers.equalTo(true));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("timudak"), Matchers.equalTo(true));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("dzpezda"), Matchers.equalTo(true));

        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("asdaklsj"), Matchers.equalTo(false));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("pirog"), Matchers.equalTo(false));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("hub"), Matchers.equalTo(false));
        MatcherAssert.assertThat(BadWordsFilter.isContainsBad("manddaa"), Matchers.equalTo(false));

    }
}
