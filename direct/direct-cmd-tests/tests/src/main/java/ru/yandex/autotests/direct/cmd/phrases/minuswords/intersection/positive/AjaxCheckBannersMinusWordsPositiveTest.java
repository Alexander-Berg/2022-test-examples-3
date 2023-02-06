package ru.yandex.autotests.direct.cmd.phrases.minuswords.intersection.positive;

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
@Description("Пересечения ключевых слов и минус слов в попапе на странице редактирования группы")
@Stories(TestFeatures.Phrases.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AjaxCheckBannersMinusWordsPositiveTest extends AjaxCheckMinusWordsPositiveBase {

    @Test
    @Description("Пересечение ключевых слов и минус слов на странице группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10737")
    public void ajaxCheckBannersMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(campaignId.toString())
                .withJsonKeyWords(Collections.singletonList(keyPhraseSrt))
                .withJsonMinusWords(Collections.singletonList(minusWordsSrt))
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().phrasesSteps().ajaxCheckBannersMinusWords(ajaxCheckMinusWordsRequest);

        assertThat("Минус слова прошли валидацию", response.getOk(), equalTo("1"));
    }
}
