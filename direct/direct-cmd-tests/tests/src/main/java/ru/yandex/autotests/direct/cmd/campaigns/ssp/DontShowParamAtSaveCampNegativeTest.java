package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

// TESTIRT-8758
@Aqua.Test
@Description("Валидация запрещенных площадок и ssp-платформ при редактировании кампании")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class DontShowParamAtSaveCampNegativeTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter
    public String dontShow;
    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    @Parameterized.Parameters(name = "Запрещенные: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                /*невалидные данные*/
                {"test..."},
                {"*.pskovonline.ru"},
                {"%bing.com%"},

                /*запрещенные прощадки*/
                {"platform"},
                {"Rubicon,platform"},
                {"lostfilm.tv,platform"},
                {"lostfilm.tv,Rubicon,platform"},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Валидация запрещенных площадок и ssp-платформ при редактировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9550")
    public void testDontShowParamAtSaveCampNegative() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(getSaveCampRequest());
    }

    private SaveCampRequest getSaveCampRequest() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.setCid(bannersRule.getCampaignId().toString());
        request.setDontShow(dontShow);
        return request;
    }
}
