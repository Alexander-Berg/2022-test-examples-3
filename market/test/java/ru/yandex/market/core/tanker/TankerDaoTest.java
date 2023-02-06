package ru.yandex.market.core.tanker;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.core.tanker.dao.TankerDao;
import ru.yandex.market.core.tanker.model.MessageSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "TankerDaoTest.before.csv")
class TankerDaoTest extends FunctionalTest {

    @Autowired
    private TankerDao tankerDao;

    static Stream<Arguments> argumentsForGettingSingleKeyset() {
        return Stream.of(
                Arguments.of("keyset1", Language.CHINESE_SIMPLIFIED, new MessageSet(
                        Language.CHINESE_SIMPLIFIED,
                        Map.of(
                                "key11", "value11_hans",
                                "key12", "value12_hans"
                        )
                )),
                Arguments.of("keyset3", Language.RUSSIAN, new MessageSet(Language.RUSSIAN, Map.of())),
                Arguments.of("keyset4", Language.RUSSIAN, new MessageSet(Language.RUSSIAN, Map.of())),
                Arguments.of("keyset4", Language.CHINESE_SIMPLIFIED, new MessageSet(
                        Language.CHINESE_SIMPLIFIED,
                        Map.of("key41", "value41_hans")
                ))
        );
    }

    static Stream<Arguments> argumentsForGettingAllKeys() {
        return Stream.of(
                Arguments.of(Set.of("keyset1", "keyset2", "keyset3"), Language.CHINESE_SIMPLIFIED,
                        Map.of(
                                "keyset1", new MessageSet(
                                        Language.CHINESE_SIMPLIFIED,
                                        Map.of(
                                                "key11", "value11_hans",
                                                "key12", "value12_hans"
                                        )
                                ),
                                "keyset2", new MessageSet(
                                        Language.CHINESE_SIMPLIFIED,
                                        Map.of("key21", "value21_hans")
                                )
                        )
                ),
                Arguments.of(Set.of("keyset1", "keyset2", "keyset3"), Language.RUSSIAN,
                        Map.of(
                                "keyset1", new MessageSet(
                                        Language.RUSSIAN,
                                        Map.of("key11", "value11_ru")
                                ),
                                "keyset2", new MessageSet(
                                        Language.RUSSIAN,
                                        Map.of("key21", "value21_ru")
                                )
                        )
                ),
                Arguments.of(Set.of("keyset3"), Language.RUSSIAN, Map.of()),
                Arguments.of(Set.of("keyset4"), Language.RUSSIAN, Map.of()),
                Arguments.of(Set.of("keyset4"), Language.CHINESE_SIMPLIFIED,
                        Map.of("keyset4", new MessageSet(
                                Language.CHINESE_SIMPLIFIED,
                                Map.of("key41", "value41_hans")
                        ))
                )
        );
    }

    /**
     * Тестирует загрузку коллекции keyset'ов.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("argumentsForGettingAllKeys")
    void testGetKeySets(Set<String> keySetNames, Language language, Map<String, MessageSet> expected) {
        var actual = tankerDao.getMessageSet(keySetNames, language);

        assertThat(actual).hasSameSizeAs(keySetNames);
        for (var entry : expected.entrySet()) {
            var expectedKeys = entry.getValue().getMessages();
            assertThat(actual)
                    .hasEntrySatisfying(
                            entry.getKey(),
                            ms -> assertThat(ms.getMessages()).isEqualTo(expectedKeys)
                    );
        }
    }

    /**
     * Тестирует загрузку одного keyset'а.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("argumentsForGettingSingleKeyset")
    void testGetKeySet(String keySetName, Language language, MessageSet expected) {
        var actual = tankerDao.getMessageSet(keySetName, language);

        assertThat(actual).isNotNull();
        assertThat(actual.getMessages()).isEqualTo(expected.getMessages());
        assertThat(actual.getLanguage()).isEqualTo(expected.getLanguage());
    }

    @Test
    @DbUnitDataSet(after = "TankerDaoTest.saveMessageSet.after.csv")
    void saveMessageSet() {
        tankerDao.saveMessageSet("keyset1", new MessageSet(Language.RUSSIAN, Map.of(
                "key11", "VALUE11_RU",
                "key12", "VALUE12_RU",
                "key13", "VALUE13_RU"
        )));
        tankerDao.saveMessageSet("keyset1", new MessageSet(Language.ENGLISH, Map.of(
                "key12", "VALUE12_EN",
                "key13", "VALUE13_EN"
        )));
    }
}
