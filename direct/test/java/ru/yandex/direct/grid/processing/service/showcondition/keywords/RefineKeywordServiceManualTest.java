package ru.yandex.direct.grid.processing.service.showcondition.keywords;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdRefineKeyword;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdRefineKeywordPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.RefinedWord;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Ignore
@ContextConfiguration(classes = GridProcessingConfiguration.class)
@RunWith(Parameterized.class)
public class RefineKeywordServiceManualTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private RefineKeywordService refineKeywordService;

    @Parameterized.Parameter
    public String keyword;
    @Parameterized.Parameter(1)
    public List<String> minusWords;
    @Parameterized.Parameter(2)
    public List<String> refinedWordsByPerl;

    @Parameterized.Parameters(name = "{0} && -{1} => {2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        "красное платье",
                        Collections.singletonList("красное"),
                        Arrays.asList(
                                "маленький",
                                "фильм",
                                "2018",
                                "фото",
                                "девушка",
                                "цвет",
                                "черно",
                                "белый",
                                "какой",
                                "под")
                },
                {
                        "красное платье",
                        Collections.singletonList("маленькое"),
                        Arrays.asList(
                                "фильм",
                                "2018",
                                "фото",
                                "девушка",
                                "цвет",
                                "черно",
                                "белый",
                                "какой",
                                "под",
                                "купить")
                },
                {
                        "красное платье",
                        Arrays.asList("!красное", "красное яблоко"),
                        Arrays.asList(
                                "маленькая",
                                "фильм",
                                "2018",
                                "фото",
                                "девушка",
                                "цвет",
                                "черный",
                                "белый",
                                "под",
                                "купить")
                },
                {
                        "пена морская",
                        Arrays.asList("мыльная пена", "\"котик\""),
                        Arrays.asList("цвет",
                                "афродита",
                                "маска",
                                "богиня",
                                "пузырьковый",
                                "8",
                                "фото",
                                "флокс",
                                "какой",
                                "песня")
                },
                {
                        "пена морская",
                        Arrays.asList("[мыльная пена]", "\"котик\""),
                        Arrays.asList("цвет",
                                "афродита",
                                "маска",
                                "богиня",
                                "пузырьковый",
                                "8",
                                "фото",
                                "флокс",
                                "какой",
                                "песня")
                },
                {
                        "слово минус +на",
                        Collections.singletonList("плюс"),
                        Arrays.asList(
                                "песнь",
                                "со",
                                "без",
                                "песня",
                                "свет",
                                "мама",
                                "родной",
                                "свадьба",
                                "группа",
                                "кампания")
                },
                {
                        "билеты [Москва Париж]",
                        Collections.emptyList(),
                        Arrays.asList(
                                "цена",
                                "самолет",
                                "купить",
                                "поезд",
                                "стоять",
                                "сколько",
                                "дешевый",
                                "прямая",
                                "обратно",
                                "дешево")
                }
        });
    }

    /**
     * Для ручного запуска.
     * Перед запуском необходимо закомментировать мокирование бина advqClient в CoreTestingConfiguration,
     * так как тест должен отправлять запрос в реальный Advq.
     */
    @Test
    public void testRefinedWords() {
        GdRefineKeyword request = new GdRefineKeyword()
                .withGeo(Collections.singletonList(Region.RUSSIA_REGION_ID))
                .withKeyword(keyword)
                .withMinusWords(minusWords);
        GdRefineKeywordPayload keywordPayload = refineKeywordService.refine(request);
        List<String> refinedWords = mapList(keywordPayload.getWords(), RefinedWord::getWord);
        assertThat(refinedWords).isEqualTo(refinedWordsByPerl);
    }
}

