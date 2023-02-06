package ru.yandex.direct.web.entity.retargetinglists.service;

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
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionWeb;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingConditionsServiceTest {
    @Autowired
    private UpdateRetargetingConditionsService updateRetargetingConditionsService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private RetConditionSteps retargetingConditionSteps;

    private RetConditionInfo retCondInfo;

    @Before
    public void before() {
        retCondInfo =
                retargetingConditionSteps.createDefaultRetCondition(testAuthHelper.createDefaultUser().getClientInfo());
    }

    @Test
    public void updateRetargetingCondition() {
        RetargetingConditionWeb request = new RetargetingConditionWeb();
        request.setConditionName(retCondInfo.getRetCondition().getName() + "change");
        request.setRetargetingConditionId(retCondInfo.getRetConditionId());

        WebResponse response = updateRetargetingConditionsService.updateCondition(request);

        Assertions.assertThat(response).extracting(WebResponse::isSuccessful).isEqualTo(true);
    }
}
