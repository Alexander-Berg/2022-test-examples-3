package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfFeedHistoryRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 18/08/15.
 * https://st.yandex-team.ru/TESTIRT-9276
 */
@Aqua.Test
@Title("Проверка обработки различных ошибок скриптом ppcFeedToBannerLand.pl")
@Features(FeatureNames.PPC_FEED_TO_BANNER_LAND)
@Issue("https://st.yandex-team.ru/DIRECT-53641")
public class PpcFeedToBannerlandInvalidResponseTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String LOGIN = ru.yandex.autotests.directapi.darkside.Logins.PPC_FEED_TO_BANNER_LAND_4;
    private static final String FEED_URL = "http://some-url.domain.ru/files/invalid-response.xml";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.SUPER_LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    private int shardId;
    private long feedId;

    @Before
    @Step("Подготовка данных для теста")
    public void init() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean().withObjectIds(
                Collections.singletonList(FEED_URL)
        );
        HELPER.saveFakeBSProxyLogBean(logBean.withResponseEntity("{\"asdasdasdasd:[\"asdasd\"]}"));
        addJsonAttachment("Ответ BL для фида " + FEED_URL, logBean.getResponseEntity());

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.Updating)
                .setUrl(FEED_URL);

        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}
                , null, FakeBsProxyConfig.getPpcFeedToBannerlandUrl());
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        List<PerfFeedHistoryRecord> perfFeedHistories
                = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getPerfFeedHistoryRecords(feedId);
        assumeThat("число записей в таблице perf_feed_history соответствует ожидаемому"
                , perfFeedHistories, anyOf(nullValue(), iterableWithSize(0)));
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord()
                .setFetchErrorsCount(1)
                .setUpdateStatus(FeedsUpdateStatus.Updating);
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("запсись в таблице feeds соответствует ожиданиям", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }

    @After
    public void after() {
        HELPER.deleteFakeBSProxyLogBeansById(FEED_URL);
    }

}
