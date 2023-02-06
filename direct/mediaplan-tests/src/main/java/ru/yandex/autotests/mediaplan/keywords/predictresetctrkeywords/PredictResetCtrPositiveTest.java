package ru.yandex.autotests.mediaplan.keywords.predictresetctrkeywords;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_predict_reset_ctr_keywords.Api5PredictResetCtrKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_predict_reset_ctr_keywords.ParamsApi5PredictResetCtrKeywords;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_predict_reset_ctr_keywords.PredictResetCTR;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_FORECAST;
import static ru.yandex.autotests.mediaplan.datafactories.PredictResetCtrFactory.*;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_FORECAST)
@Description("Предсказания для ключевых слов для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class PredictResetCtrPositiveTest {
    @Parameterized.Parameter(value = 1)
    public List<PredictResetCTR> predictResetCTRs;
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одно ключевое слово", onePredict()},
                {"Два ключевых слова", twoPredict()},
                {"Тысяча ключевых слова", thousandPredict()},
        });
    }

    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule();

    @Test
    public void predict() {
        Api5PredictResetCtrKeywordsResult predicts = adgroupRule.getUserSteps().keywordsSteps().api5KeywordsPredictResetCtrKeywords(
                new ParamsApi5PredictResetCtrKeywords().withPredictResetCTR(predictResetCTRs));
        assertThat("число предсказаний соотвестует ожиданиям", predicts.getPredictResetCTRResults(), hasSize(predictResetCTRs.size()));
    }
}
