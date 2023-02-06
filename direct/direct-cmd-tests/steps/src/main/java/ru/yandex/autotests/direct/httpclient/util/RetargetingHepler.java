package ru.yandex.autotests.direct.httpclient.util;

import com.google.common.collect.Iterators;
import ru.yandex.autotests.direct.httpclient.data.retargeting.Condition;
import ru.yandex.autotests.direct.httpclient.data.retargeting.Goal;
import ru.yandex.autotests.direct.httpclient.data.retargeting.Retargeting;
import ru.yandex.autotests.directapi.common.api45.RetargetingCondition;
import ru.yandex.autotests.directapi.common.api45.RetargetingConditionGoalItem;
import ru.yandex.autotests.directapi.common.api45.RetargetingConditionItem;

import java.util.*;

import static ru.yandex.autotests.irt.testutils.beans.BeanHelper.getOnlyFields;

/**
 * Created by shmykov on 30.09.14.
 */
public class RetargetingHepler {

    public static Retargeting convertToFormParameters(RetargetingCondition apiBean) {
        Retargeting retargeting = new Retargeting();
        retargeting.setRetargetingConditionID(String.valueOf(apiBean.getRetargetingConditionID()));
        retargeting.setConditionName(apiBean.getRetargetingConditionName());
        retargeting.setConditionDesc(apiBean.getRetargetingConditionDescription());
        List<Condition> conditions = new ArrayList<>();
        for (RetargetingConditionItem beanCondition : apiBean.getRetargetingCondition()) {
            conditions.add(convertConditionItem(beanCondition));
        }
        retargeting.setConditions(conditions);
        return retargeting;
    }

    private static Condition convertConditionItem(RetargetingConditionItem beanCondition) {
        Condition condition = new Condition();
        condition.setType(beanCondition.getType());
        List<Goal> goals = new ArrayList<>();
        for (RetargetingConditionGoalItem beanGoal : beanCondition.getGoals()) {
            goals.add(convertGoal(beanGoal));
        }
        condition.setGoals(goals);
            return condition;
    }

    private static Goal convertGoal(RetargetingConditionGoalItem beanGoal) {
        Goal goal = new Goal();
        goal.setGoalId(String.valueOf(beanGoal.getGoalID()));
        goal.setTime(String.valueOf(beanGoal.getTime()));
        return goal;
    }

    public static void fillNullGoalIds(Retargeting retargeting, List<Long> goalIds) {
        if (retargeting.getConditions() == null) {
            return;
        }
        Iterator goalsList = Iterators.cycle(goalIds);
        for (Condition condition : retargeting.getConditions()) {
            if (condition == null || condition.getGoals() == null) {
                continue;
            }
            for (Goal goal : condition.getGoals()) {
                if (goal != null && goal.getGoalId() == null) {
                    goal.setGoalId(String.valueOf(goalsList.next()));
                }
            }
        }
    }

    public static Retargeting getDeepCopy(Retargeting src) {
        Retargeting dst = new Retargeting();
        dst = getOnlyFields(src, "retargetingConditionID", "conditionName", "conditionDesc");
        if (src.getConditions() == null) {
            return dst;
        }
        dst.setConditions(new ArrayList<Condition>());
        for (Condition srcCondition : src.getConditions()) {
            Condition dstCondition = new Condition();
            dstCondition = getOnlyFields(srcCondition, "type");
            dst.getConditions().add(dstCondition);
            if (srcCondition.getGoals() == null) {
                continue;
            }
            dstCondition.setGoals(new ArrayList<Goal>());
            for (Goal srcGoal : srcCondition.getGoals()) {
                Goal dstGoal = new Goal();
                dstGoal = getOnlyFields(srcGoal, "goalId", "time");
                dstCondition.getGoals().add(dstGoal);
            }
        }
        return dst;
    }

    public static void addConditionWithGoalTo(Retargeting retargeting) {
        if (retargeting.getConditions() == null) {
            return;
        }
        retargeting.getConditions().add(new Condition());
        int last = retargeting.getConditions().size() - 1;
        retargeting.getConditions().get(last).setGoals(new ArrayList<Goal>());
        addGoalToCondition(retargeting.getConditions().get(last));
    }

    public static void addGoalToCondition(Condition condition) {
        if (condition.getGoals() == null) {
            return;
        }
        condition.getGoals().add(new Goal());
        int last = condition.getGoals().size() - 1;
        Random randomTime = new Random();
        condition.getGoals().get(last).setTime("30");
    }
}
