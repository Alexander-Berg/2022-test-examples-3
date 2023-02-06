package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.logic.ppc.RetargetingGoals;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionGoalItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by semkagtn on 08.07.15.
 * https://st.yandex-team.ru/TESTIRT-6236
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.PPC_RETARGETING_CHECK_GOALS)
@Issue("https://st.yandex-team.ru/DIRECT-43212")
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для одного клиента")
@RunWith(Parameterized.class)
public class PpcRetargetingCheckGoalsForOneClientOneRetargetingConditionTest {

    private static final String LOGIN = Logins.CHECK_GOALS_CLIENT;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(0)
    public int isAccessible;

    @Parameterized.Parameters(name = "is_accessible = {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0},
                {1},
        });
    }

    private static int shard;
    private static String clientId;
    private static long goalId;

    @BeforeClass
    public static void prepareData() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        api.userSteps.deleteAllRetargetingListsByLogin(LOGIN);
        List<Long> goals = api.userSteps.retargetingSteps().getRetargetingGoalIDs(LOGIN);
        assumeThat("у пользователя есть цели в метрике", goals, not(empty()));
        goalId = goals.get(0);

    }

    private int retargetingConditionId;

    @Before
    @Step("Подготовка данных для теста")
    public void createRetargetingCondition() {
        int[] retargetingConditionIds = api.userSteps.retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(LOGIN)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.ALL)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(goalId))));
        assumeThat("было создано одно условие ретаргетинга", retargetingConditionIds.length, equalTo(1));
        retargetingConditionId = retargetingConditionIds[0];

        api.userSteps.getDBSteps().getRetargetingGoalsSteps().setIsAccessible(
                retargetingConditionId, goalId, isAccessible, shard);
    }

    @Test
    public void checkIsAccessibleValueAfterScript() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcRetargetingCheckGoals(shard, clientId);

        RetargetingGoals actualGoal = api.userSteps.getDBSteps().getRetargetingGoalsSteps().getRetargetingGoals(
                retargetingConditionId, goalId, shard);
        assumeThat("вернулась запись для условия ретаргетинга", actualGoal, notNullValue());

        assertThat("значение is_accessible соответствует ожидаемому", actualGoal.getIsAccessible(), equalTo(1));
    }
}
