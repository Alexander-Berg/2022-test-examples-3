package ru.yandex.direct.web.testing.data;

import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

public class TestKeywords {

    private TestKeywords() {
    }

    public static WebKeyword randomPhraseKeyword(Long id) {
        return new WebKeyword()
                .withId(id)
                .withPhrase(randomAlphabetic(7));
    }

    public static WebKeyword keyword(Long id, String phrase) {
        return new WebKeyword()
                .withId(id)
                .withPhrase(phrase);
    }
}
