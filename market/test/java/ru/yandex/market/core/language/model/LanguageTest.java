package ru.yandex.market.core.language.model;


import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.assertEquals;

/**
 * Тест для {@link Language}.
 */
public class LanguageTest {

    static Stream<Arguments> argumentsForLanguageCreation() {
        return Stream.of(
                Arguments.of("en", Language.ENGLISH),
                Arguments.of("ru", Language.RUSSIAN),
                Arguments.of("de", Language.GERMAN),
                Arguments.of("tr", Language.TURKISH),
                Arguments.of("zh-Hans", Language.CHINESE_SIMPLIFIED),
                Arguments.of("zh-Hant", Language.CHINESE_TRADITIONAL)
        );
    }

    static Stream<Arguments> argumentsForTagCreation() {
        return Stream.of(
                Arguments.of(Language.ENGLISH, "en"),
                Arguments.of(Language.RUSSIAN, "ru"),
                Arguments.of(Language.GERMAN, "de"),
                Arguments.of(Language.TURKISH, "tr"),
                Arguments.of(Language.CHINESE_SIMPLIFIED, "zh-Hans"),
                Arguments.of(Language.CHINESE_TRADITIONAL, "zh-Hant")
        );
    }

    /**
     * Тест успешных случаев работы {@link Language#findByLanguageTag(String)}.
     *
     * @param languageTag - языковой тэг
     * @param expected    - ожидаемое значение енума
     */
    @ParameterizedTest
    @MethodSource("argumentsForLanguageCreation")
    public void testSuccessfulForLanguageTag(String languageTag, Language expected) {
        assertEquals(expected, Language.findByLanguageTag(languageTag));
    }

    /**
     * Тест фэйлов работы {@link Language#findByLanguageTag(String)}.
     */
    @Test(expected = RuntimeException.class)
    public void testSuccessfulForLanguageTag() {
        Language.findByLanguageTag("BLABLABLA");
    }

    /**
     * Тест успешных случаев работы {@link Language#toLanguageTag()} .
     *
     * @param language - языков
     * @param expected - ожидаемое значение тэга
     */
    @ParameterizedTest
    @MethodSource("argumentsForTagCreation")
    public void testSuccessfulForLanguageTag(Language language, String expected) {
        assertEquals(expected, language.toLanguageTag());
    }
}
