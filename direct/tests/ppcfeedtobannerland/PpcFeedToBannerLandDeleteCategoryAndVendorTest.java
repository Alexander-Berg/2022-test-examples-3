package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedCategoriesRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedVendorsRecord;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 05.11.15.
 * https://st.yandex-team.ru/TESTIRT-7490
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка удаления категории с помощью скрипта ppcFeedToBannerLand.pm")
public class PpcFeedToBannerLandDeleteCategoryAndVendorTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_3;
    private static final String ELLIPTICS_FEED_NAME =
            PpcFeedToBannerLandDeleteCategoryAndVendorTest.class.getSimpleName();

    private static final String DEFAULT_CATEGORY_ID = "11111";
    private static final String DEFAULT_CATEGORY_NAME = "default";
    private static final String CATEGORY_ID = "22222";
    private static final String CATEGORY_PARENT_ID = "54321";
    private static final String CATEGORY_NAME = "omgCategory1";

    private static final String URL = "http://url.ru";
    private static final String OFFER_NAME = "omgThisIsOffer";
    private static final String VENDOR_NAME = "omgThisIsVendor";
    private static final String DEFAULT_VENDOR_NAME = "vendor";
    private static final Category DEFAULT_CATEGORY = new Category(
            DEFAULT_CATEGORY_ID, CATEGORY_PARENT_ID, DEFAULT_CATEGORY_NAME);
    private static final Offer DEFAULT_OFFER = new Offer()
            .withUrl(URL + "/1")
            .withName("offer")
            .withVendor(DEFAULT_VENDOR_NAME)
            .withCategoryId(DEFAULT_CATEGORY_ID);

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

        String url = saveToEllipticsFeedWithCategoryAndOffer(
                new Category(CATEGORY_ID, CATEGORY_PARENT_ID, CATEGORY_NAME), new Offer()
                        .withUrl(URL)
                        .withName(OFFER_NAME)
                        .withVendor(VENDOR_NAME)
                        .withCategoryId(CATEGORY_ID));

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(url);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                new String[]{clientId});
        feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        assumeThat("статус фида изменился на Done", feeds.getUpdateStatus(),
                equalTo(FeedsUpdateStatus.Done));
        saveToEllipticsFeedWithCategoryAndOffer(null, null);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                new String[]{clientId});
    }

    private static String saveToEllipticsFeedWithCategoryAndOffer(Category category, Offer offer) {
        YmlCatalog feed = new YmlCatalog().withShop(new Shop()
                .withCategories(new Categories().withCategories(DEFAULT_CATEGORY, category))
                .withOffers(new Offers().withOffers(DEFAULT_OFFER, offer)));
        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        return api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_categories")
    public void checkCategoriesTable() {
        List<PerfFeedCategoriesRecord> actualCategories = api.userSteps.getDirectJooqDbSteps().useShard(shardId)
                .feedsSteps().getPerfFeedCategoriesRecords(feedId);
        actualCategories.sort(Comparator.comparing(PerfFeedCategoriesRecord::getCategoryId));

        PerfFeedCategoriesRecord expectedCategories = new PerfFeedCategoriesRecord()
                .setCategoryId(BigInteger.valueOf(Long.valueOf(CATEGORY_ID)))
                .setIsDeleted(1);
        PerfFeedCategoriesRecord defaultExpectedCategories = new PerfFeedCategoriesRecord()
                .setCategoryId(BigInteger.valueOf(Long.valueOf(DEFAULT_CATEGORY_ID)))
                .setIsDeleted(0);

        assertThat("в таблицу ppc.perf_feed_categories записи изменились верно",
                actualCategories.stream().map(r -> r.intoMap()).collect(Collectors.toList()),
                beanDiffer(Arrays.asList(defaultExpectedCategories.intoMap(), expectedCategories.intoMap()))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Проверка записи в таблице ppc.perf_feed_vendors")
    public void checkVendorsTable() {
        List<PerfFeedVendorsRecord> actualVendors = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .getPerfFeedVendorsRecords(feedId);

        PerfFeedVendorsRecord expectedVendor = new PerfFeedVendorsRecord()
                .setName(DEFAULT_VENDOR_NAME);

        assertThat("из таблицы ppc.perf_feed_vendors была удалена запись с feedId = " + feedId,
                actualVendors.stream().map(r -> r.intoMap()).collect(Collectors.toList()),
                beanDiffer(Arrays.asList(expectedVendor.intoMap()))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
