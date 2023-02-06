package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
import ru.yandex.qatools.allure.annotations.Step;
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
@Title("Проверка успешного выставления статуса BannerLand.setStatusForAdGroups")
@Issue("https://st.yandex-team.ru/DIRECT-45226")
@RunWith(Parameterized.class)
public class SetStatusForAdGroupsTest {
    private static final String LOGIN = Logins.LOGIN_YNDX_FIXED;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public GenerateStatus status;

    @Parameterized.Parameters(name = "status = {0}")
    public static Iterable<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (GenerateStatus generateStatus : GenerateStatus.values()) {
            data.add(new Object[]{
                    generateStatus
            });
        }
        return data;
    }

    private static BannerLandResponse bannerLandResponse;
    private static BannerLandResponse expectedBannerLandResponse;
    private static Long pid;
    private static Long cid;

    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass

    public static void init() {
        cid = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
        expectedBannerLandResponse = new BannerLandResponse();
        expectedBannerLandResponse.setSuccess(1);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void send() {
        bannerLandResponse = api.userSteps.getDarkSideSteps().getBannerLandSteps()
                .setStatusForAdGroups(cid, pid, status);
        assumeThat("получили ответ", bannerLandResponse, notNullValue());

    }

    @Test
    @Title("Проверка поля Success")
    public void testSuccess() {
        assertThat("статус успешно выставлен", bannerLandResponse, beanDiffer(expectedBannerLandResponse));
    }
}
