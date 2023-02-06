package ru.yandex.direct.core.entity.keyword.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KeywordWithMinusesTest {

    @Test
    public void fromPhrase_success_whenNoMinusKeywords() {
        KeywordWithMinuses keyword = KeywordWithMinuses.fromPhrase("фраза");
        assertThat(keyword.getPlusKeyword()).isEqualTo("фраза");
        assertThat(keyword.getMinusKeywords()).isEmpty();
    }

    @Test
    public void fromPhrase_success_whenMinusKeywordsPresent() {
        KeywordWithMinuses keyword = KeywordWithMinuses.fromPhrase("фраза -с минус -фразами");
        assertThat(keyword.getPlusKeyword()).isEqualTo("фраза");
        assertThat(keyword.getMinusKeywords()).contains("с минус", "фразами");
    }

    @Test
    public void fromPhrase_success_withEmptyPhrase() {
        KeywordWithMinuses keyword = KeywordWithMinuses.fromPhrase("");
        assertThat(keyword.getPlusKeyword()).isEmpty();
        assertThat(keyword.getMinusKeywords()).isEmpty();
    }

    @Test
    public void fromPhrase_success_whenNoPlusKeyword() {
        KeywordWithMinuses keyword = KeywordWithMinuses.fromPhrase("-с минус -фразами");
        assertThat(keyword.getPlusKeyword()).isEmpty();
        assertThat(keyword.getMinusKeywords()).contains("с минус", "фразами");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void fromPhrase_NPE_onNullPhrase() {
        assertThatThrownBy(() -> KeywordWithMinuses.fromPhrase(null))
                .isInstanceOf(NullPointerException.class);
    }

}
