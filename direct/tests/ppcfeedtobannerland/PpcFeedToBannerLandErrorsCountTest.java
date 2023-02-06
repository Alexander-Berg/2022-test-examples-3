package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.util.Arrays;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 16.11.15.
 * https://st.yandex-team.ru/TESTIRT-7490
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка подсчёта ошибок (errors_count в таблице ppc.feeds) скриптом PpcFeedToBannerLand.pl")
@RunWith(Parameterized.class)
public class PpcFeedToBannerLandErrorsCountTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    private static final String ELLIPTICS_FEED_NAME = PpcFeedToBannerLandErrorsCountTest.class.getSimpleName();

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);
    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public YmlCatalog feed;
    @Parameterized.Parameter(2)
    public Integer expectedErrorsCount;
    private int shardId;
    private long feedId;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"валидный фид", new YmlCatalog().withShop(new Shop()
                        .withCategories(new Categories().withCategories(
                                new Category("12345", "54321", "CategoryName")))
                        .withOffers(new Offers().withOffers(new Offer()
                                .withUrl("http://url.ru")
                                .withName("OfferName")
                                .withVendor("VendorName")
                                .withCategoryId("12345")))),
                        0},
                {"невалидный фид", new YmlCatalog().withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category()))
                        .withOffers(new Offers()
                                .withOffers(new Offer()))),
                        1},
        });
    }

    @Before
    @Step("Подготовка данных для теста")
    public void getClientInfo() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(url);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                new String[]{clientId});
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord().setFetchErrorsCount(expectedErrorsCount);

        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("число ошибок в таблице feeds соответствует ожидаемому", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }
}
