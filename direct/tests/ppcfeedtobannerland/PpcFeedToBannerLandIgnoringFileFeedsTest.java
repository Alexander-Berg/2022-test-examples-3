package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsSource;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.sql.Timestamp;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 16.11.15.
 * https://st.yandex-team.ru/TESTIRT-7490
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка игнорирования скриптом PpcFeedToBannerLand.pm фидов-файлов")
public class PpcFeedToBannerLandIgnoringFileFeedsTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_5;
    private static final String ELLIPTICS_FEED_NAME = PpcFeedToBannerLandIgnoringFileFeedsTest.class.getSimpleName();

    private static final String CATEGORY_ID = "12345";
    private static final String CATEGORY_PARENT_ID = "54321";
    private static final String CATEGORY_NAME = "omgCategory1";

    private static final String OFFER_NAME = "omgThisIsOffer";
    private static final String VENDOR_NAME = "omgThisIsVendor";

    private static final Timestamp LAST_REFRESHED = new Timestamp(1416131797000L);

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private static String clientId;
    @Rule
    public Trashman trasher = new Trashman(api);
    private long feedId;

    @BeforeClass
    public static void getUserShard() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
    }

    @Before
    @Step("Подготовка данных для теста")
    public void getClientInfo() {
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        YmlCatalog feed = new YmlCatalog().withShop(new Shop()
                .withCategories(new Categories().withCategories(
                        new Category(CATEGORY_ID, CATEGORY_PARENT_ID, CATEGORY_NAME)))
                .withOffers(new Offers().withOffers(new Offer()
                        .withName(OFFER_NAME)
                        .withVendor(VENDOR_NAME)
                        .withCategoryId(CATEGORY_ID))));

        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setSource(FeedsSource.file)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.Done)
                .setUrl(url)
                .setLastRefreshed(LAST_REFRESHED);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
    }

    @Test
    public void checkFeedsTable() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                new String[]{clientId});
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);

        FeedsRecord expectedFeed = new FeedsRecord().setLastRefreshed(LAST_REFRESHED);

        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("значение last_change для фида-файла не изменилось", actualFeed.intoMap(),
                beanDiffer(expectedFeed.intoMap()).useCompareStrategy(strategy));
    }
}
