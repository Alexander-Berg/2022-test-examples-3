package ru.yandex.direct.core.entity.freelancer.service;

import java.util.Optional;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus.CANCELLEDBYFREELANCER;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class FreelancerRegisterServiceDisableTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    Steps steps;
    @Autowired
    FreelancerService freelancerService;
    @Autowired
    FreelancerRegisterService freelancerRegisterService;

    private FreelancerInfo freelancerInfo;
    private ClientInfo superClient;

    @Before
    public void setUp() throws Exception {
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();

        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
    }

    // Выключение фрилансера

    @Test
    public void disableFreelancer_success() {
        Result<Long> result =
                freelancerRegisterService.disableFreelancer(freelancerInfo.getFreelancerId(), superClient.getUid());
        assertThat(result.isSuccessful())
                .describedAs("Disabling was successful")
                .isTrue();
    }

    @Test
    public void disableFreelancer_success_whenDisabled() {
        freelancerRegisterService.disableFreelancer(freelancerInfo.getFreelancerId(), superClient.getUid());

        Result<Long> result =
                freelancerRegisterService.disableFreelancer(freelancerInfo.getFreelancerId(), superClient.getUid());
        assertThat(result.isSuccessful())
                .describedAs("Disabling was successful")
                .isTrue();
    }

    @Test
    public void disableFreelancer_freelancerMissed() {
        Long freelancerId = freelancerInfo.getFreelancerId();
        freelancerRegisterService.disableFreelancer(freelancerId, superClient.getUid());

        Optional<Freelancer> freelancer =
                freelancerService.getFreelancers(singletonList(freelancerId)).stream().findAny();
        assertThat(freelancer)
                .describedAs("Freelancer after disabling")
                .isNotPresent();
    }

    @Test
    public void disableFreelancer_brokenResult_whenFreelancerNotFound() {
        Long freelancerId = (long) Integer.MAX_VALUE;
        Result<Long> result = freelancerRegisterService.disableFreelancer(freelancerId, superClient.getUid());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(matchesWith(objectNotFound()))));
    }

    @Test
    @Parameters({"NEW", "INPROGRESS"})
    public void disableFreelancer_cancelActiveProjects(FreelancerProjectStatus status) {
        Long freelancerId = freelancerInfo.getFreelancerId();
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        FreelancerProject project = createProjectWithStatus(clientInfo, status);

        freelancerRegisterService.disableFreelancer(freelancerId, superClient.getUid());

        FreelancerProject projectAfterDisabling =
                steps.freelancerSteps().getProject(clientInfo.getShard(), project.getId());
        assertThat(checkNotNull(projectAfterDisabling).getStatus())
                .isEqualTo(CANCELLEDBYFREELANCER);
    }

    private FreelancerProject createProjectWithStatus(ClientInfo clientInfo, FreelancerProjectStatus status) {
        long clientId = clientInfo.getClientId().asLong();
        FreelancerProject project =
                defaultFreelancerProject(clientId, freelancerInfo.getFreelancerId())
                        .withStatus(status);
        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));
        return project;
    }

    // Включение фрилансера

    @Test
    public void enableFreelancer_success() {
        Long freelancerId = freelancerInfo.getFreelancerId();
        Long operatorUid = superClient.getUid();
        Result<Long> disableResult = freelancerRegisterService.disableFreelancer(freelancerId, operatorUid);
        assumeThat(disableResult.isSuccessful(), is(true));

        Result<Long> enableResult = freelancerRegisterService.enableFreelancer(freelancerId, operatorUid);
        assertThat(enableResult.isSuccessful())
                .describedAs("Enabling was successful")
                .isTrue();
    }

    @Test
    public void enableFreelancer_success_whenEnabled() {
        Long freelancerId = freelancerInfo.getFreelancerId();
        Long operatorUid = superClient.getUid();

        Result<Long> enableResult = freelancerRegisterService.enableFreelancer(freelancerId, operatorUid);
        assertThat(enableResult.isSuccessful())
                .describedAs("Enabling was successful")
                .isTrue();
    }

    @Test
    public void enableFreelancer_freelancerReturns() {
        Long freelancerId = freelancerInfo.getFreelancerId();
        Long operatorUid = superClient.getUid();
        Result<Long> disableResult = freelancerRegisterService.disableFreelancer(freelancerId, operatorUid);
        assumeThat(disableResult.isSuccessful(), is(true));

        freelancerRegisterService.enableFreelancer(freelancerId, operatorUid);

        Optional<Freelancer> freelancer =
                freelancerService.getFreelancers(singletonList(freelancerId)).stream().findAny();
        assertThat(freelancer)
                .describedAs("Freelancer after enabling")
                .isPresent();
    }

    @Test
    public void enableFreelancer_brokenResult_whenFreelancerNotFound() {
        Long freelancerId = (long) Integer.MAX_VALUE;
        Result<Long> result = freelancerRegisterService.enableFreelancer(freelancerId, superClient.getUid());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(matchesWith(objectNotFound()))));
    }
}
