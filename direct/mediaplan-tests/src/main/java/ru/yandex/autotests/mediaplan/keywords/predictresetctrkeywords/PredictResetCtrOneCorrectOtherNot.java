package ru.yandex.autotests.mediaplan.keywords.predictresetctrkeywords;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_predict_reset_ctr_keywords.Api5PredictResetCtrKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_predict_reset_ctr_keywords.ParamsApi5PredictResetCtrKeywords;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_FORECAST;
import static ru.yandex.autotests.mediaplan.datafactories.PredictResetCtrFactory.OneCorrectOtherNot;
@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_FORECAST)
@Description("Предсказания для одного корректного ключевого слова и одного некрректного для медиаплана")
@Tag(MasterTags.MASTER)
public class PredictResetCtrOneCorrectOtherNot {
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule();

    @Test
    public void predict() {
        Api5PredictResetCtrKeywordsResult predicts = adgroupRule.getUserSteps().keywordsSteps().api5KeywordsPredictResetCtrKeywords(
                new ParamsApi5PredictResetCtrKeywords().withPredictResetCTR(OneCorrectOtherNot()));
        assertThat("число предсказаний соотвестует ожиданиям", predicts.getPredictResetCTRResults(), hasSize(OneCorrectOtherNot().size()));
    }
}
