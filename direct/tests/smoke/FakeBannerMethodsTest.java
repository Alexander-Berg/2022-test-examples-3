package ru.yandex.autotests.directintapi.tests.smoke;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/DIRECT-27178
 */

@Aqua.Test
@Features(FeatureNames.FAKE_INTAPI_MONITOR)
public class FakeBannerMethodsTest {
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static long BANNER_ID = 224214898;

    @Rule
    public BottleMessageRule bmr = new BottleMessageRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Stories("FakeGetBannerParams")
    @Test
    public void getBannerParamsTest() {
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(BANNER_ID);
        assertThat("Неверный bid у баннера", bannerFakeInfo.getBid(), equalTo(BANNER_ID));
        assertNotNull(bannerFakeInfo.getLogin());
    }

    @Stories("FakeBannerParams")
    @Test
    public void updateBannerParamsTest() {
        String bannerID = String.valueOf(Math.abs(RandomUtils.getNextInt()));
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(BANNER_ID);
        bannerFakeInfo.setBannerID(bannerID);
        darkSideSteps.getBannersFakeSteps().updateBannerParams(bannerFakeInfo);
        assertThat("Неверный bannerID у баннера",
                darkSideSteps.getBannersFakeSteps().getBannerParams(BANNER_ID).getBannerID(), equalTo(bannerID));
    }
}
