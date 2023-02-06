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
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
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
@Description("Проверка контроллера AjaxGetBannersCount")
@Stories(TestFeatures.Banners.AJAX_GET_BANNERS_COUNT)
@Features(TestFeatures.BANNERS)
@Tag(OldTag.YES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
public class AjaxGetBannersCountTest {

    private static final String CLIENT_LOGIN = "at-direct-ajax-get";
    private static final String ANOTHER_CLIENT_CID = "123";
    private static final String GROUP_COUNT = "2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    @Test
    @Description("Проверяем ответ при пустом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10057")
    public void emptyCidValidationTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        DirectResponse response = cmdRule.oldSteps().ajaxGetBannersCount().getBannersCountByCid(null);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, equalTo("0"));
    }

    @Test
    @Description("Проверяем число групп под ролью клиент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10058")
    public void groupCountWithClientRoleTest() {
        addGroupToCampaign(campaignId);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        DirectResponse response =
                cmdRule.oldSteps().ajaxGetBannersCount().getBannersCount(String.valueOf(campaignId), CLIENT_LOGIN);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, equalTo(String.valueOf(GROUP_COUNT)));
    }

    @Test
    @Description("Проверяем, что архивные группы также учитываются")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10059")
    public void archiveGroupCountTest() {
        addGroupToCampaign(campaignId);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiAggregationSteps().archiveBanner(CLIENT_LOGIN, bannersRule.getBannerId());
        DirectResponse response =
                cmdRule.oldSteps().ajaxGetBannersCount().getBannersCount(String.valueOf(campaignId), CLIENT_LOGIN);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, equalTo(String.valueOf(GROUP_COUNT)));
    }

    @Test
    @Description("Проверяем валидацию при cid другого клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10060")
    public void anotherClientCidValidationTest() {
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        DirectResponse response = cmdRule.oldSteps().ajaxGetBannersCount().getBannersCountByCid(ANOTHER_CLIENT_CID);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    private void addGroupToCampaign(Long campaignId) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT_LOGIN, campaignId, bannersRule.getGroup()));
    }
}
