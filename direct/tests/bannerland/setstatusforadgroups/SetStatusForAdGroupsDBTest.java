package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.Arrays;

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
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannerland.GenerateStatus;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.logic.ppc.AdgroupsDynamic;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by buhter on 08/12/15.
 * https://st.yandex-team.ru/TESTIRT-7950
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BANNER_LAND_SET_STATUS_FOR_ADGROUPS)
@Title("Проверка выставления статуса BannerLand.setStatusForAdGroups проверка в БД")
@Issue("https://st.yandex-team.ru/DIRECT-45226")
@RunWith(Parameterized.class)
public class SetStatusForAdGroupsDBTest {
    private static final String LOGIN = Logins.LOGIN_YNDX_FIXED;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public GenerateStatus status;

    @Parameterized.Parameter(1)
    public String statusInDB;

    @Parameterized.Parameters(name = "status = {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {GenerateStatus.EMPTY, Status.NO},
                {GenerateStatus.NON_EMPTY, Status.YES}
        });
    }

    private AdgroupsDynamic adgroupsDynamic;
    private static Long pid;
    private static Long cid;
    private static int shard;

    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass
    public static void init() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        cid = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void send() {
        api.userSteps.getDarkSideSteps().getBannerLandSteps().setStatusForAdGroups((long) cid, pid, status);
        adgroupsDynamic
                = api.userSteps.getDBSteps().adgroupsDynamicSteps().getAdgroupsDynamic(pid, shard);
        assumeThat("получили запись из базы", adgroupsDynamic, notNullValue());

    }

    @Test
    public void testBlStatusGeneratedInDB() {
        assertThat("запись из базы соответствует ожидаемой"
                , adgroupsDynamic.getStatusBlGenerated(), equalTo(statusInDB));
    }
}
