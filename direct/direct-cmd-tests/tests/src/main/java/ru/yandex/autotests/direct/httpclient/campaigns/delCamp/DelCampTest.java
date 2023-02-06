package ru.yandex.autotests.direct.httpclient.campaigns.delCamp;

import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 *         testirt-5017
 */

@Aqua.Test
@Description("Удаление кампании контроллером delCamp")
@Stories(TestFeatures.Campaigns.DEL_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.DEL_CAMP)
@Tag(OldTag.YES)
public class DelCampTest {

    private static final String CLIENT = "at-direct-backend-c4";
    private static final String SECOND_CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Удаляем кампанию под клиентом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10322")
    public void deleteCampaignTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());

        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().delCampSteps().deleteCampaign(csrfToken, String.valueOf(bannersRule.getCampaignId()));

        CampaignGetItem campaign = cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT).stream()
                .filter(c -> bannersRule.getCampaignId().equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertThat("Кампания удалена", campaign, nullValue());
    }

    @Test
    @Description("Пытаемся удалить чужую кампанию под клиентом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10323")
    public void deleteOtherClientCampaignValidationTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(SECOND_CLIENT).getPassportUID());
        cmdRule.cmdSteps().authSteps().authenticate(User.get(SECOND_CLIENT));

        DirectResponse response = cmdRule.oldSteps().delCampSteps()
                .deleteCampaign(csrfToken, String.valueOf(bannersRule.getCampaignId()));
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Пытаемся удалить кампанию чужого клиента под менеджером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10326")
    public void deleteOtherClientCampaignWithManagerRoleTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.MANAGER).getPassportUID());
        DirectResponse response =
                cmdRule.oldSteps().delCampSteps()
                        .deleteCampaign(csrfToken, String.valueOf(bannersRule.getCampaignId()), CLIENT);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
