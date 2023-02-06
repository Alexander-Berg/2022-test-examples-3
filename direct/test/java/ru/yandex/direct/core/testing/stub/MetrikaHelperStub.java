package ru.yandex.direct.core.testing.stub;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.MetrikaHelper;

import static java.util.stream.Collectors.toSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Если при запуске теста возникает ошибка
 * 'metrikaHelperStub'; nested exception is org.springframework.beans.factory.NoSuchBeanDefinitionException
 * то стоит попробовать переместить MetrikaHelperStub на последнее место в списке Autowired
 */
public class MetrikaHelperStub extends MetrikaHelper {

    private final ConcurrentHashMap<Long, User> usersByUids = new ConcurrentHashMap<>();

    public MetrikaHelperStub() {
        super(null);
    }

    public MetrikaHelperStub(MetrikaClient metrikaClient) {
        super(metrikaClient);
    }

    public void addGoalIds(long uid, Collection<Long> goals) {
        getUser(uid).addGoals(goals);
    }

    public void addGoals(long uid, Collection<Goal> goals) {
        if (getMetrikaClient() != null) {
            var client = (MetrikaClientStub) getMetrikaClient();
            var g = StreamEx.of(goals)
                    .filter(t -> t.getType().isMetrika())
                    .toSet();
            client.addGoals(uid, g);
        }
        getUser(uid).addGoals(mapList(goals, GoalBase::getId));
    }

    public void addGoalsFromConditions(long uid, List<RetargetingCondition> conditions) {
        addGoalIds(uid, extractGoalIdsFromConditions(conditions));
    }

    public void addGoalsFromRules(long uid, List<Rule> conditionRules) {
        if (getMetrikaClient() != null) {
            var client = (MetrikaClientStub) getMetrikaClient();
            var goals = StreamEx.of(conditionRules)
                    .map(Rule::getGoals)
                    .nonNull()
                    .flatMap(Collection::stream)
                    .nonNull()
                    .filter(g -> g.getType().isMetrika())
                    .toSet();
            client.addGoals(uid, goals);
        }
        addGoalIds(uid, extractGoalIdsFromRules(conditionRules));
    }

    private User getUser(long uid) {
        return usersByUids.computeIfAbsent(uid, User::new);
    }

    private Set<Long> extractGoalIdsFromConditions(List<RetargetingCondition> conditions) {
        return conditions.stream()
                .map(cond -> extractGoalIdsFromRules(cond.getRules()))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private Set<Long> extractGoalIdsFromRules(List<Rule> conditionRules) {
        return conditionRules.stream()
                .map(Rule::getGoals)
                .map(goals -> mapList(goals, Goal::getId))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }


    private static class User {
        long uid;
        Set<Long> goals = new HashSet<>();

        public User(long uid) {
            this.uid = uid;
        }

        public Set<Long> getGoals() {
            return goals;
        }

        public void addGoals(Collection<Long> goals) {
            this.goals.addAll(goals);
        }
    }
}
