package ru.yandex.direct.grid.core.entity.retergeting;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingCondition;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingConditionFilter;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingConditionRuleItem;
import ru.yandex.direct.grid.core.entity.showcondition.repository.GridRetargetingYtRepository;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingConditionService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingConditionService.DEFAULT_AUDIENCE_DOMAIN;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
public class GridRetargetingConditionServiceTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Long GOAL_ID = RandomNumberUtils.nextPositiveLong();
    private static final String NAME = "name";
    private static final String DOMAIN = "ya.ru";
    public static final int SHARD = 1;

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @Mock
    private GridRetargetingYtRepository retargetingYtRepository;

    @Mock
    private RetargetingGoalsRepository retargetingGoalsRepository;

    @Mock
    private RetargetingRepository retargetingRepository;

    @InjectMocks
    private GridRetargetingConditionService gridRetargetingConditionService;

    private Goal goal;

    @Before
    public void before() {
        goal = new Goal();
        goal.setId(GOAL_ID);
        goal.setDomain(DOMAIN);
        goal.setName(NAME);
    }

    @Test
    public void getRetargetingConditions() {
        mockRetargetingConditionServiceMethods();
        List<GdiRetargetingCondition> expectedConditions = mockGetRetargetingConditions();
        expectedConditions.get(0).setGoalDomains(singleton(DOMAIN));
        List<GdiRetargetingCondition> actualRetargetingConditions = gridRetargetingConditionService
                .getRetargetingConditions(SHARD, CLIENT_ID, new GdiRetargetingConditionFilter());

        assertThat(actualRetargetingConditions)
                .is(matchedBy(beanDiffer(expectedConditions)));
    }

    @Test
    public void getRetargetingConditions_EmptyGoalDomains_GoalDomainsEmpty() {
        goal.setDomain("");
        mockRetargetingConditionServiceMethods();
        mockGetRetargetingConditions();

        List<GdiRetargetingCondition> actualRetargetingConditions = gridRetargetingConditionService
                .getRetargetingConditions(SHARD, CLIENT_ID, new GdiRetargetingConditionFilter());

        assertThat(actualRetargetingConditions.get(0).getGoalDomains())
                .isEmpty();
    }


    @Test
    public void getRetargetingConditions_EmptyAudienceGoalDomain_GoalDomainsIsNotEmpty() {
        goal.setDomain("");
        goal.setId(METRIKA_AUDIENCE_UPPER_BOUND - 1);
        mockRetargetingConditionServiceMethods();
        mockGetRetargetingConditions();

        List<GdiRetargetingCondition> actualRetargetingConditions = gridRetargetingConditionService
                .getRetargetingConditions(SHARD, CLIENT_ID, new GdiRetargetingConditionFilter());

        assertThat(actualRetargetingConditions.get(0).getGoalDomains())
                .isNotEmpty();
        assertThat(actualRetargetingConditions.get(0).getGoalDomains()).contains(DEFAULT_AUDIENCE_DOMAIN);
    }

    private void mockRetargetingConditionServiceMethods() {
        doReturn(singletonList(goal))
                .when(retargetingConditionService)
                .getAvailableMetrikaGoalsForRetargeting(any(), anyCollection());
        doReturn(singletonList(goal))
                .when(retargetingConditionService)
                .getMetrikaGoalsForRetargeting(any(), any());
    }

    private List<GdiRetargetingCondition> mockGetRetargetingConditions() {
        GdiRetargetingCondition gdiRetargetingCondition = new GdiRetargetingCondition()
                .withRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withConditionRules(singletonList(new GdiRetargetingConditionRuleItem()
                        .withGoals(singletonList(goal))
                        .withType(RuleType.ALL)));
        List<GdiRetargetingCondition> conditions = Collections.singletonList(gdiRetargetingCondition);
        doReturn(conditions)
                .when(retargetingYtRepository)
                .getRetargetingConditions(anyInt(), any());
        return conditions;
    }

}
