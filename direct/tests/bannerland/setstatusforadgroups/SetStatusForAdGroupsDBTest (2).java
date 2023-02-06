package ru.yandex.autotests.directintapi.tests.bannerland.setstatusforadgroups;

import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdgroupsDynamicStatusblgenerated;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdgroupsDynamicRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannerland.GenerateStatus;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

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
    public AdgroupsDynamicStatusblgenerated statusInDB;

    @Parameterized.Parameters(name = "status = {0}")
    public static Iterable<Object[]> data() {
        Object[][] data = new Object[][]{
                {GenerateStatus.EMPTY, AdgroupsDynamicStatusblgenerated.No},
                {GenerateStatus.NON_EMPTY, AdgroupsDynamicStatusblgenerated.Yes}
        };
        return Arrays.asList(data);
    }

    @Rule
    public Trashman trashman = new Trashman(api);

    @Test
    public void testBlStatusGeneratedInDB() {
        int shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        Long cid = api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroupDynamic(cid);
        api.userSteps.getDarkSideSteps().getBannerLandSteps().setStatusForAdGroups(cid, pid, status);
        AdgroupsDynamicRecord adgroupsDynamic
                = api.userSteps.getDirectJooqDbSteps().useShard(shard).adGroupsSteps().getAdgroupsDynamic(pid);
        assertThat("запись из базы соответствует ожидаемой"
                , adgroupsDynamic.getStatusblgenerated(), equalTo(statusInDB));
    }
}
