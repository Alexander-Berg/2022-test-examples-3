package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetConditions;
import static ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType.metrika_goals;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceGetTest extends BaseRetargetingConditionServiceTest {

    private List<Long> retConditionIds;

    @Before
    public void before() {
        super.before();
        // добавляем условия, которые будем доставать через сервис
        List<RetargetingCondition> retConditions = defaultRetConditions(clientId, 2);
        retConditionIds = retConditionRepository.add(shard, retConditions);
    }

    @Test
    public void getRetargetingConditions_NullIds_AllConditions() {
        List<RetargetingCondition> fetchedConditions = retargetingConditionService
                .getRetargetingConditions(clientId, null,
                        singleton(metrika_goals), LimitOffset.maxLimited());
        List<Long> fetchedConditionIds = mapList(fetchedConditions, RetargetingCondition::getId);
        assertThat(fetchedConditionIds, containsInAnyOrder(retConditionIds.toArray()));
    }

    @Test
    public void getRetargetingConditions_ExistingIds_ConditionsInDefinedOrder() {
        List<Long> idsToSelect = asList(retConditionIds.get(0), retConditionIds.get(1));
        List<RetargetingCondition> fetchedConditions =
                retargetingConditionService.getRetargetingConditions(clientId, idsToSelect, singleton(metrika_goals),
                        LimitOffset.maxLimited());
        List<Long> fetchedConditionIds = mapList(fetchedConditions, RetargetingCondition::getId);
        assertThat(fetchedConditionIds, contains(idsToSelect.toArray()));
    }

    @Test
    public void getRetargetingConditions_NotExistingIds_EmptyList() {
        List<Long> idsToSelect = asList(12345L, 67890L);
        List<RetargetingCondition> fetchedConditions =
                retargetingConditionService.getRetargetingConditions(clientId, idsToSelect, singleton(metrika_goals),
                        LimitOffset.maxLimited());
        assertThat(fetchedConditions, emptyIterable());
    }
}
