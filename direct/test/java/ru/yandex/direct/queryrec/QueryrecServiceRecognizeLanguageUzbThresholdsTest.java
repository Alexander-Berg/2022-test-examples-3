package ru.yandex.direct.queryrec;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.direct.common.db.PpcProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.queryrec.QueryrecService.ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE;
import static ru.yandex.direct.queryrec.model.Language.ENGLISH;
import static ru.yandex.direct.queryrec.model.Language.RUSSIAN;
import static ru.yandex.direct.queryrec.model.Language.TURKISH;
import static ru.yandex.direct.queryrec.model.Language.UZBEK;
import static ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryrecServiceRecognizeLanguageUzbThresholdsTest {
    private static QueryrecJni queryrecJni = new QueryrecJni(true);
    private QueryrecService queryrecService;

    private PpcProperty<Double> cyrillicUzbekToRussianThresholdProperty;
    private PpcProperty<Double> latinUzbekToRussianThresholdProperty;
    private PpcProperty<Double> uzbekToTurkishThresholdProperty;
    private PpcProperty<Double> uzbekToUnknownThresholdProperty;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        PpcProperty<Set<Long>> clientsWithEnabledUzbekLanguageProperty = mock(PpcProperty.class);
        doReturn(ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE)
                .when(clientsWithEnabledUzbekLanguageProperty).getOrDefault(anySet());
        PpcProperty<Set<String>> recognizedLanguagesProperty = mock(PpcProperty.class);
        doReturn(Set.of()).when(recognizedLanguagesProperty).getOrDefault(Set.of());

        cyrillicUzbekToRussianThresholdProperty = mock(PpcProperty.class);
        latinUzbekToRussianThresholdProperty = mock(PpcProperty.class);
        uzbekToTurkishThresholdProperty = mock(PpcProperty.class);
        uzbekToUnknownThresholdProperty = mock(PpcProperty.class);

        UzbekLanguageThresholds thresholds = new UzbekLanguageThresholds(cyrillicUzbekToRussianThresholdProperty,
                latinUzbekToRussianThresholdProperty, uzbekToTurkishThresholdProperty, uzbekToUnknownThresholdProperty);

        queryrecService = new QueryrecService(new LanguageRecognizer(), clientsWithEnabledUzbekLanguageProperty,
                mock(PpcProperty.class), recognizedLanguagesProperty, thresholds, queryrecJni);
    }

    @AfterAll
    static void afterAll() {
        queryrecJni.destroy();
    }

    @Test
    void recognize_RussianText_PropertyCyrillicUzbekToRussianIsHigh_RecognizedAsUzbek() {
        String text = "Продам гараж дешево";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(RUSSIAN));
        doReturn(1.0).when(cyrillicUzbekToRussianThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
    }

    @Test
    void recognize_UzbekText_PropertyCyrillicUzbekToRussianIsLow_RecognizedAsRussian() {
        String text = "Танишганимдан хурсандман";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
        doReturn(0.0).when(cyrillicUzbekToRussianThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(RUSSIAN));
    }

    @Test
    void recognize_EnglishText_PropertyLatinUzbekToRussianIsHigh_RecognizedAsUzbek() {
        String text = "Sell cheap fridge";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(ENGLISH));
        doReturn(1.0).when(latinUzbekToRussianThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
    }

    @Test
    void recognize_UzbekText_PropertyLatinUzbekToRussianIsLow_RecognizedAsEnglish() {
        String text = "Men bora olmayman";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
        doReturn(0.0).when(latinUzbekToRussianThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(ENGLISH));
    }

    @Test
    void recognize_TurkishText_PropertyUzbekToTurkishIsHigh_RecognizedAsUzbek() {
        String text = "Sinopark Otel";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(TURKISH));
        doReturn(1.0).when(uzbekToTurkishThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
    }

    @Test
    void recognize_UzbekText_PropertyUzbekToTurkishIsLow_RecognizedAsTurkish() {
        String text = "Tahsin, ma'qullash";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
        doReturn(0.0).when(uzbekToTurkishThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(TURKISH));
    }

    @Test
    void recognize_FrenchText_PropertyUzbekToUnknownIsLow_RecognizedAsEnglish() {
        String text = "...Qanday topsam boladi?";

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(UZBEK));
        doReturn(-0.1).when(uzbekToUnknownThresholdProperty).get();

        assertThat(queryrecService.recognize(text, null, UZBEKISTAN_REGION_ID), is(ENGLISH));
    }
}
