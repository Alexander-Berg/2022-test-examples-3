package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingConditionsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingGoalsRecord;
import ru.yandex.autotests.direct.db.steps.RetargetingConditionSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionGoalItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by semkagtn on 09.07.15.
 * https://st.yandex-team.ru/TESTIRT-6236
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.PPC_RETARGETING_CHECK_GOALS)
@Issue("https://st.yandex-team.ru/DIRECT-43212")
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для недоступной цели")
public class PpcRetargetingCheckGoalsForNonaccessibleGoalTest {

    private static final String LOGIN = Logins.CLIENT_WITH_NONEXISTENT_GOAL;
    private static final int NONACCESSIBLE_GOAL_ID = 12092868;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    private static int shard;
    private static String clientId;
    private int retargetingConditionId;


    /**
     * На случай, если предыдщий запуск теста упал - подчищаем данные.
     * Иначе добавление условия упадет (что условие уже есть)
     * Новыми степами через api5 нльзя добавить условие с недоступной целью
     * а руками через базу добавлять - прямо сейчас совсем не хотелось
     */
    @BeforeClass
    public static void fetchClientInfoAndCleanup() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        RetargetingConditionSteps retargetingConditionSteps = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .retargetingConditionSteps();

        retargetingConditionSteps
                .getUndeletedRetargetingConditionsByClient(Long.parseLong(clientId))
                .stream()
                .filter(r -> r.getConditionJson().matches(".*\\D" + NONACCESSIBLE_GOAL_ID + "\\D.*"))
                .map(RetargetingConditionsRecord::getRetCondId)
                .forEach(retargetingConditionSteps::deleteRetargeingCondition);
    }

    @Before
    @Step("Подготовка данных для теста")
    public void prepareData() {
        int[] retargetingConditionIds = api.userSteps.retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(LOGIN)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.ALL)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(NONACCESSIBLE_GOAL_ID))));
        assumeThat("создалось одно условие ретаргетинга", retargetingConditionIds.length, equalTo(1));
        retargetingConditionId = retargetingConditionIds[0];
    }

    @Test
    public void runScriptAndCheckResult() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible((long) retargetingConditionId, (long) NONACCESSIBLE_GOAL_ID, 1);

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcRetargetingCheckGoals(shard, clientId);

        RetargetingGoalsRecord goal = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .retargetingGoalsSteps().getRetargetingGoals(
                        (long) retargetingConditionId, (long) NONACCESSIBLE_GOAL_ID);
        assumeThat("вернулась запись для условия ретаргетинга", goal, notNullValue());
        assertThat("значение is_accessible соответствует ожидаемому", goal.getIsAccessible(), equalTo(0));
    }
}
