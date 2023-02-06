package ru.yandex.autotests.direct.cmd.phrases.minuswords;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Добавление минус-слов в кампанию")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AddCampMinusWordsTest {

    public static final String CLIENT = "at-backend-minuswords-camp";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    @Parameterized.Parameter(value = 0)
    public String minusKeywordsStr;
    @Parameterized.Parameter(value = 1)
    public String expMinusKeywordsStr;

    @Parameterized.Parameters(name = "Добавляем в кампанию минус слова {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {" ", null},
                {"стол-стул", "стол-стул"},
                {"[конь]", "[конь]"},
                {"[для]", "[для]"},
                {"!words +minus new", "!words +minus new"},
        });
    }

    @Test
    @Description("Меняем минус слова на кампанию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9918")
    public void changeCampMinusKeywords() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(Arrays.asList(minusKeywordsStr.split(" ")))
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        List actualMinusKeywords = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT, String.valueOf(bannersRule.getCampaignId())).getMinusKeywords();

        List expMinusKeywords = expMinusKeywordsStr == null ?
                null : Arrays.asList(expMinusKeywordsStr.split(" "));
        assertThat("Сохраненные минус слова совпадают с ожидаемыми", actualMinusKeywords, equalTo(expMinusKeywords));
    }
}
