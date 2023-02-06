package ru.yandex.market.core.language.service;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.language.dao.MemCachedPreferrableLanguageDao;
import ru.yandex.market.core.language.dao.PreferableLanguageDao;
import ru.yandex.market.core.language.model.Language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LanguageServiceTest extends FunctionalTest {

    @Autowired
    private LanguageService tested;
    @Autowired
    private PreferableLanguageDao languageDao;

    static Stream<Arguments> regionSearchPositiveArgs() {
        return Stream.of(
                Arguments.of(1l, Optional.of(Language.RUSSIAN)),
                Arguments.of(5l, Optional.of(Language.GERMAN)),
                Arguments.of(6l, Optional.of(Language.CHINESE_TRADITIONAL)),
                Arguments.of(2l, Optional.of(Language.RUSSIAN)),
                Arguments.of(3l, Optional.of(Language.RUSSIAN)),
                Arguments.of(7l, Optional.empty())
        );
    }

    static Stream<Arguments> regionSearchFailureArgs() {
        return Stream.of(
                Arguments.of(4l),
                Arguments.of(9l)
        );
    }

    static Stream<Arguments> shopSearchPositiveArgs() {
        return Stream.of(
                Arguments.of(1l, Optional.of(Language.RUSSIAN)),
                Arguments.of(2l, Optional.of(Language.GERMAN)),
                Arguments.of(3l, Optional.empty())
        );
    }

    /**
     * Очистка кэша перед тестами.
     */
    @BeforeEach
    public void cleanCaches() {
        ((MemCachedPreferrableLanguageDao) languageDao).clean();
    }

    /**
     * Тест, проверяющий успешную работу метода, возвращающего предпочтительный язык коммуникаций в указанном регионе.
     * <p>
     * Дерево регионов и предпочитаемые языки в странах:
     * <p>
     * 4 Евразия
     * |                           |                                |                     |
     * 1 Россия (ru)            5 Германия (de)                 6 Китай (zh-Hant)      7. Япония
     * |
     * 2 Московская обл
     * |
     * 3 Димитровский район.
     *
     * @param regionId      - айди региона, для которого ищется предпочитаемый язык
     * @param expectedValue - ожидаемое значение языка
     */
    @DbUnitDataSet(before = "databaseTestData.csv")
    @ParameterizedTest
    @MethodSource("regionSearchPositiveArgs")
    public void testFindPreferableLanguageForRegion_successful(long regionId, Optional<Language> expectedValue) {
        assertEquals(expectedValue, tested.findPreferableLanguageForRegion(regionId));
    }

    /**
     * Тест, проверяющий некорректную работу сервиса в случаях:
     * 1. Регион объема больше страны.
     * 2. Региона нет в базе регионов.
     * <p>
     * Дерево регионов и предпочитаемые языки в странах:
     * <p>
     * 4 Евразия
     * |                           |                                |                     |
     * 1 Россия (ru)            5 Германия (de)                 6 Китай (zh-Hant)      7. Япония
     * |
     * 2 Московская обл
     * |
     * 3 Димитровский район.
     *
     * @param regionId - айди региона, для которого ищется предпочитаемый язык
     */
    @DbUnitDataSet(before = "databaseTestData.csv")
    @ParameterizedTest
    @MethodSource("regionSearchFailureArgs")
    public void testFindPreferableLanguageForRegion_failures(long regionId) {
        assertThrows(IllegalArgumentException.class, () -> tested.findPreferableLanguageForRegion(regionId));
    }

    /**
     * Тест, проверяющий успешную работу метода, возвращающего предпочтительный язык коммуникаций для магазина.
     * <p>
     * Дерево регионов и предпочитаемые языки в странах:
     * <p>
     * 4 Евразия
     * |                           |                                |                     |
     * 1 Россия (ru)            5 Германия (de)                 6 Китай (zh-Hant)      7. Япония
     * |
     * 2 Московская обл
     * |
     * 3 Димитровский район.
     * <p>
     * Магазины и их страны:
     * 1 - Россия
     * 2 - Германия
     * 3 - неизвестно
     *
     * @param shopId        - айди магазина, для которого ищется предпочитаемый язык
     * @param expectedValue - ожидаемое значение языка
     */
    @DbUnitDataSet(before = "databaseTestData.csv")
    @ParameterizedTest
    @MethodSource("shopSearchPositiveArgs")
    public void testFindPreferableLanguageForShop(long shopId, Optional<Language> expectedValue) {
        assertEquals(expectedValue, tested.findPreferableLanguageForShop(shopId));
    }
}
