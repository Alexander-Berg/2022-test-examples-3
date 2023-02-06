package ru.yandex.autotests.direct.cmd.phrases.minuswords;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание кампании с минус словами")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class CreateCampWithMinusWordsTest {
    public static final String CLIENT = "at-backend-minuswords";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;

    public String expMinusKeywords;

    public CreateCampWithMinusWordsTest(String minusKeywords, String expMinusKeywords) {
        this.expMinusKeywords = expMinusKeywords;
        bannersRule = new TextBannersRule()
                .withMinusWords(new ArrayList<>(Arrays.asList(minusKeywords.split(" "))))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сохраняем кампанию с минус словами: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"", null},
                {" ", null},
                {"abc", "abc"},
                {"d b c", "b c d"},
        });
    }

    @Test
    @Description("Проверка новой кампании с минус словами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9919")
    public void checkNewCampaignWithMinusKeywords() {
        List actualMinusKeywords = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT, String.valueOf(bannersRule.getCampaignId())).getMinusKeywords();
        List expMinusKeywordsList = expMinusKeywords == null ?
                null : Arrays.asList(expMinusKeywords.split(" "));
        assertThat("Сохраненные минус слова совпадают с ожидаемыми", actualMinusKeywords, equalTo(expMinusKeywordsList));
    }
}
