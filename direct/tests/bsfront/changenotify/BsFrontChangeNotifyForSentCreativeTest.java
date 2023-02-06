package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.bsapi.BsDbSteps;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 15.09.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка работы BsFront.change_notify в режиме нотификации о модерации. " +
        "Креативы в Директе и BS готовы к модерации")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontChangeNotifyForSentCreativeTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static final String CLIENT = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;
    private static final Short HEIGHT_FOR_UPDATE = 15000;
    private static int shard;

    @Parameterized.Parameter(value = 0)
    public Long creativeId;

    @Parameterized.Parameter(value = 1)
    public String expectedStatus;

    @Parameterized.Parameter(value = 2)
    public String client;

    @Parameterized.Parameters(name = "creativeId = {0}, expectedStatus = {1}, client = {2}")
    public static Collection strategies() {
        CreativesHelper helper = new CreativesHelper(api);

        Long creativeYes = helper.createCreativeInBSWithStatusModerate(BsDbSteps.StatusModerate.YES).longValue();
        Long creativeNo = helper.createCreativeInBSWithStatusModerate(BsDbSteps.StatusModerate.NO).longValue();

        Object[][] data = new Object[][]{
                {creativeYes, Status.YES, null},
                {creativeNo, Status.NO, null},
                {creativeYes, Status.YES, Logins.LOGIN_FOR_RUB},
                {creativeNo, Status.NO, Logins.LOGIN_FOR_RUB},
        };
        return Arrays.asList(data);
    }

    private PerfCreativesRecord perfCreatives;

    @BeforeClass
    public static void getShard() {
        shard = api.userSteps.clientFakeSteps().getUserShard(CLIENT);
    }

    @Before
    public void prepareData() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        if (perfCreatives == null) {
            Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(CLIENT).getPassportID());
            api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(new BsFrontRequest()
                    .withOperatorUid(uid)
                    .withCreatives(new Creative().withId(creativeId)));
            perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
            assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        }
        perfCreatives.setSumGeo(RegionIDValues.MOSCOW.getId().toString());
        perfCreatives.setStatusmoderate(PerfCreativesStatusmoderate.Sent);
    }

    @Test
    public void callWithCreativeIfDataNotChanged() {
        //DIRECT-46348
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        List<BsFrontChangeNotifyResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotify(
                        new BsFrontRequest()
                                .withClientLogin(client)
                                .withCreatives(new Creative().withId(creativeId)));
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        assertThat(String.format("статус креатива = %s", expectedStatus),
                perfCreatives.getStatusmoderate(), equalTo(PerfCreativesStatusmoderate.valueOf(expectedStatus)));
    }

    @Test
    public void callWithCreativeIfDataChanged() {
        //DIRECT-46348
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        List<BsFrontChangeNotifyResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotify(
                        new BsFrontRequest()
                                .withClientLogin(client)
                                .withCreatives(new Creative().withId(creativeId)));
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        assertThat(String.format("статус креатива = %s", expectedStatus),
                perfCreatives.getStatusmoderate(), equalTo(PerfCreativesStatusmoderate.valueOf(expectedStatus)));
        assertThat("данные обновились", perfCreatives.getHeight(), not(equalTo(HEIGHT_FOR_UPDATE)));
    }
}
