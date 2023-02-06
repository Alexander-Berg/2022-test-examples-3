package ru.yandex.autotests.mediaplan.keywords.forecast;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_forecast_keywords.Api5ForecastKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_forecast_keywords.ParamsApi5ForecastKeywords;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_FORECAST;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.*;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_FORECAST)
@Description("Предсказания для ключевых слов для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class ForecastKeywordPositiveTest {
    public List<Keyword> keywords;

    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одно ключевое слово", oneKeyWord()},
                {"Два ключевых слова", twoKeyWords()},
                {"200 ключевых слова", maxKeyWords()},
                {"10000 ключевых слова", tenThousandsKeyWords()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    public ForecastKeywordPositiveTest(String text, List<Keyword> keywords) {
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withKeywords(keywords);
        this.keywords = keywords;
    }

    @Test
    public void forecastKeyword() {
        keywords.stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        keywords = IntStream.range(0, keywords.size()).mapToObj(x -> keywords.get(x).withKeyword(null)
                .withKeywordId(adgroupRule.getKeywordsIds().get(x))).collect(Collectors.toList());
        Api5ForecastKeywordsResult ids = adgroupRule.getUserSteps().keywordsSteps()
                .api5KeywordsForecast(new ParamsApi5ForecastKeywords().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient()).withTimestamp(adgroupRule.getLastUpdateTimestamp()).withKeywords(keywords));
        assertThat("число прогнозов соотвествует ожиданиям", ids.getForecastResults(), hasSize(keywords.size()));
    }

}
