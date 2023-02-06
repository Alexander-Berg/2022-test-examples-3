package ru.yandex.autotests.direct.cmd.campaigns.ssp;

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
@Description("Сохранение запрещенных площадок и ssp-платформ при редактировании кампании под разными ролями")
@Stories(TestFeatures.Campaigns.DISABLED_SSP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DontShowParamAtSaveNewCampRolesTest {

    private static final String DONT_SHOW = "Smaato";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;
    private String client;

    @SuppressWarnings("unused")
    public DontShowParamAtSaveNewCampRolesTest(String role,
                                               String authLogin,
                                               String client) {
        this.client = client;
        bannersRule = new TextBannersRule().withUlogin(client);
        cmdRule = DirectCmdRule.defaultRule().as(authLogin).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Роль: {0}; Логин: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"клиент", Logins.DEFAULT_CLIENT, Logins.DEFAULT_CLIENT},
                {"менеджер", Logins.MANAGER, "at-direct-b-firstaid-mngr-c"},
                {"супер", Logins.SUPER, Logins.DEFAULT_CLIENT}
        });
    }

    @Test
    @Description("Сохранение запрещенных площадок и ssp-платформ при редактировании кампании под разными ролями")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9552")
    public void testDontShowParamAtSaveNewCampForRoles() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(getSaveCampRequest());

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), client);
        List<String> actualDontShow = Arrays.asList(campResponse.getCampaign().getDontShow());
        assertThat("запрещенные площадки/ssp-платформы на уровне кампании сохранились",
                actualDontShow, containsInAnyOrder(DONT_SHOW));
    }

    private SaveCampRequest getSaveCampRequest() {
        return bannersRule.getSaveCampRequest().
                withCid(bannersRule.getCampaignId().toString()).
                withDontShow(DONT_SHOW);
    }
}
