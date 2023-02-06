package ru.yandex.autotests.directintapi.tests.ppcperfomancerejectedoffers;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import com.google.gson.Gson;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRejectedOffersRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.performancerejectedoffers.DataBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.performancerejectedoffers.PerformanceRejectedOffersResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.performancerejectedoffers.ReasonBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.apiclient.config.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.campaigns.Status;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig.getPerfomanceRejectedOffersUrl;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


/**
 * Created by buhter on 18/03/16
 * https://st.yandex-team.ru/TESTIRT-8658
 * Because script uses single property "update_feeds_rejected_offers_timestamp" from ppcdict.ppc_properties
 * it's not recommended to run these or any other tests on ppcFeedsRejectedOffers.pl script in parallel.
 * If you need new test: extend this class with new test methods or do not include new one in regression.
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_FEEDS_REJECTED_OFFERS)
@Issue("https://st.yandex-team.ru/DIRECT-49080")
public class PpcFeedsRejectedOffersTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static final String FEED_URL = RandomUtils.getString(25);
    private static int OFFER_ID = RandomUtils.getRandomInteger(100000, 200000);
    private static HashMap<String, List<ReasonBean>> reasons = new HashMap<>();
    private static Long clientId;
    private static DirectJooqDbSteps dbSteps;
    private Long timestampFromDb;
    private Long timestampForResponse;
    private List<FeedsRejectedOffersRecord> feedsRejectedOffers;

    @BeforeClass
    public static void beforeClass() {
        int shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = Long.valueOf(User.get(LOGIN).getClientID());
        dbSteps = new DirectJooqDbSteps(DirectTestRunProperties.getInstance().getDirectHost().replace("https://", ""))
                .useShardForLogin(LOGIN);
        reasons.put(ReasonBean.GLOBAL, Collections.singletonList(
                new ReasonBean().withDeclineReasons("{\"someItem\":\"someReason\"}")));
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        dbSteps.feedRejectedOffersSteps().deleteFeedsRejectedOffers(clientId);
        timestampFromDb = api.userSteps.getDarkSideSteps().getDBSteps()
                .getPpcPropertiesSteps().getUpdateFeedsRejectedOffersTimestamp();
        timestampForResponse = new Date().toInstant().getEpochSecond();
    }

    @After
    public void after() {
        HELPER.deleteFakeBSProxyLogBeansById(timestampFromDb);
    }

    @Test
    @Description("В ответе нет данных")
    public void testNoDataInResponse() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(new ArrayList<>());

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();

        assumeThat("в базу записался правильный timestamp", timestampFromDb, equalTo(timestampForResponse));
        assertThat("в базе не появилось записей для clientId = " + clientId, feedsRejectedOffers, iterableWithSize(0));
    }

    @Test
    @Description("В ответе нет отклоненных офферов")
    public void testNoRejectedData() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Collections.singletonList(new DataBean()
                        .withClientID(clientId)
                        .withFeedUrl(FEED_URL)
                        .withOfferID(String.valueOf(OFFER_ID))
                        .withStatusModerate(Status.YES)
                        .withReasons(new HashMap<>())));

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();

        assumeThat("в базу записался правильный timestamp", timestampFromDb, equalTo(timestampForResponse));
        assertThat("в базе не появилось записей для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(0));
    }

    @Test
    @Description("В ответе один отклоненный оффер")
    public void testOneRejectedOffer() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Collections.singletonList(new DataBean()
                        .withClientID(clientId)
                        .withFeedUrl(FEED_URL)
                        .withOfferID(String.valueOf(OFFER_ID))
                        .withStatusModerate(Status.NO)
                        .withReasons(reasons)));

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        FeedsRejectedOffersRecord expected = new FeedsRejectedOffersRecord()
                .setClientid(clientId)
                .setExternalOfferId(String.valueOf(OFFER_ID))
                .setReasonsJson(new Gson().toJson(reasons));

        getTimestampAndFeedsRejectedOffersFromDb();

        assumeThat("в базу записался правильный timestamp", timestampFromDb, equalTo(timestampForResponse));
        assumeThat("в базе появилась запись для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(1));
        assertThat("в базе появилась ожидаемая запись", feedsRejectedOffers.get(0).intoMap()
                , beanDiffer(expected.intoMap())
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("В ответе один повторно отклоненный оффер")
    public void testRejectedReasonsUpdated() {
        createRejectedOfferInDb();
        HashMap<String, List<ReasonBean>> anotherReasons = new HashMap<>();
        anotherReasons.put(ReasonBean.TARGET_URL, Collections.singletonList(
                new ReasonBean().withDeclineReasons("{\"anotherItem\":\"anotherReason\"}")));

        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Collections.singletonList(new DataBean()
                        .withClientID(clientId)
                        .withFeedUrl(FEED_URL)
                        .withOfferID(String.valueOf(OFFER_ID))
                        .withStatusModerate(Status.NO)
                        .withReasons(anotherReasons)));

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        FeedsRejectedOffersRecord expected = new FeedsRejectedOffersRecord()
                .setClientid(clientId)
                .setExternalOfferId(String.valueOf(OFFER_ID))
                .setReasonsJson(new Gson().toJson(anotherReasons));

        getTimestampAndFeedsRejectedOffersFromDb();

        assumeThat("в базу записался правильный timestamp", timestampFromDb, equalTo(timestampForResponse));
        assumeThat("в базе появилась запись для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(1));
        assertThat("в базе появилась ожидаемая запись", feedsRejectedOffers.get(0).intoMap()
                , beanDiffer(expected.intoMap())
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("В ответе один принятый оффер, ранее отклоненный")
    public void testAcceptedAfterReject() {
        createRejectedOfferInDb();
        HashMap<String, List<ReasonBean>> anotherReasons = new HashMap<>();
        anotherReasons.put(ReasonBean.TARGET_URL, Collections.singletonList(
                new ReasonBean().withDeclineReasons("{\"anotherItem\":\"anotherReason\"}")));

        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Collections.singletonList(new DataBean()
                        .withClientID(clientId)
                        .withFeedUrl(FEED_URL)
                        .withOfferID(String.valueOf(OFFER_ID))
                        .withStatusModerate(Status.YES)
                        .withReasons(new HashMap<>())));

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();

        assertThat("в базе удалилась запись для clientId=" + clientId, feedsRejectedOffers, iterableWithSize(0));
    }

    @Test
    @Description("В ответе два неотклоненных оффера")
    public void testTwoAccepted() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Arrays.asList(new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(FEED_URL)
                                .withOfferID(String.valueOf(OFFER_ID))
                                .withStatusModerate(Status.YES)
                                .withReasons(new HashMap<>())
                        , new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(RandomUtils.getString(23))
                                .withOfferID(RandomUtils.getString(10))
                                .withStatusModerate(Status.YES)
                                .withReasons(new HashMap<>()))
                );

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();
        timestampForResponse = new Date().toInstant().getEpochSecond();

        assertThat("в базе не появилось записей для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(0));
    }

    @Test
    @Description("В ответе два отклоненных оффера")
    public void testTwoRejected() {
        HashMap<String, List<ReasonBean>> anotherReasons = new HashMap<>();
        anotherReasons.put(ReasonBean.TARGET_URL, Collections.singletonList(
                new ReasonBean().withDeclineReasons("{\"anotherItem\":\"anotherReason\"}")));

        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Arrays.asList(new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(FEED_URL)
                                .withOfferID(String.valueOf(OFFER_ID))
                                .withStatusModerate(Status.NO)
                                .withReasons(reasons)
                        , new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(RandomUtils.getString(23))
                                .withOfferID(RandomUtils.getString(10))
                                .withStatusModerate(Status.NO)
                                .withReasons(anotherReasons))
                );

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();
        timestampForResponse = new Date().toInstant().getEpochSecond();

        assertThat("в базе появилось 2 записи для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(2));
    }

    @Test
    @Description("В ответе два оффера - отклоненный и нет")
    public void testRejectedAndAccepted() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Arrays.asList(new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(FEED_URL)
                                .withOfferID(String.valueOf(OFFER_ID))
                                .withStatusModerate(Status.NO)
                                .withReasons(reasons)
                        , new DataBean()
                                .withClientID(clientId)
                                .withFeedUrl(RandomUtils.getString(23))
                                .withOfferID(RandomUtils.getString(10))
                                .withStatusModerate(Status.YES)
                                .withReasons(new HashMap<>()))
                );

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();
        timestampForResponse = new Date().toInstant().getEpochSecond();

        assertThat("в базе появилась одна запись для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(1));
    }


    private void getTimestampAndFeedsRejectedOffersFromDb() {
        feedsRejectedOffers
                = dbSteps.feedRejectedOffersSteps().getFeedsRejectedOffers(clientId);
        timestampFromDb = api.userSteps.getDarkSideSteps().getDBSteps()
                .getPpcPropertiesSteps().getUpdateFeedsRejectedOffersTimestamp();
    }

    @Step("Выполняем полный цикл вызова скрипта, для получения отклоненного оффера в базе")
    private void createRejectedOfferInDb() {
        PerformanceRejectedOffersResponseBean responseBean = new PerformanceRejectedOffersResponseBean()
                .withTimestamp(timestampForResponse)
                .withData(Collections.singletonList(new DataBean()
                        .withClientID(clientId)
                        .withFeedUrl(FEED_URL)
                        .withOfferID(String.valueOf(OFFER_ID))
                        .withStatusModerate(Status.NO)
                        .withReasons(reasons)));

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean()
                .withObjectIds(Collections.singletonList(timestampFromDb))
                .withResponseEntity(new Gson().toJson(responseBean));

        HELPER.saveFakeBSProxyLogBean(logBean);

        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcFeedsRejectedOffers(getPerfomanceRejectedOffersUrl());

        getTimestampAndFeedsRejectedOffersFromDb();
        timestampForResponse = new Date().toInstant().getEpochSecond();

        assumeThat("в базе появилась запись для clientId=" + clientId
                , feedsRejectedOffers, iterableWithSize(1));
        HELPER.deleteFakeBSProxyLogBeansById(timestampFromDb);
    }
}
