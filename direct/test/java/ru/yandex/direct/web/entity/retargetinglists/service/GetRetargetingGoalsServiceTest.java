package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.retargeting.MetrikaGoalWeb;

import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRetargetingGoalsServiceTest {
    @Autowired
    private GetRetargetingGoalsService getRetargetingGoalsService;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Before
    public void before() {
        metrikaClientStub.addGoals(
                testAuthHelper.createDefaultUser().getUid(),
                new HashSet<>(defaultMetrikaGoals())
        );
    }

    @Test
    public void getGoalsForRetargeting() {
        List<MetrikaGoalWeb> response = getRetargetingGoalsService.getMetrikaGoalsForRetargeting();

        Assertions.assertThat(response).isNotEmpty();
    }
}
