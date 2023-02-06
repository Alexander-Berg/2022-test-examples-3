package ru.yandex.autotests.direct.cmd.campaigns.ssp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import org.junit.After;
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
import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

// TESTIRT-8758
@Aqua.Test
@Description("Копирование запрещенных площадок/ssp-платформ при копировании кампании")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DontShowParamAtCopyCampTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT2;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;

    private String[] expectedDontShow;

    private Long newCid;

    public DontShowParamAtCopyCampTest(CampaignTypeEnum campType, String dontShow, String[] expectedDontShow) {
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
                {CampaignTypeEnum.TEXT, "Rubicon,smaato,lostfilm.tv,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                // пустое значение
                {CampaignTypeEnum.TEXT, "", new String[0]},
                {CampaignTypeEnum.TEXT, null, new String[0]},

                {CampaignTypeEnum.MOBILE, "Rubicon,smaato,lostfilm.tv,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                {CampaignTypeEnum.MOBILE, "", new String[0]},
                {CampaignTypeEnum.MOBILE, null, new String[0]},

                {CampaignTypeEnum.DTO, "Rubicon,smaato,lostfilm.tv,lostfilm.tv,avito.ru",
                        new String[]{"Rubicon", "Smaato", "lostfilm.tv", "avito.ru"}},
                {CampaignTypeEnum.DTO, "", new String[0]},
                {CampaignTypeEnum.DTO, null, new String[0]}
        });
    }

    @After
    public void shutDown() {
        if (newCid != null) {
            deleteAdGroupMobileContent(newCid, CLIENT);
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }
    }

    @Test
    @Description("Копирование запрещенных площадок/ssp-платформ при копировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9549")
    public void testDontShowParamAtCopyCamp() {
        String expectedDontShowStr = new Gson().toJson(expectedDontShow);

        List<String> dontShowAfterSave = getDontShowFromCamp(bannersRule.getCampaignId());
        assumeThat(String.format("запрещенные площадки/ssp-платформы при создании кампании сохранились (%s)", expectedDontShowStr),
                dontShowAfterSave, containsInAnyOrder(expectedDontShow));

        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());

        List<String> dontShowAfterCopying = getDontShowFromCamp(newCid);
        assertThat(String.format("запрещенные площадки/ssp-платформы при копировании кампании сохранились (%s)", expectedDontShowStr),
                dontShowAfterCopying, containsInAnyOrder(expectedDontShow));
    }

    private List<String> getDontShowFromCamp(Long campaignId) {
        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(campaignId, CLIENT);
        return Arrays.asList(campResponse.getCampaign().getDontShow());
    }
}
