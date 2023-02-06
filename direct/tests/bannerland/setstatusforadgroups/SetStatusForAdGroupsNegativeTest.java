package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
@Title("Проверка вызова BannerLand.setStatusForAdGroups с ошибками")
public class SetStatusForAdGroupsNegativeTest {
    private static final String LOGIN = Logins.LOGIN_YNDX_FIXED;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    private static Long cid;
    private static Long cidAnother;
    private static Long pid;
    private static Long pidAnother;

    @BeforeClass
    public static void init() {
        cid = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
        cidAnother = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        pidAnother = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cidAnother);
    }

    @Test
    public void testNullRequest() {
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(null);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());
        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        assertThat("получили ожидаемый ответ", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    public void testNullStatus() {
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(cid, pid, null);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    public void testUnexistingOrderId() {
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(cid + 100000l, pid, GenerateStatus.EMPTY);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put(cid + 100000l, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }


    @Test
    public void testUnexistingGroupId() {
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(cid, pid + 100000, GenerateStatus.EMPTY);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pid + 100000));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }


    @Test
    public void testGroupFromOtherOrder() {
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(cid, pidAnother, GenerateStatus.EMPTY);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pidAnother));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    public void testGroupsFromOthersOrder() {
        Map<Long, GenerateStatus> groups = new HashMap<>();
        Map<Long, GenerateStatus> groupsAnother = new HashMap<>();
        groups.put(pidAnother, GenerateStatus.EMPTY);
        groupsAnother.put(pid, GenerateStatus.NON_EMPTY);

        Map<Long, Map<Long, GenerateStatus>> orders = new HashMap<>();

        orders.put(cid, groups);
        orders.put(cidAnother, groupsAnother);

        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(orders);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pidAnother));
        errorItems.put((long) cidAnother, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    public void testGroupFromOtherOrderAndValid() {
        Map<Long, GenerateStatus> groups = new HashMap<>();
        Map<Long, GenerateStatus> groupsAnother = new HashMap<>();
        groups.put(pidAnother, GenerateStatus.EMPTY);
        groupsAnother.put(pidAnother, GenerateStatus.NON_EMPTY);

        Map<Long, Map<Long, GenerateStatus>> orders = new HashMap<>();

        orders.put(cid, groups);
        orders.put(cidAnother, groupsAnother);

        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(orders);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pidAnother));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    public void testNullStatusAndValid() {
        Map<Long, GenerateStatus> groups = new HashMap<>();
        Map<Long, GenerateStatus> groupsAnother = new HashMap<>();
        groups.put(pid, null);
        groupsAnother.put(pidAnother, GenerateStatus.NON_EMPTY);

        Map<Long, Map<Long, GenerateStatus>> orders = new HashMap<>();

        orders.put(cid, groups);
        orders.put(cidAnother, groupsAnother);

        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(orders);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        assertThat("в ответе содержатся ожидаемые ошибки", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }
}
