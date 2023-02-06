package ru.yandex.direct.grid.processing.service.goal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.goal.GdLalSegment;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdLalSegments;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdLalSegmentsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.test.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.LAL_SEGMENT;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoalsForLals;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GoalMutationGraphQLLalCreationTest {

    private final static String MUTATION_NAME = "createLalSegmentForGoal";
    private final static String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult{\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "    segments {\n"
            + "      id\n"
            + "      name\n"
            + "      parentId\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdLalSegments, GdLalSegmentsPayload>
            CREATE_LAL_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
            GdLalSegments.class, GdLalSegmentsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    private User user;
    private List<Goal> parentGoals;
    private Map<Long, Goal> lalByParentId;

    @Before
    public void before() {
        var userInfo = steps.userSteps().createDefaultUser();
        user = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(user);

        parentGoals = defaultMetrikaGoalsForLals();
        parentGoals.forEach(parent -> parent.setName(TestUtils.randomName("", 10)));
        metrikaClientStub.addGoals(user.getUid(), Set.copyOf(parentGoals));

        var lals = mapList(parentGoals, parent -> (Goal) defaultGoalByType(LAL_SEGMENT).withParentId(parent.getId()));
        lalByParentId = listToMap(lals, Goal::getParentId);
        doReturn(lals).
                when(metrikaGoalsService).createLalSegments(mapList(parentGoals, GoalBase::getId));
    }

    @Test
    public void testLalSegmentsCreation() {
        var input = new GdLalSegments()
                .withParentGoalIds(mapList(parentGoals, GoalBase::getId));
        var result = processor.doMutationAndGetPayload(CREATE_LAL_MUTATION, input, user);

        assertThat(result.getValidationResult()).isNull();
        assertThat(result.getSegments()).hasSize(parentGoals.size());

        var expected = mapList(parentGoals,
                parent -> new GdLalSegment()
                        .withId(lalByParentId.get(parent.getId()).getId())
                        .withName(parent.getName())
                        .withParentId(parent.getId()));

        assertThat(result.getSegments()).is(matchedBy(beanDiffer(expected)));
    }
}
