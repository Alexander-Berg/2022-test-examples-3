package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxapplyrejectcorrections;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrase;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.CorrectionType.STOPWORD_FIXATED;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 11.05]6.15.
 * TESTIRT-4999
 */
@Aqua.Test
@Description("Проверка валидации параметров запроса в контроллере ajaxApplyRejectCorrections")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.PHRASE)
@Tag(CmdTag.AJAX_APPLY_REJECT_CORRECTION)
@Tag(CampTypeTag.TEXT)
public class AjaxApplyRejectCorrectionsRequestValidationTest extends AjaxApplyRejectCorrectionsTestBase {

    private AjaxApplyRejectCorrectionsPhrase testPhrase;
    private static ArrayList phrases;

    @BeforeClass
    public static void beforeClass() {
        phrases = new ArrayList<>();
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("2 как и когда"));
    }

    public AjaxApplyRejectCorrectionsRequestValidationTest() {
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
    @Description("Неверный cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10246")
    public void wrongCidTest() {
        requestParams.setCid("123");
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Неверный ulogin")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10245")
    public void wrongUloginTest() {
        final String OTHER_LOGIN = "at-backend-banners";
        requestParams.setUlogin(OTHER_LOGIN);
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Неверный adgroupId")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10248")
    public void wrongAdGroupIdTest() {
        jsonPhrases.setGroupPhrasesForStopWordsFixation("123", testPhrase);
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Проверка, что фразы не изменились при неверном phraseId")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10247")
    public void wrongPhraseIdTest() {
        testPhrase.setPhraseId(String.valueOf("123"));
        jsonPhrases.setGroupPhrasesForStopWordsFixation(String.valueOf(bannersRule.getGroupId()), testPhrase);
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("фраза при проверке через api соответствует ожиданиям",
                actualGroup.getPhrases().get(0).getPhrase(),
                equalTo(testPhrase.getOriginalPhrase()));
    }
}
