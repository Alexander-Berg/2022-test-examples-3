package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingGoalsRecord;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
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
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для одного клиента с двумя условиями ретаргетинга")
@RunWith(Parameterized.class)
public class PpcRetargetingCheckGoalsForOneClientTwoRetargetingConditionsTest {

    private static final String LOGIN = Logins.CHECK_GOALS_CLIENT2;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(0)
    public int isAccessible1;

    @Parameterized.Parameter(1)
    public int isAccessible2;

    @Parameterized.Parameters(name = "is_accessible for first = {0}, is_accessible for second = {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, 0},
                {0, 1},
                {1, 1},
        });
    }

    private static int shard;
    private static String clientId;
    private static long goalId1;
    private static long goalId2;

    @BeforeClass
    public static void prepareData() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        api.userSteps.deleteAllRetargetingListsByLogin(LOGIN);

        List<Long> goals = api.userSteps.retargetingSteps().getRetargetingGoalIDs(LOGIN);
        assumeThat("у пользователя есть не менее двух целей в метрике", goals, hasSize(greaterThanOrEqualTo(2)));
        goalId1 = goals.get(1);
        goalId2 = goals.get(2);
    }

    private int retargetingConditionId1;
    private int retargetingConditionId2;

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
                                                .withGoalID(goalId1))),
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(LOGIN)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.ALL)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(goalId2))));
        assumeThat("было создано два условия ретаргетинга", retargetingConditionIds.length, equalTo(2));
        retargetingConditionId1 = retargetingConditionIds[0];
        retargetingConditionId2 = retargetingConditionIds[1];

        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible((long) retargetingConditionId1, goalId1, isAccessible1);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible((long) retargetingConditionId2, goalId2, isAccessible2);
    }

    @Test
    public void checkIsAccessibleValueAfterScript() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcRetargetingCheckGoals(shard, clientId);

        RetargetingGoalsRecord actualGoal1 = api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .getRetargetingGoals((long) retargetingConditionId1, goalId1);
        assumeThat("вернулась запись для первого условия ретаргетинга", actualGoal1, notNullValue());

        RetargetingGoalsRecord actualGoal2 = api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .getRetargetingGoals((long) retargetingConditionId2, goalId2);
        assumeThat("вернулась запись для второго условия ретаргетинга", actualGoal2, notNullValue());

        assertThat("значения is_accessible соответствует ожидаемомым", Arrays.asList(
                actualGoal1.getIsAccessible(), actualGoal2.getIsAccessible()), Matchers.contains(1, 1));
    }
}
