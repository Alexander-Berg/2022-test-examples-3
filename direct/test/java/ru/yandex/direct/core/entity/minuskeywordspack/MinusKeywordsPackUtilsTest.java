package ru.yandex.direct.core.entity.minuskeywordspack;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class MinusKeywordsPackUtilsTest {

    @Test
    public void minusKeywordsToJson_EmptyMinusKeywords_returnsNull() {
        assertThat(MinusKeywordsPackUtils.minusKeywordsToJson(emptyList()), nullValue());
    }

    @Test
    public void minusKeywordsToJson_NullMinusKeywords_returnsNull() {
        assertThat(MinusKeywordsPackUtils.minusKeywordsToJson(null), nullValue());
    }

    @Test
    public void minusKeywordsFromJson_EmptyString_ReturnsNull() {
        assertThat(MinusKeywordsPackUtils.minusKeywordsFromJson(""), empty());
    }

}
