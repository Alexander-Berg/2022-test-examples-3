package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.beans.feeds.FeedType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsBusinessType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedCategoriesRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedHistoryRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedVendorsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * https://st.yandex-team.ru/TESTIRT-11363
 */
@Aqua.Test
@Title("Проверка успешного получения от BL данных для фидов различного типа скриптом ppcFeedToBannerLand.pl")
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-66084")
@RunWith(Parameterized.class)
public class PpcFeedToBannerLandPositiveTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);
    public static DirectJooqDbSteps dbSteps = api.userSteps.getDirectJooqDbSteps();
    public static FeedsRecord feed;
    public static PerfFeedHistoryRecord feedHistory;
    public static List<PerfFeedCategoriesRecord> feedCategories;
    public static List<PerfFeedVendorsRecord> feedVendors;

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private static long feedId;
    private static String clientId;
    @Rule
    public Trashman trasher = new Trashman(api);
    @Parameterized.Parameter(0)
    public String feedUrl;
    @Parameterized.Parameter(1)
    public FeedsBusinessType businessType;
    @Parameterized.Parameter(2)
    public FeedType feedType;
    @Parameterized.Parameter(3)
    public Long offersCount;
    @Parameterized.Parameter(4)
    public List<PerfFeedCategoriesRecord> expectedFeedCategories;
    @Parameterized.Parameter(5)
    public List<PerfFeedVendorsRecord> expectedFeedVendors;

    public static List<PerfFeedCategoriesRecord> retailFeedCategories = new ArrayList<PerfFeedCategoriesRecord>() {{
        add(new PerfFeedCategoriesRecord().setCategoryId(BigInteger.valueOf(2))
                .setParentCategoryId(BigInteger.valueOf(0)).setName("Телефоны").setOffersCount(1L));
        add(new PerfFeedCategoriesRecord().setCategoryId(BigInteger.valueOf(18))
                .setParentCategoryId(BigInteger.valueOf(19)).setName("Пылесосы").setOffersCount(1L));
    }};
    public static List<PerfFeedVendorsRecord> retailFeedVendors = new ArrayList<PerfFeedVendorsRecord>() {{
        add(new PerfFeedVendorsRecord().setName("Dyson"));
        add(new PerfFeedVendorsRecord().setName("Meizu"));
    }};

    public static List<PerfFeedCategoriesRecord> universalFeedCategories = new ArrayList<PerfFeedCategoriesRecord>() {{
        add(new PerfFeedCategoriesRecord().setCategoryId(BigInteger.valueOf(1000000001))
                .setParentCategoryId(BigInteger.valueOf(0)).setName("Все").setOffersCount(0L));
    }};
    public static List<PerfFeedVendorsRecord> emptyFeedVendors = new ArrayList<PerfFeedVendorsRecord>();

    @Parameterized.Parameters(name = "businessType = {1}, feedUrl = {0}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/retail_small.xml",
                        FeedsBusinessType.retail, FeedType.YANDEX_MARKET, 2L, retailFeedCategories, retailFeedVendors},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/hotels-for-offer-examples.csv",
                        FeedsBusinessType.hotels, FeedType.GOOGLE_HOTELS, 213L, universalFeedCategories,
                        emptyFeedVendors},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/realty-for-offer-examples.xml",
                        FeedsBusinessType.realty, FeedType.YANDEX_REALTY, 6732L, universalFeedCategories,
                        emptyFeedVendors},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/auto-for-offer-examples.xml",
                        FeedsBusinessType.auto, FeedType.AUTO_RU, 1035L, universalFeedCategories, emptyFeedVendors},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/flights-for-offer-examples.csv",
                        FeedsBusinessType.flights, FeedType.GOOGLE_FLIGHTS, 4672L, universalFeedCategories,
                        emptyFeedVendors},
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
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setBusinessType(businessType)
                .setUrl(feedUrl);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                new String[]{clientId});
        feed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        feedHistory =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedHistoryRecords(feedId)
                        .get(0);
        feedCategories = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .getPerfFeedCategoriesRecords(feedId);
        feedVendors =
                api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedVendorsRecords(feedId);
    }

    @Test
    public void checkFeedValues() {
        // таблица feeds
        FeedsRecord expectedFeed =
                new FeedsRecord().setUpdateStatus(FeedsUpdateStatus.Done).setFeedType(feedType.getTypedValue())
                        .setOffersCount(offersCount);
        assertThat("в таблицe ppc.feeds записи изменились верно", feed,
                JooqRecordDifferMatcher.recordDiffer(expectedFeed)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
        assertThat("поле offer_examples содержит валидный JSON",
                JsonUtils.getObject(feed.getOfferExamples(), Object.class), is(notNullValue()));
    }

    @Test
    public void checkFeedHistoryValues() {
        // таблица perf_feed_history
        assertThat("поле offers_count содержит правильное количество офферов", feedHistory.getOffersCount(),
                equalTo(offersCount));
        assertThat("поле parse_results_json_compressed не содержит записи об ошибке",
                feedHistory.getParseResultsJsonCompressed(), is(nullValue()));
    }

    @Test
    public void checkFeedCategoriesValues() {
        // таблица perf_feed_categories
        feedCategories.sort(Comparator.comparing(PerfFeedCategoriesRecord::getCategoryId));
        expectedFeedCategories.sort(Comparator.comparing(PerfFeedCategoriesRecord::getCategoryId));
        List<Map<String, Object>> actualCategories =
                feedCategories.stream().map(r -> r.intoMap()).collect(Collectors.toList());
        List<Map<String, Object>> expectedCategories =
                expectedFeedCategories.stream().map(r -> r.intoMap()).collect(Collectors.toList());
        assertThat("в таблицe ppc.perf_feed_categories записи изменились верно",
                actualCategories,
                beanDiffer(expectedCategories)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void checkFeedVendorsValues() {
        // таблица perf_feed_vendors
        feedVendors.sort(Comparator.comparing(PerfFeedVendorsRecord::getName));
        expectedFeedVendors.sort(Comparator.comparing(PerfFeedVendorsRecord::getName));
        List<Map<String, Object>> actualVendors =
                feedVendors.stream().map(r -> r.intoMap()).collect(Collectors.toList());
        List<Map<String, Object>> expectedVendors =
                expectedFeedVendors.stream().map(r -> r.intoMap()).collect(Collectors.toList());
        assertThat("в таблицe ppc.perf_feed_vendors записи изменились верно",
                actualVendors,
                beanDiffer(expectedVendors)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @After
    public void after() {
        dbSteps.feedsSteps().deletePerfFeedCategoriesRecords(feedId);
        dbSteps.feedsSteps().deletePerfFeedVendorsRecords(feedId);
        dbSteps.feedsSteps().deletePerfFeedHistoryRecords(feedId);
        dbSteps.feedsSteps().deleteFeedById(feedId);
    }
}
