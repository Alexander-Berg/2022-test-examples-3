package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.bsapi.Templates;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.directapi.darkside.steps.BsFrontSteps.getExpectedPerfCreativesElseRecord;
import static ru.yandex.autotests.directapi.darkside.steps.BsFrontSteps.getExpectedPerfCreativesRecord;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

/**
 * Created by pavryabov on 27.08.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Создание и обновление креативов вызовом BsFront.change_notify")
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-53877"),
        @Issue("https://st.yandex-team.ru/DIRECT-43716")
})
@RunWith(Parameterized.class)
public class BsFrontChangeNotifyTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static final String PREVIEW_URL = "preview_url";
    private static final String MODERATE_SEND_TIME = "moderate_send_time";
    private static final String MODERATE_TRY_COUNT = "moderate_try_count";
    private static final String TEMPLATE_ID = "template_id";
    private static final Short HEIGHT_FOR_UPDATE = 15000;
    private static final String NAME_FOR_UPDATE = "someNewName";

    private static final String CLIENT = Logins.LOGIN_MAIN;
    private static int shard;
    private static Long creativeId;
    private static Long creativeIdElse;

    private static Long clientId;
    private Integer uid;

    @Parameterized.Parameter(value = 0)
    public String operator;

    @Parameterized.Parameter(value = 1)
    public String client;

    @Parameterized.Parameters(name = "operator = {0}, client = {1}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {CLIENT, null},
                {Logins.LOGIN_SUPER, CLIENT},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void init() {
        CreativesHelper helper = new CreativesHelper(api);

        PerfCreativesRecord expectedRecord = getExpectedPerfCreativesRecord(0L, 0L, 0L);
        PerfCreativesRecord expectedElseRecord = getExpectedPerfCreativesElseRecord(0L, 0L, 0L);

        creativeId = helper.createCreativeInBS(Templates.TEMPLATE_240x400,
                expectedRecord.getName(), expectedRecord.getHref()).longValue();
        creativeIdElse = helper.createCreativeInBS(Templates.TEMPLATE_300x250, expectedElseRecord.getName(),
                expectedElseRecord.getHref()).longValue();

        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(CLIENT);
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        clientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(CLIENT).getClientID());
    }

    @Before
    public void prepareData() {
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operator).getPassportID());
    }

    @Test
    public void createOneCreative() {
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).creativesShardingSteps()
                .deleteCreativeClientLink(creativeId);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @Test
    public void createTwoCreatives() {
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).creativesShardingSteps()
                .deleteCreativeClientLink(creativeId);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeIdElse);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).creativesShardingSteps()
                .deleteCreativeClientLink(creativeIdElse);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId), new Creative().withId(creativeIdElse));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        BsFrontChangeNotifyResponse expectedResponseElse = new BsFrontChangeNotifyResponse();
        expectedResponseElse.setId(creativeIdElse);
        expectedResponseElse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse, expectedResponseElse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("первый креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));

        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeIdElse);
        expectedCreative = getExpectedPerfCreativesElseRecord(clientId, creativeIdElse,
                perfCreatives.getCreativeGroupId());
        assertThat("второй креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @Test
    public void createWithOneValidAndOneNotFoundCreatives() {
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).creativesShardingSteps()
                .deleteCreativeClientLink(creativeId);
        long notFoundCreativeId = RandomUtils.getRandomInteger(1, 100000000);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId), new Creative().withId(notFoundCreativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        BsFrontChangeNotifyResponse expectedResponseElse = new BsFrontChangeNotifyResponse();
        expectedResponseElse.setId(notFoundCreativeId);
        expectedResponseElse.setResult(0);
        expectedResponseElse.setErrorMessage(BsFrontChangeNotifyResponse.CREATIVE_NOT_FOUND);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse, expectedResponseElse)));

        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @Test
    public void updateOneCreative() {
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        if (perfCreatives == null) {
            Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(CLIENT).getPassportID());
            api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(new BsFrontRequest()
                    .withOperatorUid(uid)
                    .withCreatives(new Creative().withId(creativeId)));
            perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
            assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        }
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        perfCreatives.setName(NAME_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Collections.singletonList(expectedResponse)));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }


    @Test
    public void updateTwoCreatives() {
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        assumeThat("первый креатив есть в базе", perfCreatives, notNullValue());
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        perfCreatives.setName(NAME_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeIdElse);
        assumeThat("второй креатив есть в базе", perfCreatives, notNullValue());
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        perfCreatives.setName(NAME_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId), new Creative().withId(creativeIdElse));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        BsFrontChangeNotifyResponse expectedResponseElse = new BsFrontChangeNotifyResponse();
        expectedResponseElse.setId(creativeIdElse);
        expectedResponseElse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse, expectedResponseElse)));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("первый креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));

        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeIdElse);
        expectedCreative = getExpectedPerfCreativesElseRecord(clientId, creativeIdElse,
                perfCreatives.getCreativeGroupId());
        assertThat("второй креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @Test
    public void createOneCreativeTwoTimesInRequest() {
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).creativesShardingSteps()
                .deleteCreativeClientLink(creativeId);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId), new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse, expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @Test
    public void updateOneCreativeTwoTimesInRequest() {
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        if (perfCreatives == null) {
            Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(CLIENT).getPassportID());
            api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(new BsFrontRequest()
                    .withOperatorUid(uid)
                    .withCreatives(new Creative().withId(creativeId)));
            perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
            assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        }
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        perfCreatives.setName(NAME_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId), new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assertThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse, expectedResponse)));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        PerfCreativesRecord expectedCreative = getExpectedPerfCreativesRecord(clientId, creativeId,
                perfCreatives.getCreativeGroupId());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(PREVIEW_URL), newPath(MODERATE_SEND_TIME), newPath(MODERATE_TRY_COUNT),
                        newPath(TEMPLATE_ID))
                .useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                perfCreatives, recordDiffer(expectedCreative).useCompareStrategy(strategy));
    }

    @AfterClass
    public static void cleanup() {
        for (Long creativeIdToDelete : Arrays.asList(creativeId, creativeIdElse)) {
            api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeIdToDelete);
        }
    }
}
