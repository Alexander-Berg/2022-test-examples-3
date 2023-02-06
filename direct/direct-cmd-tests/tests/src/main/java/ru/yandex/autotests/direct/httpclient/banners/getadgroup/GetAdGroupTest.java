package ru.yandex.autotests.direct.httpclient.banners.getadgroup;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PhrasesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupRequestParametersBuilder;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper.map;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.03.15
 *         TESTIRT-4094
 */


@Aqua.Test
@Description("Проверка ответа контроллера getAdGroup")
@Stories(TestFeatures.Banners.GET_AD_GROUP)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.GROUP)
@Tag(CmdTag.GET_AD_GROUP)
@Tag(CampTypeTag.TEXT)
public class GetAdGroupTest {

    private static final String CLIENT = "at-direct-b-getadgroup-c";
    private static final int BANNER_COUNT = 2;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    private GetAdGroupRequestParameters getAdGroupRequestParameters;
    private GroupFakeInfo groupFakeInfoExpected;
    private Long[] bannerIds;

    @Before
    public void before() {
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());

        Long groupId = createGroupWithManyBanners(bannersRule.getCampaignId(), BANNER_COUNT);
        bannerIds = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule.getCampaignId(), groupId).stream()
                .map(Banner::getBid)
                .toArray(Long[]::new);
        groupFakeInfoExpected = cmdRule.apiSteps().groupFakeSteps().getGroupParams(groupId);
        groupFakeInfoExpected.setGeo(null);
        getAdGroupRequestParameters = new GetAdGroupRequestParametersBuilder().
                setAdGroupId(String.valueOf(groupFakeInfoExpected.getPid())).createGetAdGroupRequestParameters();

    }

    @Test
    @Description("Проверка ответа контроллера getAdGroup без параметра adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10201")
    public void checkGetAdGroupResponseWithoutAdGroupId() {
        getAdGroupRequestParameters.setAdGroupId(null);
        DirectResponse directResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroupDirectResponse(getAdGroupRequestParameters);
        assertThat("Ответ контроллера getAdGroup не совпадает с данными в api",
                directResponse.getResponseContent().asString(),
                containsString("Ошибка: не задан номер группы"));
    }

    @Test(expected = BackEndClientException.class)
    @Description("Проверка ответа контроллера getAdGroup с неверным параметром adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10203")
    public void checkGetAdGroupResponseWithEmptyAdGroupId() {
        getAdGroupRequestParameters.setAdGroupId("111111111111");
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
    }

    @Test
    @Description("Проверка ответа контроллера getAdGroup только с параметром adgroup_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10204")
    public void checkGetAdGroupResponseWithAdGroupId() {
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        GetAdGroupResponse getAdGroupResponseExpected = map(groupFakeInfoExpected, GetAdGroupResponse.class);
        assertThat("Ответ контроллера getAdGroup не совпадает с данными в api", getAdGroupResponse,
                beanEquivalent(getAdGroupResponseExpected));
    }

    @Test
    @Description("Проверка параметра count контроллера getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10202")
    public void checkGetAdGroupResponseWithCount() {
        getAdGroupRequestParameters.setCount("1");
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        List<String> expectedBannerIds = new LinkedList<>();
        expectedBannerIds.add(String.valueOf(bannerIds[0]));
        assertThat("Список баннеров в ответе контроллера getAdGroup не совпадает с данными в api",
                getAdGroupResponse.getBids(), equalTo(expectedBannerIds));
    }

    @Test
    @Description("Проверка параметра count и page контроллера getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10205")
    public void checkGetAdGroupResponseWithCountAndPage() {
        getAdGroupRequestParameters.setCount("1");
        getAdGroupRequestParameters.setPage("2");
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        List<String> expectedBannerIds = new LinkedList<>();
        expectedBannerIds.add(String.valueOf(bannerIds[1]));
        assertThat("Список баннеров в ответе контроллера getAdGroup не совпадает с данными в api",
                getAdGroupResponse.getBids(), equalTo(expectedBannerIds));
    }

    @Test
    @Description("Проверка параметра arch_banners=1 контроллера getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10206")
    public void checkGetAdGroupResponseWithArchBanners() {
        getAdGroupRequestParameters.setArchBanners("1");
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerIds[0]);
        cmdRule.apiAggregationSteps().archiveBanner(CLIENT, bannerIds[0]);
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        List<String> expectedBannerIds = new LinkedList<>();
        expectedBannerIds.add(String.valueOf(bannerIds[0]));
        assertThat("Список баннеров в ответе контроллера getAdGroup не совпадает с данными в api",
                getAdGroupResponse.getBids(), equalTo(expectedBannerIds));
    }

    @Test
    @Description("Проверка параметра arch_banners=0 контроллера getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10207")
    public void checkGetAdGroupResponseWithNoArchBanners() {
        getAdGroupRequestParameters.setArchBanners("0");
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerIds[0]);
        cmdRule.apiAggregationSteps().archiveBanner(CLIENT, bannerIds[0]);
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        List<String> expectedBannerIds = new LinkedList<>();
        expectedBannerIds.add(String.valueOf(bannerIds[1]));
        assertThat("Список баннеров в ответе контроллера getAdGroup не совпадает с данными в api",
                getAdGroupResponse.getBids(), equalTo(expectedBannerIds));
    }

    private Long createGroupWithManyBanners(Long campaignId, Integer bannersCount) {
        Group group = bannersRule.getGroup();
        for (int i = 1; i < bannersCount; i++) {
            group.getBanners().add(BannersFactory.getDefaultTextBanner());
        }
        cmdRule.cmdSteps().groupsSteps()
                .postSaveTextAdGroups(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps().getPhrasesByCid(campaignId)
        .stream().map(PhrasesRecord::getPid).max(Long::compare).get();
    }
}
