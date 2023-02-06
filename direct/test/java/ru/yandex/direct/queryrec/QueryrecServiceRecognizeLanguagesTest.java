package ru.yandex.direct.queryrec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.queryrec.model.ISO639Code;
import ru.yandex.direct.queryrec.model.Language;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.queryrec.QueryrecService.ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE;
import static ru.yandex.direct.queryrec.model.Language.BELARUSIAN;
import static ru.yandex.direct.queryrec.model.Language.CZECH;
import static ru.yandex.direct.queryrec.model.Language.ENGLISH;
import static ru.yandex.direct.queryrec.model.Language.GERMAN;
import static ru.yandex.direct.queryrec.model.Language.KAZAKH;
import static ru.yandex.direct.queryrec.model.Language.POLISH;
import static ru.yandex.direct.queryrec.model.Language.PORTUGUESE;
import static ru.yandex.direct.queryrec.model.Language.RUSSIAN;
import static ru.yandex.direct.queryrec.model.Language.SPANISH;
import static ru.yandex.direct.queryrec.model.Language.TURKISH;
import static ru.yandex.direct.queryrec.model.Language.UKRAINIAN;
import static ru.yandex.direct.queryrec.model.Language.UNKNOWN;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryrecServiceRecognizeLanguagesTest {

    private static QueryrecJni queryrecJni = new QueryrecJni(true);
    private QueryrecService queryrecService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        PpcProperty<Set<Long>> enableUzbekLanguageProperty = mock(PpcProperty.class);
        doReturn(ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE)
                .when(enableUzbekLanguageProperty).getOrDefault(emptySet());
        PpcProperty<Set<String>> recognizedLanguagesProperty = mock(PpcProperty.class);
        doReturn(Arrays.stream(Language.values())
                .map(Language::getIso639Code).map(ISO639Code::toString).collect(Collectors.toSet())
        ).when(recognizedLanguagesProperty).getOrDefault(Set.of());

        UzbekLanguageThresholds thresholds = new UzbekLanguageThresholds(mock(PpcProperty.class),
                mock(PpcProperty.class), mock(PpcProperty.class), mock(PpcProperty.class));

        // так как используем Lifecycle.PER_CLASS, для тестов инициализируется один экземпляр тестового класса
        queryrecService = new QueryrecService(new LanguageRecognizer(), enableUzbekLanguageProperty,
                mock(PpcProperty.class), recognizedLanguagesProperty, thresholds, queryrecJni);
    }

    @AfterAll
    static void afterAll() {
        queryrecJni.destroy();
    }

    static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"пустая строка", "", UNKNOWN},
                {"строка из пробелов", "  ", UNKNOWN},

                {"казахский язык (уникальные нац. символы)", "қазақ тілі", KAZAKH},
                {"украинский язык (уникальные нац. символы)", "українська мова", UKRAINIAN},
                {"русский язык (только кириллица без др. нац. символов)", "по-русски", RUSSIAN},

                {"украинский язык (queryrec)", "оплата готівкою", UKRAINIAN},
                {"белорусский язык (queryrec)", "карты, грошы і два ствала", BELARUSIAN},

                {"немецкий язык (yникальные нац. символы)", "Schläfst", GERMAN},
                {"турецкий язык (уникальные нац. символы)", "Şimdiki", TURKISH},
                {"национальные символы отсутствуют", "42", UNKNOWN},

                {"турецкий (queryrec)", "Avrupa ve Asya", TURKISH},
                {"турецкий - большая вероятность (queryrec)", "ayni grupta or same", TURKISH},
                {"немецкий (queryrec)", "setzt sich aus zusammen", GERMAN},

                {"немецкий язык - общие с турецким нац. символы (queryrec)", "föderal", GERMAN},
                {"турецкий язык - общие с немецким нац. символы (queryrec)", "ötesi", TURKISH},

                {"немецкий язык - только общие с турецкие нац. символы (queryrec)", "Üöü", GERMAN},

                {"латиница (queryrec)", "test", ENGLISH},

                {"испанский (queryrec)", "El idioma español", SPANISH},
                {"испанский, панграмма (queryrec)",
                        "La cigüeña tocaba cada vez mejor el saxofón y el búho pedía kiwi y queso", SPANISH},
                {"португальский (queryrec)", "Um pequeno jabuti xereta viu dez cegonhas felizes", PORTUGUESE},
                {"чешский (yникальные нац. символы)", "Příliš žluťoučký kůň úpěl ďábelské ódy", CZECH},
                {"польский (yникальные нац. символы)", "Pchnąć w tę łódź jeża lub ośm skrzyń fig", POLISH},
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void testParametrized(String testName, String text, Language language) {
        Language actualLanguage = queryrecService.recognize(text, null, null);
        assertThat(actualLanguage, is(language));
    }
}
