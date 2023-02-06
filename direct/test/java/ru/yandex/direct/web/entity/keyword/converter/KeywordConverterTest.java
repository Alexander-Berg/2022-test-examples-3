package ru.yandex.direct.web.entity.keyword.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.SimpleConversionMatcher.converts;
import static ru.yandex.direct.web.entity.keyword.converter.KeywordConverter.webKeywordToCoreKeyword;
import static ru.yandex.direct.web.entity.keyword.converter.KeywordConverter.webKeywordsToCoreKeywords;

@SuppressWarnings("ConstantConditions")
public class KeywordConverterTest {

    @Test
    public void convertPrimitives() {
        Set<String> fieldsNotToFill = ImmutableSet.of("id", "price", "priceContext");
        assertThat(KeywordConverter::webKeywordToCoreKeyword,
                converts(new WebKeyword(), fieldsNotToFill));
    }

    @Test
    public void convertIdWhenIdIsNull() {
        WebKeyword webKeyword = new WebKeyword().withId(null);
        assertThat(webKeywordToCoreKeyword(webKeyword).getId(), nullValue());
    }

    @Test
    public void convertIdWhenIdIsZero() {
        WebKeyword webKeyword = new WebKeyword().withId(0L);
        assertThat(webKeywordToCoreKeyword(webKeyword).getId(), nullValue());
    }

    @Test
    public void convertIdWhenIdIsPositive() {
        WebKeyword webKeyword = new WebKeyword().withId(1L);
        assertThat(webKeywordToCoreKeyword(webKeyword).getId(), equalTo(1L));
    }

    @Test
    public void convertPricesWhenTheyAreNull() {
        WebKeyword webKeyword = new WebKeyword();
        Keyword keyword = webKeywordToCoreKeyword(webKeyword);
        assertThat(keyword.getPrice(), nullValue());
        assertThat(keyword.getPriceContext(), nullValue());
    }

    @Test
    public void convertPricesWhenTheyAreZeroes() {
        WebKeyword webKeyword = new WebKeyword()
                .withPrice(0.0)
                .withPriceContext(0.0);
        Keyword keyword = webKeywordToCoreKeyword(webKeyword);
        assertThat(keyword.getPrice(), nullValue());
        assertThat(keyword.getPriceContext(), nullValue());
    }

    @Test
    public void convertPricesWhenTheyArePositive() {
        WebKeyword webKeyword = new WebKeyword()
                .withPrice(123.0)
                .withPriceContext(345.0);
        Keyword keyword = webKeywordToCoreKeyword(webKeyword);
        assertThat(keyword.getPrice().compareTo(BigDecimal.valueOf(123L)), is(0));
        assertThat(keyword.getPriceContext().compareTo(BigDecimal.valueOf(345L)), is(0));
    }

    @Test
    public void convertNullFields() {
        Keyword keyword = webKeywordToCoreKeyword(new WebKeyword());
        assertThat(keyword, notNullValue());
    }

    @Test
    public void convertNull() {
        Keyword keyword = webKeywordToCoreKeyword(null);
        assertThat(keyword, nullValue());
    }

    @Test
    public void convertNullList() {
        List<Keyword> keywords = webKeywordsToCoreKeywords(null);
        assertThat(keywords, nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertListWithNullItem() {
        List<Keyword> keywords =
                webKeywordsToCoreKeywords(asList(new WebKeyword(), null));
        assertThat(keywords, contains(notNullValue(), nullValue()));
    }
}
