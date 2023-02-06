package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.bsapi.BsDbSteps;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 14.09.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка работы BsFront.change_notify в режиме нотификации о модерации." +
        "Креативы в Директе или в BS не готовы к модерации")
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-53877"),
        @Issue("https://st.yandex-team.ru/DIRECT-54553"),
        @Issue("https://st.yandex-team.ru/DIRECT-43716")
})
@RunWith(Parameterized.class)
public class BsFrontChangeNotifyIfCreativeNotReadyToModerateTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static final String CLIENT = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;
    private static final Short HEIGHT_FOR_UPDATE = 15000;

    private static List<Long> createdCreatives;

    @Parameterized.Parameter()
    public String status;

    @Parameterized.Parameter(1)
    public Long creativeId;

    @Parameterized.Parameter(2)
    public String statusAfterUpdate;

    @Parameterized.Parameters(name = "status before update = {0}, creative = {1}, status after update = {2}")
    public static Collection data() {
        CreativesHelper helper = new CreativesHelper(api);

        Long creativeNew = helper.createSomeCreativeInBS().longValue();
        Long creativeYes = helper.createCreativeInBSWithStatusModerate(BsDbSteps.StatusModerate.YES).longValue();
        Long creativeNo = helper.createCreativeInBSWithStatusModerate(BsDbSteps.StatusModerate.NO).longValue();

        createdCreatives = Arrays.asList(creativeNew, creativeYes, creativeNo);

        Object[][] data = new Object[][]{
                {Status.NEW, creativeNew, Status.NEW},
                {Status.NEW, creativeNo, Status.NEW},
                {Status.NEW, creativeYes, Status.NEW},
                {Status.READY, creativeNew, Status.READY},
                {Status.READY, creativeNo, Status.NO},
                {Status.READY, creativeYes, Status.YES},
                {Status.SENT, creativeNew, Status.READY},
                {Status.SENT, creativeNo, Status.NO},
                {Status.SENT, creativeYes, Status.YES},
                {Status.SENDING, creativeNew, Status.READY},
                {Status.SENDING, creativeNo, Status.NO},
                {Status.SENDING, creativeYes, Status.YES},
                {Status.YES, creativeNew, Status.READY},
                {Status.YES, creativeNo, Status.NO},
                {Status.YES, creativeYes, Status.YES},
                {Status.NO, creativeNew, Status.READY},
                {Status.NO, creativeNo, Status.NO},
                {Status.NO, creativeYes, Status.YES},
        };
        return Arrays.asList(data);
    }

    private PerfCreativesRecord perfCreatives;
    private static int shard;
    private static Integer uid;

    @BeforeClass
    public static void getShard() {
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(CLIENT).getPassportID());
        shard = api.userSteps.clientFakeSteps().getUserShard(CLIENT);
    }

    @Before
    public void prepareData() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        if (perfCreatives == null) {
            api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(new BsFrontRequest()
                    .withOperatorUid(uid)
                    .withCreatives(new Creative().withId(creativeId)));
            perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
            assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        }
        perfCreatives.setSumGeo(RegionIDValues.MOSCOW.getId().toString());
        perfCreatives.setStatusmoderate(PerfCreativesStatusmoderate.valueOf(status));
    }

    @Test
    public void callChangeNotifyWithOperator() {
        //DIRECT-45913
        //DIRECT-46357
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
        List<BsFrontChangeNotifyResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotify(
                        new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withCreatives(new Creative().withId(creativeId)));
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        assertThat("креатив имеет правильный статус", perfCreatives.getStatusmoderate(), equalTo(PerfCreativesStatusmoderate.valueOf(statusAfterUpdate)));
        assertThat("данные обновились", perfCreatives.getHeight(), not(equalTo(HEIGHT_FOR_UPDATE)));
    }

    @AfterClass
    public static void cleanup() {
        for (Long creativeId : createdCreatives) {
            api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
        }
    }
}
