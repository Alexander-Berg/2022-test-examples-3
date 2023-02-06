package ru.yandex.autotests.direct.httpclient.banners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 *         TESTIRT-5010
 */

@Aqua.Test
@Description("Проверка контроллера AjaxGetBannersCount под менеджером")
@Stories(TestFeatures.Banners.AJAX_GET_BANNERS_COUNT)
@Features(TestFeatures.BANNERS)
@Tag(OldTag.YES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
public class AjaxGetBannersCountManagerTest {

    private static final String CLIENT_LOGIN = "at-direct-ajax-get";
    private static final String SERVICED_CLIENT = "at-direct-searchbanners1";
    private static final String GROUP_COUNT = "2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule()
            .withUlogin(SERVICED_CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        addGroupToCampaign(campaignId);
    }

    @Test
    @Description("Проверяем валидацию для чужого клиента под ролью менеджер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10820")
    public void anotherClientWithManagerRoleValidationTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        DirectResponse response =
                cmdRule.oldSteps().ajaxGetBannersCount().getBannersCount(String.valueOf(campaignId), CLIENT_LOGIN);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Проверяем число групп под ролью менеджер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10821")
    public void groupCountWithManagerRoleTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        DirectResponse response =
                cmdRule.oldSteps().ajaxGetBannersCount().getBannersCount(String.valueOf(campaignId), SERVICED_CLIENT);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, equalTo(String.valueOf(GROUP_COUNT)));
    }

    private void addGroupToCampaign(Long campaignId) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(SERVICED_CLIENT, campaignId, bannersRule.getGroup()));
    }
}
