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
@Description("Запрет показа на площадках и ssp-платформах на странице статистики")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_CAMP_DONT_SHOW_MULTI)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DisablingAtSetCampDontShowMultiTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public String dontShow;
    public List<String> disable;
    public String[] expectedDontShow;
    private BannersRule bannersRule;

    public DisablingAtSetCampDontShowMultiTest(CampaignTypeEnum campType, String dontShow,
                                               String[] disable, String[] expectedDontShow) {
        this.dontShow = dontShow;
        this.disable = Arrays.asList(disable);
        this.expectedDontShow = expectedDontShow;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campType).
                overrideCampTemplate(new SaveCampRequest().withDontShow(dontShow)).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; начальный список: {1}, список для ручки: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // 1 ssp
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Smaato", new String[]{"RuBiCon"}, new String[]{"Smaato", "Rubicon"}},
                // 2 ssp
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon", "Smaato"}, new String[]{"Rubicon", "Smaato"}},
                // дубликаты ssp
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon", "Rubicon"}, new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Smaato", new String[]{"Rubicon", "Rubicon"}, new String[]{"Smaato", "Rubicon"}},
                {CampaignTypeEnum.TEXT, "Smaato", new String[]{"Smaato"}, new String[]{"Smaato"}},
                {CampaignTypeEnum.TEXT, "Smaato", new String[]{"Rubicon", "Smaato", "RubicoN"},
                        new String[]{"Rubicon", "Smaato"}},

                // 1 площадка
                {CampaignTypeEnum.TEXT, null, new String[]{"lostfilm.tv"}, new String[]{"lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "avito.ru", new String[]{"lostfilm.tv"},
                        new String[]{"avito.ru", "lostfilm.tv"}},
                // 2 площадки
                {CampaignTypeEnum.TEXT, null, new String[]{"lostfilm.tv", "avito.ru"},
                        new String[]{"lostfilm.tv", "avito.ru"}},
                // дубликаты площадки
                {CampaignTypeEnum.TEXT, "avito.ru", new String[]{"lostfilm.tv", "lostfilm.tv"},
                        new String[]{"avito.ru", "lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, null, new String[]{"lostfilm.tv", "avito.ru", "lostfilm.tv"},
                        new String[]{"lostfilm.tv", "avito.ru"}},

                // дубликаты ssp + площадки
                {CampaignTypeEnum.TEXT, null, new String[]{"Rubicon", "rubicon", "lostfilm.tv", "lostfilm.tv"},
                        new String[]{"Rubicon", "lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv", "rubicon", "Smaato", "avito.ru"},
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},


                {CampaignTypeEnum.MOBILE, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv", "rubicon", "Smaato", "avito.ru"},
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},


                {CampaignTypeEnum.DTO, "Rubicon,lostfilm.tv", new String[]{"lostfilm.tv", "rubicon", "Smaato", "avito.ru"},
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}}
        });
    }

    @Test
    @Description("Запрет показа на площадках и ssp-платформах на странице статистики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9547")
    public void testDisablingAtSetCampDontShowMulti() {
        cmdRule.cmdSteps().setCampDontShowMultiSteps().disableShow(
                CLIENT, bannersRule.getCampaignId(), disable);

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        List<String> actualDontShow = Arrays.asList(campResponse.getCampaign().getDontShow());
        assertThat("запрещенные площадки/ssp-платформы на уровне кампании изменились",
                actualDontShow, containsInAnyOrder(expectedDontShow));
    }
}
