package ru.yandex.direct.grid.processing.service.freelancer;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerPrivateData;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class FreelancerDataServiceGetPrivateDataTest {
    private final TestContextManager testContextManager = new TestContextManager(FreelancerDataServiceTest.class);

    @Autowired
    Steps steps;

    @Autowired
    FreelancerDataService testedService;

    private FreelancerInfo freelancerInfo;
    private User freelancerUser;

    @Before
    public void setUp() throws Exception {
        this.testContextManager.prepareTestInstance(this);
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        freelancerUser = UserHelper.getUser(freelancerInfo.getClientInfo().getClient());

    }

    @Test
    public void getPrivateData_operatorFreelancer_success() {
        GdFreelancerPrivateData actual =
                testedService.getPrivateData(freelancerInfo.getFreelancer().getFreelancerId(), freelancerUser);
        assertThat(actual).describedAs("privateData")
                .isNotNull();
        assertThat(actual.getFreelancerId()).describedAs("privateData.freelancerId")
                .isEqualTo(freelancerInfo.getFreelancerId());
    }

    @Test
    public void getPrivateData_operatorOtherClient_null() {
        User otherUser = steps.userSteps().createDefaultUser().getUser();
        GdFreelancerPrivateData actual =
                testedService.getPrivateData(freelancerInfo.getFreelancer().getFreelancerId(), otherUser);
        assertThat(actual).describedAs("privateData")
                .isNull();
    }
}
