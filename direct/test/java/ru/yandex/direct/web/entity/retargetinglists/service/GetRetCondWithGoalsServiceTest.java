package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
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

import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterCryptaGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterMetrikaGoals;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRetCondWithGoalsServiceTest {
    @Autowired
    private GetRetCondWithGoalsService getRetCondWithGoalsService;

    @Autowired
    MetrikaClientStub metrikaClientStub;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private RetConditionSteps retargetingConditionSteps;

    public void before() {
        UserInfo user = testAuthHelper.createDefaultUser();
        List<Goal> goals = defaultGoals();
        retargetingConditionSteps.createDefaultRetCondition(goals, user.getClientInfo());
        metrikaClientStub.addGoals(
                user.getUid(),
                new HashSet<>(filterMetrikaGoals(goals))
        );
        testCryptaSegmentRepository.addAll(filterCryptaGoals(goals));
    }

    @Test
    public void getRetCondWithGoalsService() {
        WebResponse response = getRetCondWithGoalsService.getRetCondWithGoals(
                null, null, null, null);

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }

}
