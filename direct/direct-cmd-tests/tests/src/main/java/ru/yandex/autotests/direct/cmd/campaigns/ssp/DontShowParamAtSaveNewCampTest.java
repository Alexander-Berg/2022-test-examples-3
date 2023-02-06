package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
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
@Description("Сохранение запрещенных площадок и ssp-платформ при создании кампании")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DontShowParamAtSaveNewCampTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;
    private String[] expectedDontShow;

    public DontShowParamAtSaveNewCampTest(CampaignTypeEnum campType, String dontShow, String[] expectedDontShow) {
        this.expectedDontShow = expectedDontShow;
        bannersRule = BannersRuleFactory.
                getBannersRuleBuilderByCampType(campType).
                overrideCampTemplate(new SaveCampRequest().withDontShow(dontShow)).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; запрещенные: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // 1 ssp
                {CampaignTypeEnum.TEXT, "Rubicon", new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "RuBiCon", new String[]{"Rubicon"}},
                // 2 ssp
                {CampaignTypeEnum.TEXT, "Rubicon,Smaato", new String[]{"Rubicon", "Smaato"}},
                {CampaignTypeEnum.TEXT, "RubicoN,smaAto", new String[]{"Rubicon", "Smaato"}},
                // дубликаты ssp
                {CampaignTypeEnum.TEXT, "Rubicon,Rubicon", new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Rubicon,RubicoN", new String[]{"Rubicon"}},
                {CampaignTypeEnum.TEXT, "Rubicon,Smaato,RubicoN", new String[]{"Rubicon", "Smaato"}},
                // 1 площадка
                {CampaignTypeEnum.TEXT, "lostfilm.tv", new String[]{"lostfilm.tv"}},
                // 2 площадки
                {CampaignTypeEnum.TEXT, "lostfilm.tv,avito.ru", new String[]{"lostfilm.tv", "avito.ru"}},
                // дубликаты площадки
                {CampaignTypeEnum.TEXT, "lostfilm.tv,lostfilm.tv", new String[]{"lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "lostfilm.tv,lostfilm.tv,avito.ru", new String[]{"lostfilm.tv", "avito.ru"}},
                // дубликаты ssp + площадки
                {CampaignTypeEnum.TEXT, "Rubicon,rubicon,lostfilm.tv,lostfilm.tv", new String[]{"Rubicon", "lostfilm.tv"}},
                {CampaignTypeEnum.TEXT, "Rubicon,smaato,lostfilm.tv,rubicon,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                // пустое значение
                {CampaignTypeEnum.TEXT, "", new String[0]},
                {CampaignTypeEnum.TEXT, null, new String[0]},

                {CampaignTypeEnum.MOBILE, "Rubicon,smaato,lostfilm.tv,rubicon,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                {CampaignTypeEnum.MOBILE, "", new String[0]},
                {CampaignTypeEnum.MOBILE, null, new String[0]},

                {CampaignTypeEnum.DTO, "Rubicon,smaato,lostfilm.tv,rubicon,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                {CampaignTypeEnum.DTO, "", new String[0]},
                {CampaignTypeEnum.DTO, null, new String[0]}
        });
    }

    @Test
    @Description("Сохранение запрещенных площадок и ssp-платформ при создании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9553")
    public void testDontShowParamAtSaveNewCamp() {
        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        List<String> actualDontShow = Arrays.asList(campResponse.getCampaign().getDontShow());
        String expectedDontShowStr = new Gson().toJson(expectedDontShow);
        assertThat(String.format("запрещенные площадки/ssp-платформы на уровне кампании сохранились (%s)", expectedDontShowStr),
                actualDontShow, containsInAnyOrder(expectedDontShow));
    }
}
