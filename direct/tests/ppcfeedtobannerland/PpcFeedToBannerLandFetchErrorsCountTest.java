package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
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

import static ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus.Updating;
import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Features({FeatureNames.PPC_FEED_TO_BANNER_LAND})
@Issue("https://st.yandex-team.ru/DIRECT-43745")
@Description("Проверка подсчёта ошибок скачивания (fetch_errors_count в таблице ppc.feeds) скриптом PpcFeedToBannerLand.pl")
@RunWith(Parameterized.class)
public class PpcFeedToBannerLandFetchErrorsCountTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String FEED_URL = "http://some-url.domain.ru/files/valid-response.xml";

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;

    private static final String BL_RESPONSE = "{\n"
            + "   \"all_elements_amount\" : 1,\n"
            + "   \"feed_type\" : \"YandexMarket\",\n"
            + "   \"categoryId\" : {\n"
            + "      \"12345\" : 1\n"
            + "   },\n"
            + "   \"categs\" : [\n"
            + "      {\n"
            + "         \"id\" : \"12345\",\n"
            + "         \"category\" : \"CategoryName\",\n"
            + "         \"parentId\" : \"54321\"\n"
            + "      }\n"
            + "   ],\n"
            + "   \"domain\" : {\n"
            + "      \"url.ru\" : 1\n"
            + "   },\n"
            + "   \"vendor\" : {\n"
            + "      \"VendorName\" : 1\n"
            + "   },\n"
            + "   \"file_data\" : \"TTTTTT\",\n"
            + "   \"errors\" : [],\n"
            + "   \"warnings\" : []\n"
            + "}";

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    private static int maxErrorsCheckCount = Integer.parseInt(api.userSteps.getDirectJooqDbSteps().ppcPropertiesSteps()
            .getPropertyValue("bl_max_errors_count", "1"));

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);
    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String blUrl;
    @Parameterized.Parameter(2)
    public Integer currentFetchErrorsCount;
    @Parameterized.Parameter(3)
    public Integer expectedFetchErrorsCount;
    @Parameterized.Parameter(4)
    public FeedsUpdateStatus expectedFeedStatus;
    private int shardId;
    private long feedId;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"валидный ответ, исходное значение счетчика 0", FakeBsProxyConfig.getPpcFeedToBannerlandUrl(), 0, 0,
                        FeedsUpdateStatus.Done},
                {"валидный ответ, исходное значение счетчика 1", FakeBsProxyConfig.getPpcFeedToBannerlandUrl(), 1, 0,
                        FeedsUpdateStatus.Done},
                {"невалидный ответ, исходное значение счетчика 0", FakeBsProxyConfig.get500Url(), 0, 1,
                        FeedsUpdateStatus.Updating},
                {"невалидный ответ, исходное значение счетчика 1", FakeBsProxyConfig.get500Url(), 1, 2,
                        FeedsUpdateStatus.Updating},
                {"невалидный ответ, исходное значение счетчика 1", FakeBsProxyConfig.get500Url(),
                        maxErrorsCheckCount - 1, maxErrorsCheckCount, FeedsUpdateStatus.Updating},
                {"невалидный ответ, исходное значение счетчика 1", FakeBsProxyConfig.get500Url(), maxErrorsCheckCount,
                        maxErrorsCheckCount + 1, FeedsUpdateStatus.Error},
        });
    }

    @BeforeClass
    public static void prepareBannerLandResponse() {
        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean().withObjectIds(
                Collections.singletonList(FEED_URL)
        );
        HELPER.saveFakeBSProxyLogBean(logBean.withResponseEntity(BL_RESPONSE));
        addJsonAttachment("Ответ BL для фида " + FEED_URL, logBean.getResponseEntity());
    }

    @BeforeClass
    public static void cleanup() {
        HELPER.deleteFakeBSProxyLogBeansById(FEED_URL);
    }

    @Before
    @Step("Подготовка данных для теста")
    public void getClientInfo() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(Updating)
                .setFetchErrorsCount(currentFetchErrorsCount)
                .setUrl(FEED_URL);

        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedToBannerLand(shardId, new Long[]{feedId}, null, blUrl);
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord().setFetchErrorsCount(expectedFetchErrorsCount);

        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("число ошибок в таблице feeds соответствует ожидаемому", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }
}
