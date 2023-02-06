package ru.yandex.autotests.direct.cmd.phrases.minusphrases.intersection.positive;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxCheckMinusWordsRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Пересечения ключевых слов и минус-фраз в попапе на странице редактирования кампании")
@Stories(TestFeatures.Phrases.AJAX_CHECK_CAMP_MINUS_WORDS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_CHECK_CAMP_MINUS_WORDS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AjaxCheckCampMinusPhrasesPositiveTest extends AjaxCheckMinusPhrasesPositiveBase {

    @Test
    @Description("Пересечение ключевых слов и минус-фразы на странице кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10722")
    public void ajaxCheckCampMinusPhrases() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(campaignId.toString())
                .withJsonMinusWords(Collections.singletonList(minusPhraseSrt))
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().phrasesSteps().ajaxCheckCampMinusWords(ajaxCheckMinusWordsRequest);

        assertThat("Минус-фразы прошли валидацию", response.getOk(), equalTo("1"));
    }
}
