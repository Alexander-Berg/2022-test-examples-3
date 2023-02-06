package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionConverter;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionWeb;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.web.data.TestRetargetingConditionWeb.defaultRetargetingConditionWeb;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreateRetargetingConditionsServiceTest {

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private CreateRetargetingConditionsService createRetargetingConditionsService;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Test
    public void createRetargetingCondition() {
        RetargetingConditionWeb request = defaultRetargetingConditionWeb();

        addGoalsToRepositories(request);

        WebResponse response = createRetargetingConditionsService.createCondition(request);

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }

    private void addGoalsToRepositories(RetargetingConditionWeb request) {
        Collection<Goal> goals = request.getConditions().stream().flatMap(c -> c.getConditionGoalWebs().stream())
                .map(RetargetingConditionConverter::toGoal).collect(toList());
        metrikaHelperStub.addGoalIds(testAuthHelper.createDefaultUser().getUid(),
                goals.stream().filter(goal -> goal.getType().isMetrika()).map(GoalBase::getId).collect(toList()));
        testCryptaSegmentRepository.addAll(
                goals.stream().filter(goal -> !goal.getType().isMetrika()).collect(Collectors.toSet()));
    }
}
