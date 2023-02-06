package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingGoalsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionGoalItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для двух клиентов")
@RunWith(Parameterized.class)
public class PpcRetargetingCheckGoalsForTwoClientsTest {

    private static final String LOGIN1 = Logins.CHECK_GOALS_CLIENT3;
    private static final String LOGIN2 = Logins.CHECK_GOALS_CLIENT4;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_SUPER);

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
    private static String clientId1;
    private static String clientId2;
    private static long goalId1;
    private static long goalId2;

    @BeforeClass
    public static void prepareData() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN1);
        api.userSteps.clientFakeSteps().reshardUser(LOGIN2, shard);
        clientId1 = api.userSteps.clientFakeSteps().getClientData(LOGIN1).getClientID();
        clientId2 = api.userSteps.clientFakeSteps().getClientData(LOGIN2).getClientID();

        List<Long> goals1 = api.userSteps.retargetingSteps().getRetargetingGoalIDs(LOGIN1);
        assumeThat("у пользователя есть цели в метрике", goals1, not(empty()));
        goalId1 = goals1.get(0);

        List<Long> goals2 = api.userSteps.retargetingSteps().getRetargetingGoalIDs(LOGIN2);
        assumeThat("у пользователя есть цели в метрике", goals2, not(empty()));
        goalId2 = goals2.get(0);
    }

    private int retargetingConditionId1;
    private int retargetingConditionId2;

    @Before
    @Step("Подготовка данных для теста")
    public void createRetargetingCondition() {
        int[] retargetingConditionIds1 = api.userSteps.retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(LOGIN1)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.ALL)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(goalId1))));
        assumeThat("было создано одно условие ретаргетинга", retargetingConditionIds1.length, equalTo(1));
        retargetingConditionId1 = retargetingConditionIds1[0];

        int[] retargetingConditionIds2 = api.userSteps.retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(LOGIN2)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.ALL)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(goalId2))));
        assumeThat("было создано одно условие ретаргетинга", retargetingConditionIds2.length, equalTo(1));
        retargetingConditionId2 = retargetingConditionIds2[0];

        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible((long) retargetingConditionId1, goalId1, isAccessible1);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible((long) retargetingConditionId2, goalId2, isAccessible2);
    }

    @Test
    public void checkIsAccessibleValueAfterScript() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcRetargetingCheckGoals(shard, clientId1, clientId2);

        RetargetingGoalsRecord actualGoal1 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .getRetargetingGoals((long) retargetingConditionId1, goalId1);
        assumeThat("вернулась запись для первого условия ретаргетинга", actualGoal1, notNullValue());

        RetargetingGoalsRecord actualGoal2 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .getRetargetingGoals((long) retargetingConditionId1, goalId1);
        assumeThat("вернулась запись для второго условия ретаргетинга", actualGoal2, notNullValue());

        assertThat("значения is_accessible соответствует ожидаемомым", Arrays.asList(
                actualGoal1.getIsAccessible(), actualGoal2.getIsAccessible()), Matchers.contains(1, 1));
    }
}
