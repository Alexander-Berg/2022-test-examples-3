package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 14.12.15.
 * https://st.yandex-team.ru/TESTIRT-7969
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-49224")
@Description("Проверка работы скрипта PpcFeedToBannerLand.pm с пустым фидом")
public class PpcFeedToBannerLandEmptyFeedTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    //Пустой фид лежит на Руктояточнике - /usr/share/static-files/files/empty_feed.xml
    private static final String EMPTY_FEED_URL = "http://ppctest-handles.yandex.ru/files/empty_feed.xml";

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private int shardId;
    private long feedId;

    @Before
    @Step("Подготовка данных для теста")
    public void init() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(EMPTY_FEED_URL);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}, null);
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord()
                .setFetchErrorsCount(1)
                .setUpdateStatus(FeedsUpdateStatus.Error);
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("число ошибок в таблице feeds соответствует ожидаемому", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }
}
