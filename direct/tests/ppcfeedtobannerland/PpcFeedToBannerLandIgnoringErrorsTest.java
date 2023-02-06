package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 20.06.16.
 * https://st.yandex-team.ru/TESTIRT-9726
 */
@Aqua.Test
@Features({FeatureNames.INTEGRATION_WITH_BANNER_LAND, FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-55720")
@Description("Проверка выборки фидов в статусе Error скриптом ppcFeedToBannerLand.pl в зависимости от времени")
public class PpcFeedToBannerLandIgnoringErrorsTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_6;
    private static final String ELLIPTICS_FEED_NAME = PpcFeedToBannerLandIgnoringErrorsTest.class.getSimpleName();

    private static final String CATEGORY_ID = "12345";
    private static final String CATEGORY_PARENT_ID = "54321";
    private static final String CATEGORY_NAME = "omgCategory1";

    private static final String URL = "http://url.ru";
    private static final String OFFER_NAME = "omgThisIsOffer";
    private static final String VENDOR_NAME = "omgThisIsVendor";
    private static final Integer DEFAULT_CHECK_GAP_DAYS = 1;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);
    public static DirectJooqDbSteps dbSteps = api.userSteps.getDirectJooqDbSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private static String clientId;
    private static String url;
    private static int checkGapDays;
    @Rule
    public Trashman trasher = new Trashman(api);

    @BeforeClass
    public static void getClientInfo() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        checkGapDays = Integer.parseInt(dbSteps.ppcPropertiesSteps().getPropertyValue("bl_recheck_interval_error", DEFAULT_CHECK_GAP_DAYS.toString()));

        YmlCatalog feed = new YmlCatalog().withShop(new Shop()
                .withCategories(new Categories().withCategories(
                        new Category(CATEGORY_ID, CATEGORY_PARENT_ID, CATEGORY_NAME)))
                .withOffers(new Offers().withOffers(new Offer()
                        .withUrl(URL)
                        .withName(OFFER_NAME)
                        .withVendor(VENDOR_NAME)
                        .withCategoryId(CATEGORY_ID))));

        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps().ymlCatalogToXmlString(feed);
        url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);
    }

    @Test
    public void testMoreThenGapPassed() {
        Timestamp lastChange = new Timestamp(Date.from(Instant.now().minus(checkGapDays, DAYS)
                .minus(1, MINUTES)).getTime());

        FeedsRecord feed1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.Error)
                .setUrl(url)
                .setLastchange(lastChange);
        long feedId1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feed1);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId
                , new Long[]{feedId1}, new String[]{clientId}, null, false);
        feed1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId1);

        assertThat("статус фида изменился с Error", feed1.getUpdateStatus(),
                not(equalTo(FeedsUpdateStatus.Error)));
    }

    @Test
    public void testLessThenGapPassed() {
        Timestamp lastChange = new Timestamp(Date.from(Instant.now().minus(checkGapDays, DAYS)
                .plus(1, MINUTES)).getTime());
        FeedsRecord feed1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.Error)
                .setUrl(url)
                .setLastchange(lastChange);

        long feedId1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feed1);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId
                , new Long[]{feedId1}, new String[]{clientId}, null, false);
        feed1 = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId1);

        assertThat("статус фида остался Error", feed1.getUpdateStatus(),
                equalTo(FeedsUpdateStatus.Error));
    }
}
