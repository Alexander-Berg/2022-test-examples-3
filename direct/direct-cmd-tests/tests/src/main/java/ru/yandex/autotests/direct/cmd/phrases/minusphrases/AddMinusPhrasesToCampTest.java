package ru.yandex.autotests.direct.cmd.phrases.minusphrases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение минус-фраз в кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AddMinusPhrasesToCampTest extends MinusPhrasesBaseTest {

    @Test
    @Description("Сохранение кампании с минус-фразами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10696")
    public void saveCampaignWithMinusPhrases() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(minusPhraseList)
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(CLIENT);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        List actualMinusPhrasesList = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT, String.valueOf(bannersRule.getCampaignId())).getMinusKeywords();

        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", actualMinusPhrasesList, equalTo(minusPhraseList));
    }
}
