package ru.yandex.autotests.direct.cmd.phrases.minusphrases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxCheckMinusWordsRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение минус-фраз в попапе на странице редактирования кампании")
@Stories(TestFeatures.Phrases.AJAX_CHECK_CAMP_MINUS_WORDS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_CHECK_CAMP_MINUS_WORDS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxCheckCampMinusPhrasesTest extends MinusPhrasesBaseTest {

    @Test
    @Description("Валидация минус-фраз на странице кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10699")
    public void ajaxCheckCampMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(bannersRule.getCampaignId().toString())
                .withJsonMinusWords(minusPhraseList)
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().phrasesSteps().ajaxCheckCampMinusWords(ajaxCheckMinusWordsRequest);

        assertThat("Минус-фразы прошли валидацию", response.getOk(), equalTo("1"));
    }
}
