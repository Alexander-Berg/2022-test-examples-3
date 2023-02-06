package ru.yandex.direct.core.entity.retargeting.repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingGoalsRepositoryTest {
    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private RetargetingGoalsRepository retargetingGoalsRepository;

    private Integer shard;

    private RetConditionInfo info1;
    private RetConditionInfo info2;
    private List<Goal> goals1;
    private List<Goal> goals2;

    private Multimap<Long, Long> toDelete;

    @Before
    public void createGoals() {
        toDelete = HashMultimap.create();

        ClientInfo clientInfo = clientSteps.createDefaultClient();
        shard = clientInfo.getShard();
        goals1 = defaultGoals(3);
        goals2 = defaultGoals(2);

        info1 = retConditionSteps.createDefaultRetCondition(goals1, clientInfo);
        info2 = retConditionSteps.createDefaultRetCondition(goals2, clientInfo);

        assumeThat("goals from first condition successfully added",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));
        assumeThat("goals from second condition successfully added",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));
    }

    @Test
    public void nothingDeleteTest() {
        retargetingGoalsRepository.delete(shard, toDelete);

        assertThat("goals from first condition were not deleted",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));

        assertThat("goals from second condition were not deleted",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));
    }

    @Test
    public void singleConditionSingleGoalDeleteTest() {
        toDelete.put(info1.getRetConditionId(), goals1.get(0).getId());
        retargetingGoalsRepository.delete(shard, toDelete);

        assertThat("single goal from first condition was deleted",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.get(1).getId(), goals1.get(2).getId()));

        assertThat("goals from second condition were not deleted",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));
    }

    @Test
    public void singleConditionTwoGoalsDeleteTest() {
        toDelete.put(info1.getRetConditionId(), goals1.get(0).getId());
        toDelete.put(info1.getRetConditionId(), goals1.get(1).getId());
        retargetingGoalsRepository.delete(shard, toDelete);

        assertThat("two goals from first condition were deleted",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.get(2).getId()));

        assertThat("goals from second condition were not deleted",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.stream().map(Goal::getId).collect(Collectors.toList()).toArray()));
    }

    @Test
    public void twoConditionsSingleGoalsDeleteTest() {
        toDelete.put(info1.getRetConditionId(), goals1.get(0).getId());
        toDelete.put(info2.getRetConditionId(), goals2.get(0).getId());
        retargetingGoalsRepository.delete(shard, toDelete);

        assertThat("single goal from first condition was deleted",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.get(1).getId(), goals1.get(2).getId()));

        assertThat("single goal from second condition was deleted",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.get(1).getId()));
    }

    @Test
    public void twoConditionsSeveralGoalsDeleteTest() {
        toDelete.put(info1.getRetConditionId(), goals1.get(0).getId());
        toDelete.put(info1.getRetConditionId(), goals1.get(1).getId());
        toDelete.put(info2.getRetConditionId(), goals2.get(0).getId());
        retargetingGoalsRepository.delete(shard, toDelete);

        assertThat("two goals from first condition were deleted",
                retargetingGoalsRepository.getGoalIds(shard, info1.getRetConditionId()),
                containsInAnyOrder(goals1.get(2).getId()));

        assertThat("single goal from second condition was deleted",
                retargetingGoalsRepository.getGoalIds(shard, info2.getRetConditionId()),
                containsInAnyOrder(goals2.get(1).getId()));
    }

    @After
    public void deleteGoals() {
        DSLContext dslContext = dslContextProvider.ppc(shard);
        retargetingGoalsRepository
                .deleteByRetConditionIds(dslContext, Arrays.asList(info1.getRetConditionId(), info2.getRetConditionId()));
    }
}
