package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxapplyrejectcorrections;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrase;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.CorrectionType.STOPWORD_FIXATED;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-4999
 */
@Aqua.Test
@Description("Проверки контроллера ajaxApplyRejectCorrection для одной фразы")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxApplyRejectCorrectionSinglePhraseTest extends AjaxApplyRejectCorrectionsTestBase {

    private AjaxApplyRejectCorrectionsPhrase testPhrase;
    private static ArrayList phrases;

    @BeforeClass
    public static void beforeClass() {
        phrases = new ArrayList<>();
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("2 как и когда"));
    }

    public AjaxApplyRejectCorrectionSinglePhraseTest() {
        super(phrases);
    }

    @Override
    protected void setPhrasesInRequest() {
        testPhrase = loader.getHttpBean("firstPhraseWithStopWordsCorrection");
        testPhrase.setPhraseId(String.valueOf(bannersRule.getCurrentGroup().getPhrases().get(0).getId()));
        jsonPhrases.addGroupPhrasesForStopWordsFixation(bannersRule.getGroupId().toString(), testPhrase);

        requestParams.setCorrection(STOPWORD_FIXATED.toString());
        requestParams.setIsRejected("0");
    }

    @Test
    @Description("Проверка принятия корректировки стоп слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10243")
    public void ajaxApplyStopWordsCorrectionsTest() {
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        List<String> actualPhrases = getPhrases();

        assertThat("фраза содержит предложенные исправления", actualPhrases.get(0),
                equalTo(testPhrase.getCorrectedPhrase()));
    }

    @Test
    @Description("Проверка отклонения корректировки стоп слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10244")
    public void ajaxRejectStopWordsCorrectionsTest() {
        requestParams.setIsRejected("1");
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        List<String> actualPhrases = getPhrases();
        assertThat("фраза не изменилась", actualPhrases.get(0), equalTo(testPhrase.getOriginalPhrase()));
    }
}
