package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.retargetinglists.model.ReplaceGoal;

import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterCryptaGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterMetrikaGoals;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReplaceGoalsInRetargetingsServiceTest {
    @Autowired
    private ReplaceGoalsInRetargetingsService replaceGoalsInRetargetingsService;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    protected RetConditionSteps retargetingConditionSteps;
    private List<Goal> goals;

    @Before
    public void before() {
        UserInfo user = testAuthHelper.createDefaultUser();
        goals = defaultGoals();
        retargetingConditionSteps.createDefaultRetCondition(goals, user.getClientInfo());
        metrikaClientStub.addGoals(
                user.getUid(),
                new HashSet<>(filterMetrikaGoals(goals))
        );
        testCryptaSegmentRepository.addAll(filterCryptaGoals(goals));
    }

    @Test
    public void replaceGoals() {
        WebResponse response = replaceGoalsInRetargetingsService
                .replaceGoalsInRetargetings(Collections.singletonList(
                        new ReplaceGoal()
                                .withOldGoalId(goals.get(0).getId())
                                .withNewGoalId(goals.get(1).getId()))
                );

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }
}
