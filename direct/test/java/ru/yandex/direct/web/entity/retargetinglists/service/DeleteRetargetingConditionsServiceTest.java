package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteRetargetingConditionsServiceTest {
    @Autowired
    private DeleteRetargetingConditionsService deleteRetargetingConditionsService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    protected RetConditionSteps retargetingConditionSteps;
    private RetConditionInfo retCondInfo;

    @Before
    public void before() {
        retCondInfo =
                retargetingConditionSteps.createDefaultRetCondition(testAuthHelper.createDefaultUser().getClientInfo());
    }

    @Test
    public void deleteRetargetingCondition() {
        List<Long> request = Collections.singletonList(retCondInfo.getRetConditionId());

        WebResponse response =
                deleteRetargetingConditionsService.deleteConditions(request);

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }
}
