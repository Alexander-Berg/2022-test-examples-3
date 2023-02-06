package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionConverter;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionWeb;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.web.data.TestRetargetingConditionWeb.defaultRetargetingConditionWebForEstimate;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class EstimateRetargetingConditionServiceTest {
    @Autowired
    private EstimateRetargetingConditionService estimateRetargetingConditionService;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Test
    public void estimateRetargetingCondition() {
        RetargetingConditionWeb request = defaultRetargetingConditionWebForEstimate();

        addGoalsToRepositories(request);

        WebResponse response =
                estimateRetargetingConditionService.retargetingConditionEstimate(request);

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }

    private void addGoalsToRepositories(RetargetingConditionWeb request) {
        Collection<Goal> goals = request.getConditions().stream().flatMap(c -> c.getConditionGoalWebs().stream())
                .map(RetargetingConditionConverter::toGoal).collect(toList());
        var user = testAuthHelper.createDefaultUser();
        metrikaClientStub.addGoals(user.getUid(), listToSet(goals, Function.identity()));
        metrikaHelperStub.addGoalIds(user.getUid(),
                goals.stream().filter(goal -> goal.getType().isMetrika()).map(GoalBase::getId).collect(toList()));
        testCryptaSegmentRepository.addAll(
                goals.stream().filter(goal -> !goal.getType().isMetrika()).collect(Collectors.toSet()));
    }
}
