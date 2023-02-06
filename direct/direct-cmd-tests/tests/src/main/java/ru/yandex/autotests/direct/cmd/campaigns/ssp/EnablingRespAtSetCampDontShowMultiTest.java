package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetCampDontShowMultiResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// TESTIRT-8758
@Aqua.Test
@Description("Ответ ручки при включении показа на площадках и ssp-платформах на странице статистики")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_CAMP_DONT_SHOW_MULTI)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class EnablingRespAtSetCampDontShowMultiTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public List<String> disable;
    @Parameterized.Parameter(1)
    public List<String> expectedResp;
    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    @Parameterized.Parameters(name = "Отправляем: {0}; Получаем: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // ssp
                {Arrays.asList("Rubicon"), Arrays.asList("Rubicon")},
                {Arrays.asList("RuBICon"), Arrays.asList("Rubicon")},
                {Arrays.asList("RuBICon", "Rubicon"), Arrays.asList("Rubicon")},
                {Arrays.asList("Rubicon", "smaato"), Arrays.asList("Rubicon", "Smaato")},

                // площадки
                {Arrays.asList("lostfilm.tv"), Arrays.asList("lostfilm.tv")},
                {Arrays.asList("lostfilm.tv", "lostfilm.tv"), Arrays.asList("lostfilm.tv")},
                {Arrays.asList("lostfilm.tv", "avito.ru"), Arrays.asList("lostfilm.tv", "avito.ru")},

                // ssp + площадки
                {Arrays.asList("Rubicon", "lostfilm.tv"), Arrays.asList("Rubicon", "lostfilm.tv")},
                {Arrays.asList("lostfilm.tv", "Rubicon"), Arrays.asList("lostfilm.tv", "Rubicon")},
                {
                        Arrays.asList("Rubicon", "lostfilm.tv", "smaato", "avito.ru"),
                        Arrays.asList("Rubicon", "lostfilm.tv", "Smaato", "avito.ru")
                },
                {
                        Arrays.asList("Rubicon", "lostfilm.tv", "RuBIcoN", "smaato", "avito.ru", "avito.ru", "smaato"),
                        Arrays.asList("Rubicon", "lostfilm.tv", "Smaato", "avito.ru")
                }
        });
    }

    @Test
    @Description("Ответ ручки при включении показа на площадках и ssp-платформах на странице статистики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9556")
    public void testEnablingAtSetCampDontShowMulti() {
        SetCampDontShowMultiResponse resp = cmdRule.cmdSteps().setCampDontShowMultiSteps().disableShow(
                CLIENT, bannersRule.getCampaignId(), disable);
        assertThat("в ответе ручки setCampDontShowMulti присутствуют отправленные площадки/ssp-платформы",
                resp.getResponse(), containsInAnyOrder(expectedResp.toArray(new String[expectedResp.size()])));
    }
}
