package ru.yandex.direct.queryrec;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.queryrec.model.Language;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.queryrec.QueryrecService.ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE;

class QueryrecServiceRecognizeLanguageVieTest {

    private static QueryrecJni queryrecJni = new QueryrecJni(true);
    private QueryrecService queryrecService;
    private PpcProperty<Set<Long>> enableVieLanguageProperty;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        PpcProperty<Set<Long>> enableUzbekLanguageProperty = mock(PpcProperty.class);
        doReturn(ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE)
                .when(enableUzbekLanguageProperty).getOrDefault(emptySet());
        PpcProperty<Set<String>> recognizedLanguagesProperty = mock(PpcProperty.class);
        doReturn(Set.of()).when(recognizedLanguagesProperty).getOrDefault(Set.of());

        enableVieLanguageProperty = mock(PpcProperty.class);

        queryrecService = new QueryrecService(new LanguageRecognizer(), enableUzbekLanguageProperty,
                enableVieLanguageProperty, recognizedLanguagesProperty, mock(UzbekLanguageThresholds.class),
                queryrecJni);
    }

    @Test
    void clientEnabled() {
        long clientIdEnabled = RandomUtils.nextLong(1, Long.MAX_VALUE);
        long clientIdNotEnabled = RandomUtils.nextLong(1, Long.MAX_VALUE);
        doReturn(Collections.singleton(clientIdEnabled)).when(enableVieLanguageProperty).getOrDefault(emptySet());

        String text = "âấầẩẫậ";

        Language languageWithEnabled = queryrecService.recognize(text, ClientId.fromLong(clientIdEnabled), null);
        assertThat(languageWithEnabled, is(Language.VIE));

        Language languageWithNotEnabled = queryrecService.recognize(text, ClientId.fromLong(clientIdNotEnabled), null);
        assertThat(languageWithNotEnabled, not(Language.VIE));
    }
}
