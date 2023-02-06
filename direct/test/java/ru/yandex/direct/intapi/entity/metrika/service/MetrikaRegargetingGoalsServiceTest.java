package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultGoal;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaRegargetingGoalsServiceTest {

    private static final Long INVALID_DIVISOR = 0L;
    private static final Long INVALID_REMINDER = -1L;

    @Autowired
    private MetrikaRetargetingGoalsService metrikaRetargetingGoalsService;

    @Autowired
    private Steps steps;

    @Test
    public void getGoalIds() {
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();
        Long id1 = 5L;
        Long id2 = 6L;

        steps.retargetingGoalsSteps().addGoal(retargetingInfo, defaultGoal(id1));
        steps.retargetingGoalsSteps().addGoal(retargetingInfo, defaultGoal(id2));

        List<Long> ids = metrikaRetargetingGoalsService.getGoals(5L, 1L);
        assumeThat("В списке результатов должен присутствовать минимум один элемент",
                ids, hasSize(greaterThan(0)));
        assertThat("В ответе должны присутствовать ожидаемые данные",
                new HashSet<>(ids).contains(6L), is(true));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaRetargetingGoalsService.getGoals(INVALID_DIVISOR, INVALID_REMINDER);
    }
}
