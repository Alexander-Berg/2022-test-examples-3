package ru.yandex.market.core.metrika;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.error.EntityNotFoundException;
import ru.yandex.market.core.metrika.model.Goal;
import ru.yandex.market.core.metrika.model.GoalType;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "MetrikaServiceTest.before.csv")
public class MetrikaServiceTest extends FunctionalTest {

    private static final long SHOP_ID = 1;
    private static final long ACTION_ID = 2;
    private static final long COLLATION_ID = 1;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MetrikaService metrikaService;

    @Test
    @DbUnitDataSet(before = "singleGoal.csv")
    public void getCheckoutGoal() {
        Goal goal = metrikaService.getCheckoutGoal(SHOP_ID);
        MatcherAssert.assertThat(goal, equalTo(new Goal.Builder()
                .setId(1)
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build()));
    }

    @Test
    public void getCheckoutGoalNull() {
        Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> metrikaService.getCheckoutGoal(SHOP_ID)
        );
    }

    @Test
    @DbUnitDataSet(before = "singleGoal.csv")
    public void deleteCheckoutGoal() {
        metrikaService.deleteCheckoutGoal(SHOP_ID, ACTION_ID);
        MatcherAssert.assertThat(countRowsInTable(jdbcTemplate, "shops_web.metrika_counter"), equalTo(0));
    }

    @Test
    public void deleteCheckoutGoalNotFound() {
        Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> metrikaService.deleteCheckoutGoal(SHOP_ID, ACTION_ID)
        );
    }

    @Test
    @DbUnitDataSet(before = "sameGoals.csv")
    public void deleteCheckoutGoalIllegalMethodCall() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> metrikaService.deleteCheckoutGoal(SHOP_ID, ACTION_ID));
    }

    @Test
    @DbUnitDataSet(after = "singleGoal.csv")
    public void saveGoalNew() {
        Goal goal = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoal(goal, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(before = "singleGoal.csv", after = "saveGoalExistent.after.csv")
    public void saveGoalExistent() {
        Goal goal = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cntNew")
                .setGoalId("glNew")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoal(goal, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(before = "singleGoal.csv", after = "saveGoalNullGoal.after.csv")
    public void saveGoalNullGoal() {
        Goal goal = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cntNew")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoal(goal, ACTION_ID);
    }

    @Test
    @DbUnitDataSet(before = "sameGoals.csv")
    public void saveGoalIllegalMethodCall() {
        Goal goal = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cntNew")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> metrikaService.saveGoal(goal, ACTION_ID)
        );
    }

    @Test
    @DbUnitDataSet(before = "severalGoals.csv")
    public void getCheckoutGoals() {
        Goal first = new Goal.Builder()
                .setId(1)
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Goal second = new Goal.Builder()
                .setId(2)
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl2")
                .setType(GoalType.CHECKOUT).build();

        List<Goal> goals = metrikaService.getCheckoutGoals(SHOP_ID);
        MatcherAssert.assertThat(goals, IsIterableContainingInOrder.contains(first, second));
    }

    @Test
    @DbUnitDataSet(before = "severalGoals.csv")
    public void deleteCheckoutGoalFromSeveralGoals() {
        metrikaService.deleteCheckoutGoalById(COLLATION_ID, ACTION_ID);
        MatcherAssert.assertThat(countRowsInTable(jdbcTemplate, "shops_web.metrika_counter"), equalTo(1));
    }

    @Test
    @DbUnitDataSet(after = "severalGoals.csv")
    public void saveGoalsNew() {
        Goal first = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Goal second = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl2")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoals(Arrays.asList(first, second), ACTION_ID, SHOP_ID);
    }

    @Test
    @DbUnitDataSet(before = "severalGoals.csv", after = "severalGoalsExistent.after.csv")
    public void saveGoalsExistent() {

        Goal first = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Goal second = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl2")
                .setType(GoalType.CHECKOUT).build();

        Goal third = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl3")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoals(Arrays.asList(first, second, third), ACTION_ID, SHOP_ID);
    }

    @Test
    @DbUnitDataSet(before = "severalGoals.csv", after = "severalGoalsSave.after.csv")
    public void saveDeleteGoals() {
        Goal first = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Goal third = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl3")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoals(Arrays.asList(first, third), ACTION_ID, SHOP_ID);
    }

    @Test
    @DbUnitDataSet(before = "severalGoalsWithNull.csv", after = "severalGoals.csv")
    public void saveDeleteGoalsWhenExistsNull() {
        Goal first = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl")
                .setType(GoalType.CHECKOUT).build();

        Goal third = new Goal.Builder()
                .setShopId(SHOP_ID)
                .setCounterId("cnt")
                .setGoalId("gl2")
                .setType(GoalType.CHECKOUT).build();

        metrikaService.saveGoals(Arrays.asList(first, third), ACTION_ID, SHOP_ID);
    }

}
