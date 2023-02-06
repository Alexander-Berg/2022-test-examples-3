package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
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
@Description("Проверка изменения статуса креатива после редактирования")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class CheckStatusModerateAfterEditTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static final String CLIENT = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

    private static Long creativeId;
    private static Timestamp firstModerateSendTime;

    private static final Integer DELAY_MINUTES = 10;
    private static final Short HEIGHT_FOR_UPDATE = 15000;
    private static final String PATTERN_FOR_CHECK_MODERATE_SEND_TIME = "yyyy-MM-dd hh:mm";
    //проверяем с точность до минуты, чтобы не было шума

    private static int shard;
    private static int uid;

    @Parameterized.Parameter()
    public String status;

    @Parameterized.Parameters(name = "status before update = {0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {Status.READY},
                {Status.SENT},
                {Status.YES},
                {Status.NO},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void getShard() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(CLIENT);
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(CLIENT).getPassportID());
        creativeId = new CreativesHelper(api).createSomeCreativeInBS().longValue();
    }

    @Before
    public void prepareData() {
        int shard = api.userSteps.clientFakeSteps().getUserShard(CLIENT);
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        if (perfCreatives == null) {
            api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(new BsFrontRequest()
                    .withOperatorUid(uid)
                    .withCreatives(new Creative().withId(creativeId)));
            perfCreatives = api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
            firstModerateSendTime = perfCreatives.getModerateSendTime();
            assumeThat("креатив есть в базе", perfCreatives, notNullValue());
        }
        perfCreatives.setSumGeo(RegionIDValues.MOSCOW.getId().toString());
        perfCreatives.setStatusmoderate(PerfCreativesStatusmoderate.valueOf(status));
        perfCreatives.setHeight(HEIGHT_FOR_UPDATE);
        api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreatives);
    }

    @Test
    public void checkCreativeStatus() {
        //DIRECT-46333
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withCreatives(new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        assertThat("статус креатива = Ready",
                perfCreatives.getStatusmoderate(), equalTo(PerfCreativesStatusmoderate.Ready));
    }

    @Test
    public void checkModerateSendTime() {
        //DIRECT-46333
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withCreatives(new Creative().withId(creativeId));
        List<BsFrontChangeNotifyResponse> bsFrontChangeNotifyResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest);
        LocalDateTime changeTime = LocalDateTime.now();
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId);
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                bsFrontChangeNotifyResponse, beanDiffer(Arrays.asList(expectedResponse)));
        PerfCreativesRecord perfCreatives =
                api.userSteps.getDirectJooqDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        LocalDateTime moderateSendTime = perfCreatives.getModerateSendTime().toLocalDateTime();

        if (status.equals(Status.READY)) {
            assertThat("кретиву проставился правильный moderateSendTime", perfCreatives.getModerateSendTime(),
                    equalTo(firstModerateSendTime));
        } else {
            assertThat("кретиву проставился правильный moderateSendTime",
                    (moderateSendTime.isAfter(changeTime.plusMinutes(DELAY_MINUTES - 1))
                            && moderateSendTime.isBefore(changeTime.plusMinutes(DELAY_MINUTES + 1))),
                    equalTo(true));
        }
    }
}
