package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedCategoriesRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedHistoryRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedVendorsRecord;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

/**
 * Created by semkagtn on 19.10.15.
 * https://st.yandex-team.ru/TESTIRT-7490
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка изменения таблиц скриптом ppcFeedToBannerLand.pm для фида с update_status = New")
public class PpcFeedToBannerLandCheckTablesForFeedUpdateStatusNewTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_2;
    private static final String ELLIPTICS_FEED_NAME =
            PpcFeedToBannerLandCheckTablesForFeedUpdateStatusNewTest.class.getSimpleName();

    private static final String CATEGORY_ID = "12345";
    private static final String CATEGORY_PARENT_ID = "54321";
    private static final String CATEGORY_NAME = "omgCategory1";

    private static final String URL = "http://url.ru";
    private static final String OFFER_NAME = "omgThisIsOffer";
    private static final String VENDOR_NAME = "omgThisIsVendor";

    private static final Timestamp LAST_REFRESHED = new Timestamp(1416131797000L);//"2014-11-16 12:56:37.0";

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private static long feedId;
    @Rule
    public Trashman trasher = new Trashman(api);

    @BeforeClass
    public static void getClientInfo() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        YmlCatalog feed = new YmlCatalog().withShop(new Shop()
                .withCategories(new Categories().withCategories(
                        new Category(CATEGORY_ID, CATEGORY_PARENT_ID, CATEGORY_NAME)))
                .withOffers(new Offers().withOffers(new Offer()
                        .withUrl(URL)
                        .withName(OFFER_NAME)
                        .withVendor(VENDOR_NAME)
                        .withCategoryId(CATEGORY_ID))));

        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(url);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}, null);
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_categories")
    public void checkCategoriesTable() {
        List<PerfFeedCategoriesRecord> categories =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedCategoriesRecords(feedId);
        assertThat("в таблицу ppc.perf_feed_categories была добавлена новая запись",
                categories, iterableWithSize(1));
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_vendors")
    public void checkVendorsTable() {
        List<PerfFeedVendorsRecord> vendors =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedVendorsRecords(feedId);
        assertThat("в таблицу ppc.perf_feed_vendors была добавлена новая запись",
                vendors, iterableWithSize(1));
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_history")
    public void checkHistoryTable() {
        List<PerfFeedHistoryRecord> history =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedHistoryRecords(feedId);
        assertThat("в таблицу ppc.perf_feed_history была добавлена новая запись",
                history, iterableWithSize(1));
    }

    @Test
    @Description("Проверка записи в таблице ppc.feeds")
    public void checkFeedsTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);

        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("lastRefreshed"))
                .forFields(newPath("lastRefreshed")).useMatcher(not(equalTo(LAST_REFRESHED)));
        assertThat("поле last_refreshed изменилось", actualFeed, beanDiffer(new FeedsRecord())
                .useCompareStrategy(strategy));
    }
}
