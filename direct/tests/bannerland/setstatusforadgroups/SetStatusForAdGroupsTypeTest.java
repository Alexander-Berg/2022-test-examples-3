package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@Title("Проверка выставления статуса BannerLand.setStatusForAdGroups текстовым и РМП группам")
@Issue("https://st.yandex-team.ru/DIRECT-45226")
public class SetStatusForAdGroupsTypeTest {
    private static final String LOGIN = Logins.LOGIN_YNDX_FIXED;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    @Test
    @Title("Для текстовой группы")
    public void testTextGroup() {
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups((long) cid, pid, GenerateStatus.EMPTY);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());
        assertThat("статус успешно выставлен", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }

    @Test
    @Title("Для РМП группы")
    public void testMobileAppGroup() {
        int cid = api.userSteps.campaignSteps().addDefaultMobileAppCampaign().intValue();
        Long pid = api.userSteps.adGroupsSteps().addDefaultMobileGroup(cid);
        BannerLandResponse expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
        Map<Long, List<Long>> errorItems = new HashMap<>();
        errorItems.put((long) cid, Arrays.asList(pid));
        expectedBannerLandResponse.setErrorItems(errorItems);
        BannerLandResponse bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups((long) cid, pid, GenerateStatus.EMPTY);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());
        assertThat("статус успешно выставлен", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }
}
