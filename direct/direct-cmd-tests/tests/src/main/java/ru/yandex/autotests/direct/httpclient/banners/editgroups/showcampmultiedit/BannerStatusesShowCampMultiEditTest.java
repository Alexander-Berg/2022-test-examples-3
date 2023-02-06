package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.BannerCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.groups.EditGroupsErrors;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.ACTIVE;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.ARCHIVE;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.DECLINED;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.DRAFT;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.STOPPED;
import static ru.yandex.autotests.direct.httpclient.data.banners.BannerStatusEnum.WAIT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Проверка ответа showCampMultiEdit для разных статусов баннеров")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class BannerStatusesShowCampMultiEditTest {

    private static final String CLIENT = "at-direct-b-showcampmultiedit";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static CSRFToken csrfToken;

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;
    private DirectResponse response;
    private GroupsParameters requestParams;
    private Long[] bannerIds;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        bannersRule.saveGroup(
                ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters.forExistingCamp(CLIENT, campaignId,
                        BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_WITH_TWO_BANNERS, Group.class)));

        List<Group> groups = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignId);
        bannerIds = groups.stream().flatMap(group -> group.getBanners().stream()).map(Banner::getBid).toArray(Long[]::new);
        Long firstAdGroupId = Long.valueOf(groups.get(0).getAdGroupID());
        Long secondAdGroupId = Long.valueOf(groups.get(1).getAdGroupID());

        requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setAdgroupIds(StringUtils.join(new Long[]{firstAdGroupId, secondAdGroupId}, ","));
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для архивной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10139")
    public void archivedCampaignErrorTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignStopped(campaignId);
        cmdRule.apiAggregationSteps().campaignsArchive(CLIENT, campaignId);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(TextResourceFormatter.resource(EditGroupsErrors.ARCHIVED_CAMPAIGN).args(campaignId).toString()));
    }

    @Test
    @Description("Отсутствие в ответе архивного баннера при запросе всех объявлений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10140")
    public void archivedBannerAbsenceTest() {
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerIds[1]);
        cmdRule.apiAggregationSteps().archiveBanner(CLIENT, bannerIds[1]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[0]), String.valueOf(bannerIds[2]));
    }

    @Test
    @Description("Присутствие в ответе только архивного объявления при запросе архивных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10141")
    public void archivedBannerTest() {
        requestParams.setBannerStatus(ARCHIVE.toString());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerIds[1]);
        cmdRule.apiAggregationSteps().archiveBanner(CLIENT, bannerIds[1]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(EditGroupsErrors.BANNERS_UNAVAILABLE_FOR_EDIT.toString()));
    }

    @Test
    @Description("Присутствие в ответе только черновика объявления при запросе черновиков баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10142")
    public void draftBannerStatusTest() {
        requestParams.setBannerStatus(DRAFT.toString());
        cmdRule.apiSteps().makeBannerActive(bannerIds[0]);
        cmdRule.apiSteps().makeBannerActive(bannerIds[2]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[1]));
    }

    @Test
    @Description("Присутствие в ответе только активного объявления при запросе активных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10143")
    public void activeBannerStatusTest() {
        requestParams.setBannerStatus(ACTIVE.toString());
        cmdRule.apiSteps().makeBannerActive(bannerIds[1]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[1]));
    }

    @Test
    @Description("Присутствие в ответе только остановленого объявления при запросе остановленных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10144")
    public void stoppedBannerStatusTest() {
        requestParams.setBannerStatus(STOPPED.toString());
        cmdRule.apiSteps().bannersFakeSteps().makeBannersStopped(bannerIds[1]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[1]));
    }

    @Test
    @Description("Присутствие в ответе только отклоненного объявления при запросе отклоненных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10145")
    public void declinedBannerStatusTest() {
        requestParams.setBannerStatus(DECLINED.toString());
        cmdRule.apiSteps().bannersFakeSteps().makeBannersDeclined(bannerIds[1]);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[1]));
    }

    @Test
    @Description("Присутствие в ответе только промодерированного объявления при запросе промодерированных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10146")
    public void moderatedBannerStatusTest() {
        requestParams.setBannerStatus(WAIT.toString());
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(campaignId);
        cmdRule.apiAggregationSteps().moderateBanner(CLIENT, bannerIds[0]);
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerIds[0]);

        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkBannerIdsInResponse(response, String.valueOf(bannerIds[0]));
    }

    public void checkBannerIdsInResponse(DirectResponse response, String... bannerIds) {
        GroupsCmdBean actualResponse = JsonPathJSONPopulater.eval(response.getResponseContent().asString(), new GroupsCmdBean(), BeanType.RESPONSE);
        List<String> actualBids = new ArrayList<>();
        for (GroupCmdBean group : actualResponse.getGroups()) {
            actualBids.addAll(extract(group.getBanners(), on(BannerCmdBean.class).getBannerID()));
        }
        assertThat("в ответе контроллера присутствуют только неархивные баннеры", actualBids, contains(bannerIds));
    }
}
