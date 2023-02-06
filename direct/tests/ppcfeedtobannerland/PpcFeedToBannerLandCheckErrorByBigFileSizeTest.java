package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by proxeter on 28.01.16.
 * <p>
 * https://st.yandex-team.ru/TESTIRT-8363
 * https://st.yandex-team.ru/TESTIRT-9557
 */
@Aqua.Test
@Title("Проверка максимального размера фида скриптом ppcFeedToBannerLand.pl")
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-50490")
@RunWith(Parameterized.class)
public class PpcFeedToBannerLandCheckErrorByBigFileSizeTest {

    // "updateStatus": "Error" должен наступить на maxErrorsCheckCount проход скрипта, до этого New->Updating
    private static final Integer DEFAULT_MAX_ERRORS_CHECK_COUNT = 1;
    // нужно чтобы максимальный размер фида был меньше переданного, чтобы получить ошибку 1266 от БаннерЛенда
    private static final int MAX_FEED_SIZE = 1; // в Мб

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    private static final String BIG_YAML_CATALOG =
            "http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/YAMLCatalog";
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);
    public static DirectJooqDbSteps dbSteps = api.userSteps.getDirectJooqDbSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private static long feedId;
    private static String clientId;
    private static int maxErrorsCheckCount = Integer.parseInt(dbSteps.ppcPropertiesSteps().getPropertyValue("bl_max_errors_count", DEFAULT_MAX_ERRORS_CHECK_COUNT.toString()));
    ;
    @Rule
    public Trashman trasher = new Trashman(api);
    @Parameterized.Parameter(0)
    public Integer fetchErrorsCount;
    @Parameterized.Parameter(1)
    public FeedsUpdateStatus updateStatus;

    @Parameterized.Parameters(name = "fetchErrorsCount = {0}, updateStatus = {1}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {maxErrorsCheckCount - 1, FeedsUpdateStatus.Error},
                {maxErrorsCheckCount, FeedsUpdateStatus.Error},
        };

        return Arrays.asList(data);
    }

    @BeforeClass
    public static void beforeClass() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        dbSteps.useShard(shardId);
    }

    @Before
    public void before() {
        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setFetchErrorsCount(fetchErrorsCount)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(BIG_YAML_CATALOG);

        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
    }

    @Test
    public void checkUpdateStatusValue() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                clientId, MAX_FEED_SIZE);
        FeedsRecord feed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        assertThat("статус должен быть " + updateStatus, feed.getUpdateStatus(), equalTo(updateStatus));
    }

    @After
    public void after() {
        dbSteps.feedsSteps().deletePerfFeedHistoryRecords(feedId);
        dbSteps.feedsSteps().deleteFeedById(feedId);
    }
}
