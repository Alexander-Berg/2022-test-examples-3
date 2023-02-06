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
import ru.yandex.autotests.directapi.enums.UpdateStatus;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

/**
 * Created by semkagtn on 20.10.15.
 * https://st.yandex-team.ru/TESTIRT-7490
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка изменения таблиц скриптом ppcFeedToBannerLand.pm для фида с update_status = Done")
public class PpcFeedToBannerLandCheckTablesForFeedUpdateStatusDoneTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_1;
    private static final String ELLIPTICS_FEED_NAME =
            PpcFeedToBannerLandCheckTablesForFeedUpdateStatusDoneTest.class.getSimpleName();

    private static final String CATEGORY_ID = "12345";
    private static final String NEW_CATEGORY_ID = "77777";
    private static final String CATEGORY_PARENT_ID = "54321";
    private static final String CATEGORY_NAME = "omgCategory1";

    private static final String URL = "http://url.ru";
    private static final String OFFER_NAME = "omgThisIsOffer";
    private static final String VENDOR_NAME = "omgThisIsVendor";
    private static final String NEW_VENDOR_NAME = "omgThisIsVendorNew";

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

        String url = saveToEllipticsFeedWithCategoryIdAndVendorName(CATEGORY_ID, VENDOR_NAME);

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(url)
                .setLastRefreshed(LAST_REFRESHED);

        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}, null);

        feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        assumeThat("статус фида изменился на Done", feeds.getUpdateStatus(), equalTo(FeedsUpdateStatus.Done));

        saveToEllipticsFeedWithCategoryIdAndVendorName(NEW_CATEGORY_ID, NEW_VENDOR_NAME);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}, null);
    }

    private static String saveToEllipticsFeedWithCategoryIdAndVendorName(String categoryId, String vendorName) {
        YmlCatalog feed = new YmlCatalog().withShop(new Shop()
                .withCategories(new Categories().withCategories(
                        new Category(categoryId, CATEGORY_PARENT_ID, CATEGORY_NAME)))
                .withOffers(new Offers().withOffers(new Offer()
                        .withUrl(URL)
                        .withName(OFFER_NAME)
                        .withVendor(vendorName)
                        .withCategoryId(categoryId))));
        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        return api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_categories")
    public void checkCategoriesTable() {
        List<PerfFeedCategoriesRecord> actualCategories = api.userSteps.getDirectJooqDbSteps().useShard(shardId)
                .feedsSteps().getPerfFeedCategoriesRecords(feedId);
        actualCategories.sort(Comparator.comparing(PerfFeedCategoriesRecord::getCategoryId));

        PerfFeedCategoriesRecord expectedCategories1 = new PerfFeedCategoriesRecord()
                .setCategoryId(BigInteger.valueOf(Long.valueOf(CATEGORY_ID)))
                .setIsDeleted(1);
        PerfFeedCategoriesRecord expectedCategories2 = new PerfFeedCategoriesRecord()
                .setCategoryId(BigInteger.valueOf(Long.valueOf(NEW_CATEGORY_ID)))
                .setIsDeleted(0);

        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("в таблицу ppc.perf_feed_categories записи изменились верно",
                actualCategories.stream().map(r -> r.intoMap()).collect(Collectors.toList()),
                beanDiffer(Arrays.asList(expectedCategories1.intoMap(), expectedCategories2.intoMap()))
                        .useCompareStrategy(strategy));
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_vendors")
    public void checkVendorsTable() {
        List<PerfFeedVendorsRecord> actualVendors =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedVendorsRecords(feedId);
        assumeThat("получили списко из одного вендора", actualVendors, iterableWithSize(1));
        PerfFeedVendorsRecord expectedVendor = new PerfFeedVendorsRecord().setName(NEW_VENDOR_NAME);

        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("в таблицу ppc.perf_feed_vendors была изменена запись",
                actualVendors.get(0).intoMap(), beanDiffer(expectedVendor.intoMap()).useCompareStrategy(strategy));
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_history")
    public void checkHistoryTable() {
        List<PerfFeedHistoryRecord> history =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedHistoryRecords(feedId);
        assertThat("в таблицу ppc.perf_feed_history была добавлена новая запись", history, hasSize(2));
    }

    @Test
    @Description("Проверка записи в таблице ppc.feeds")
    public void checkFeedsTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);

        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("lastRefreshed"))
                .forFields(newPath("lastRefreshed")).useMatcher(not(equalTo(LAST_REFRESHED)));
        assertThat("поле last_refreshed изменилось", actualFeed.intoMap(),
                beanDiffer(new FeedsRecord().intoMap()).useCompareStrategy(strategy));
    }
}
