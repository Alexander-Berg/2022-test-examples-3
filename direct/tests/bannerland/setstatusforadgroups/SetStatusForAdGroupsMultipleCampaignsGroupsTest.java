package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannerland.BannerLandResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannerland.GenerateStatus;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08/12/15.
 * https://st.yandex-team.ru/TESTIRT-7950
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BANNER_LAND_SET_STATUS_FOR_ADGROUPS)
@Issue("https://st.yandex-team.ru/DIRECT-45226")
@Title("Проверка успешного выставления статуса BannerLand.setStatusForAdGroups в запросе несколько заказов/групп")
public class SetStatusForAdGroupsMultipleCampaignsGroupsTest {
    private static final String LOGIN = Logins.LOGIN_YNDX_FIXED;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    private static Long cid;
    private static Long cidAnother;
    private static Long pid1;
    private static Long pid2;
    private static Long pidAnother;
    private static BannerLandResponse expectedBannerLandResponse;


    @BeforeClass
    public static void init() {
        cid = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        cidAnother = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();

        pid1 = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
        pid2 = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
        pidAnother = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cidAnother);

        expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
    }

    @Test
    @Title("Один заказ с несколькими группами")
    public void testSuccess() {
        Map<Long, GenerateStatus> groups = new HashMap<>();
        groups.put(pid1, GenerateStatus.EMPTY);
        groups.put(pid2, GenerateStatus.NON_EMPTY);
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups((long) cid, groups);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());
        assertThat("статус успешно выставлен", bannerLandResponse
                , beanDiffer(SetStatusForAdGroupsMultipleCampaignsGroupsTest.expectedBannerLandResponse));
    }

    @Test
    @Title("Несколько заказов с одной группой")
    public void testErrorItems() {
        Map<Long, GenerateStatus> groups = new HashMap<>();
        Map<Long, GenerateStatus> groupsAnother = new HashMap<>();
        groups.put(pid1, GenerateStatus.EMPTY);
        groupsAnother.put(pidAnother, GenerateStatus.NON_EMPTY);

        Map<Long, Map<Long, GenerateStatus>> orders = new HashMap<>();

        orders.put((long) cid, groups);
        orders.put((long) cidAnother, groupsAnother);

        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(orders);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());
        assertThat("статус успешно выставлен", bannerLandResponse
                , beanDiffer(SetStatusForAdGroupsMultipleCampaignsGroupsTest.expectedBannerLandResponse));
    }
}
