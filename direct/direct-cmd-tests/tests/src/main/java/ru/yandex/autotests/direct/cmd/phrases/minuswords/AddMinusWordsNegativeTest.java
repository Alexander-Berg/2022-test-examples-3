package ru.yandex.autotests.direct.cmd.phrases.minuswords;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxCheckMinusWordsRequest;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Ошибки при сохранении минус-слов")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AddMinusWordsNegativeTest {
    private static final String CLIENT = "at-backend-minuswords";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameter(value = 0)
    public String minusKeywords;

    @Parameterized.Parameters(name = "Сохраняем минус-слова: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"@"},
                {"$"},
                {"_"},
                {"+"},
                {"[]"},
                {"!"},
                {"диван/кровать"},
                {"-диван"},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Ошибки при сохранении минус слов в кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10729")
    public void saveCampInvalidMinusWords() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(new ArrayList<>(Arrays.asList(minusKeywords)))
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }

    @Test
    @Description("Ошибки при сохранении минус слов в кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10727")
    public void ajaxCheckCampMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(bannersRule.getCampaignId().toString())
                .withJsonMinusWords(Collections.singletonList(minusKeywords))
                .withUlogin(CLIENT);
        CommonResponse commonResponse =
                cmdRule.cmdSteps().phrasesSteps().ajaxCheckCampMinusWords(ajaxCheckMinusWordsRequest);
        assertThat("Получили ошибку при сохранении минус-слов", commonResponse.getOk(), equalTo("0"));
    }

    @Test
    @Description("Ошибки при сохранении минус слов в группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10728")
    public void ajaxCheckBannersMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(bannersRule.getCampaignId().toString())
                .withJsonMinusWords(Collections.singletonList(minusKeywords))
                .withUlogin(CLIENT);
        CommonResponse commonResponse =
                cmdRule.cmdSteps().phrasesSteps().ajaxCheckBannersMinusWords(ajaxCheckMinusWordsRequest);
        assertThat("Получили ошибку при сохранении минус-слов", commonResponse.getOk(), equalTo("0"));
    }
}
