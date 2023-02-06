package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import ru.yandex.autotests.directapi.logic.ppc.PerfCreatives;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.DefaultMatchVariation;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.MatchVariation;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.directapi.darkside.steps.BsFrontSteps.getExpectedPerfCreatives;
import static ru.yandex.autotests.directapi.darkside.steps.BsFrontSteps.getExpectedPerfCreativesElse;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

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

    private static final String PREVIEW_URL = "previewUrl";
    private static final String MODERATE_SEND_TIME = "moderateSendTime";
    private static final String MODERATE_TRY_COUNT = "moderate_try_count";
    private static final Short HEIGHT_FOR_UPDATE = 15000;
    private static final String NAME_FOR_UPDATE = "someNewName";
    private static final String TEMPLATE_ID = "templateId";

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

        PerfCreatives expected = getExpectedPerfCreatives(0L, 0L);
        PerfCreatives expectedElse = getExpectedPerfCreativesElse(0L, 0L);

        creativeId = helper.createCreativeInBS(Templates.TEMPLATE_240x400,
                expected.getName(), expected.getHref()).longValue();
        creativeIdElse = helper.createCreativeInBS(Templates.TEMPLATE_300x250, expectedElse.getName(),
                expectedElse.getHref()).longValue();

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
        PerfCreatives expectedCreative = getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative =
                getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("первый креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeIdElse);
        expectedCreative = getExpectedPerfCreativesElse(clientId, creativeIdElse);
        assertThat("второй креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative = getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative =
                getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative =
                getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("первый креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeIdElse);
        expectedCreative = getExpectedPerfCreativesElse(clientId, creativeIdElse);
        assertThat("второй креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative =
                getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
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
        PerfCreatives expectedCreative =
                getExpectedPerfCreatives(clientId, creativeId);
        MatchVariation variation = new DefaultMatchVariation()
                .forFields(PREVIEW_URL, MODERATE_SEND_TIME, MODERATE_TRY_COUNT, TEMPLATE_ID).useMatcher(notNullValue());
        assertThat("креатив был правильно сохранен в базу",
                convert2(perfCreatives), beanDiffer(expectedCreative).withVariation(variation));
    }

    private static PerfCreatives convert2(PerfCreativesRecord from) {
        PerfCreatives r = new PerfCreatives();
        r.setCreativeId(BigInteger.valueOf(from.getCreativeId()));
        r.setStockCreativeId(BigInteger.valueOf(from.getStockCreativeId()));
        r.setClientId(from.getClientid());
        r.setCreativeType(from.getCreativeType().toString());
        r.setBusinessType(from.getBusinessType().toString());
        r.setName(from.getName());
        r.setWidth(from.getWidth().intValue());
        r.setHeight(from.getHeight().intValue());
        r.setHref(from.getHref());
        r.setStatusModerate(from.getStatusmoderate().toString());
        r.setPreviewUrl(from.getPreviewUrl());
        r.setModerateSendTime(from.getModerateSendTime().toString());
        r.setModerateTryCount(from.getModerateTryCount().intValue());
        r.setTemplateId(0L);
        return r;
    }
}
