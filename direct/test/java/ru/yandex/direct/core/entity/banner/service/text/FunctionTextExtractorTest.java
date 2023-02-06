package ru.yandex.direct.core.entity.banner.service.text;

import java.util.Map;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.text.FunctionTextExtractor.functionTextExtractor;

public class FunctionTextExtractorTest {

    private static final String TEXT_1 = "text1";
    private static final String TEXT_2 = "text2";

    private final FunctionTextExtractor<TextHolder> functionTextExtractor =
            functionTextExtractor(TextHolder.class, TextHolder::get);

    @Test
    public void extractsTextWhenTextIsNotNull() {
        TextHolder textHolder = new TextHolder(TEXT_1);
        Map<TextHolder, String> result = functionTextExtractor.extractTexts(singletonList(textHolder));
        assertThat(result)
                .containsOnlyKeys(textHolder)
                .containsValues(TEXT_1);
    }

    @Test
    public void returnsNoResultWhenTextIsNull() {
        TextHolder textHolder = new TextHolder(null);
        Map<TextHolder, String> result = functionTextExtractor.extractTexts(singletonList(textHolder));
        assertThat(result).isEmpty();
    }

    @Test
    public void extractsTwoTextsWhenBothNotNull() {
        TextHolder textHolder1 = new TextHolder(TEXT_1);
        TextHolder textHolder2 = new TextHolder(TEXT_2);
        Map<TextHolder, String> result = functionTextExtractor.extractTexts(asList(textHolder1, textHolder2));
        assertThat(result).containsOnly(
                Map.entry(textHolder1, TEXT_1),
                Map.entry(textHolder2, TEXT_2));
    }

    @Test
    public void extractsOneTextWhenOneTextIsNotNullAndOneIsNull() {
        TextHolder textHolder1 = new TextHolder(TEXT_1);
        TextHolder textHolder2 = new TextHolder(null);
        Map<TextHolder, String> result = functionTextExtractor.extractTexts(asList(textHolder1, textHolder2));
        assertThat(result).containsOnly(Map.entry(textHolder1, TEXT_1));
    }

    public static class TextHolder {

        private final String text;

        public TextHolder(String text) {
            this.text = text;
        }

        public String get() {
            return text;
        }
    }
}
