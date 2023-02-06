package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxapplyrejectcorrections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrase;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.CorrectionType.STOPWORD_FIXATED;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-4999
 */
@Aqua.Test
@Description("Проверки контроллера ajaxApplyRejectCorrection для нескольких фраз")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.PHRASE)
@Tag(CmdTag.AJAX_APPLY_REJECT_CORRECTION)
@Tag(CampTypeTag.TEXT)
public class AjaxApplyRejectCorrectionMultiplePhrasesTest extends AjaxApplyRejectCorrectionsTestBase {

    private static ArrayList phrases;
    private AjaxApplyRejectCorrectionsPhrase firstTestPhrase;
    private AjaxApplyRejectCorrectionsPhrase secondTestPhrase;

    @BeforeClass
    public static void beforeClass() {
        phrases = new ArrayList<>();
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("2 как и когда"));
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("все обо всем"));
    }

    public AjaxApplyRejectCorrectionMultiplePhrasesTest() {
        super(phrases);
    }

    @Override
    protected void setPhrasesInRequest() {
        Map<String, Long> ids = StreamEx.of(bannersRule.getCurrentGroup().getPhrases())
                .toMap(Phrase::getPhrase, Phrase::getId);

        firstTestPhrase = loader.getHttpBean("firstPhraseWithStopWordsCorrection");
        firstTestPhrase.setPhraseId(String.valueOf(ids.get(firstTestPhrase.getOriginalPhrase())));
        jsonPhrases.addGroupPhrasesForStopWordsFixation(bannersRule.getGroupId().toString(), firstTestPhrase);

        secondTestPhrase = loader.getHttpBean("secondPhraseWithStopWordsCorrection");
        secondTestPhrase.setPhraseId(String.valueOf(ids.get(secondTestPhrase.getOriginalPhrase())));
        jsonPhrases.addGroupPhrasesForStopWordsFixation(bannersRule.getGroupId().toString(), secondTestPhrase);

        requestParams.setCorrection(STOPWORD_FIXATED.toString());
        requestParams.setIsRejected("0");
    }


    @Test
    @Description("Проверка принятия корректировки стоп слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10242")
    public void ajaxApplyStopWordsCorrectionsTest() {
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        List<String> actualPhrases = getPhrases();

        assertThat("фраза содержит предложенные исправления", actualPhrases,
                containsInAnyOrder(firstTestPhrase.getCorrectedPhrase(), secondTestPhrase.getCorrectedPhrase()));
    }
}
