package ru.yandex.autotests.direct.cmd.phrases.minusphrases;

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
@Description("Ошибки при сохранении минус-фраз")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class MinusPhrasesLimitNegativeTest {
    private static final String CLIENT = "at-backend-minus-phrase-err";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameter(value = 0)
    public String minusKeyphrase;

    @Parameterized.Parameters(name = "Сохраняем минус-фразы: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"рано утром рассвете просыпаются мышата утята котята гризлята"},
                {"рано утром на рассвете просыпаются мышата и утята"},
                {"Санкт-Петербург бесспорно прекрасен в любое время года"},
                {"!Санкт-Петербург прекрасен в любое время дня и ночи"},
                {"Как быстро научиться разговаривать по-русски совсем без акцента"},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Ошибки при сохранении минус-фраз в кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10700")
    public void saveCampInvalidMinusPhrases() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(new ArrayList<>(Arrays.asList(minusKeyphrase)))
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }

    @Test
    @Description("Ошибки при сохранении минус-фраз в кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10701")
    public void ajaxCheckCampMinusPhrases() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(bannersRule.getCampaignId().toString())
                .withJsonMinusWords(Collections.singletonList(minusKeyphrase))
                .withUlogin(CLIENT);
        CommonResponse commonResponse =
                cmdRule.cmdSteps().phrasesSteps().ajaxCheckCampMinusWords(ajaxCheckMinusWordsRequest);
        assertThat("Получили ошибку при сохранении минус-фраз", commonResponse.getOk(), equalTo("0"));
    }

    @Test
    @Description("Ошибки при сохранении минус-фраз в группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10702")
    public void ajaxCheckBannersMinusPhrases() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(bannersRule.getCampaignId().toString())
                .withJsonMinusWords(Collections.singletonList(minusKeyphrase))
                .withUlogin(CLIENT);
        CommonResponse commonResponse =
                cmdRule.cmdSteps().phrasesSteps().ajaxCheckBannersMinusWords(ajaxCheckMinusWordsRequest);
        assertThat("Получили ошибку при сохранении минус-фраз", commonResponse.getOk(), equalTo("0"));
    }
}
