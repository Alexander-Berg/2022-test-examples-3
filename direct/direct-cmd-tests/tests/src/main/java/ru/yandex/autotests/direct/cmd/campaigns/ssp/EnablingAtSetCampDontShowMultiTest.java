package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// TESTIRT-8758
@Aqua.Test
@Description("Разрешение показа на площадках и ssp-платформах на странице статистики")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_CAMP_DONT_SHOW_MULTI)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EnablingAtSetCampDontShowMultiTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;
    private List<String> enable;
    private String[] expectedDontShow;

    public EnablingAtSetCampDontShowMultiTest(CampaignTypeEnum campType, String dontShow,
                                              String[] enable, String[] expectedDontShow) {
        this.enable = Arrays.asList(enable);
        this.expectedDontShow = expectedDontShow;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campType).
                overrideCampTemplate(new SaveCampRequest().withDontShow(dontShow)).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; начальный список: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // 1 ssp
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Rubicon", new String[]{"Rubicon"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Rubicon", new String[]{"Rubicon", "RubiCon"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Rubicon", new String[]{"platform"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"Rubicon"}, new String[]{"Smaato"}},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"Rubicon", "Rubicon"}, new String[]{"Smaato"}},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"RuBiCon"}, new String[]{"Smaato"}},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"RuBiCon", "platform"}, new String[]{"Smaato"}},
                // 2 ssp
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon", "Smaato"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"RuBiCon", "Smaato"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Smaato,Rubicon", new String[]{"RuBiCon", "Smaato", "RuBiCon"}, new String[0]},

                // 1 площадка
                {CampaignTypeEnum.TEXT, null, new String[]{"lostfilm.tv"}, new String[0]},
                {CampaignTypeEnum.TEXT, "lostfilm.tv", new String[]{"lostfilm.tv"}, new String[0]},
                {CampaignTypeEnum.TEXT, "lostfilm.tv", new String[]{"lostfilm.tv", "lostfilm.tv"}, new String[0]},
                {CampaignTypeEnum.TEXT, "lostfilm.tv", new String[]{"domain.ru"}, new String[]{"lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "lostfilm.tv,avito.ru", new String[]{"lostfilm.tv"}, new String[]{"avito.ru"}},
                {CampaignTypeEnum.TEXT, "lostfilm.tv,avito.ru", new String[]{"lostfilm.tv"},
                        new String[]{"avito.ru"}},
                {CampaignTypeEnum.TEXT, "avito.ru,lostfilm.tv", new String[]{"lostfilm.tv", "domain.ru"},
                        new String[]{"avito.ru"}},
                // 2 площадки
                {CampaignTypeEnum.TEXT, null, new String[]{"lostfilm.tv", "avito.ru"}, new String[0]},
                {CampaignTypeEnum.TEXT, "lostfilm.tv,avito.ru", new String[]{"lostfilm.tv", "avito.ru"}, new String[0]},
                {CampaignTypeEnum.TEXT, "lostfilm.tv,avito.ru", new String[]{"lostfilm.tv", "domain.ru", "avito.ru"},
                        new String[0]},

                // ssp + площадки
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon", "lostfilm.tv"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv", "rubicon"}, new String[0]},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"rubicon"}, new String[]{"lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"platform"},
                        new String[]{"Rubicon", "lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"platform", "RuBiCoN"},
                        new String[]{"lostfilm.tv"}},

                {CampaignTypeEnum.MOBILE, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.MOBILE, "Rubicon,lostfilm.tv", new String[]{"rubicon"}, new String[]{"lostfilm.tv"}},

                {CampaignTypeEnum.DTO, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.DTO, "Rubicon,lostfilm.tv", new String[]{"rubicon"}, new String[]{"lostfilm.tv"}}
        });
    }

    @Test
    @Description("Разрешение показа на площадках и ssp-платформах на странице статистики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9555")
    public void testEnablingAtSetCampDontShowMulti() {
        cmdRule.cmdSteps().setCampDontShowMultiSteps().enableShow(CLIENT, bannersRule.getCampaignId(), enable);

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        List<String> actualDontShow = Arrays.asList(campResponse.getCampaign().getDontShow());
        assertThat("запрещенные площадки/ssp-платформы на уровне кампании изменились",
                actualDontShow, containsInAnyOrder(expectedDontShow));
    }
}
