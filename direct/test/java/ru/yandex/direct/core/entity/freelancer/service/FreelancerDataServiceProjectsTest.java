package ru.yandex.direct.core.entity.freelancer.service;

import java.util.EnumSet;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectIdentity;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CommonDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class FreelancerDataServiceProjectsTest {

    @Autowired
    Steps steps;

    @Autowired
    FreelancerService testedService;

    private TestContextManager testContextManager = new TestContextManager(FreelancerDataServiceProjectsTest.class);

    private ClientInfo clientInfo;
    private FreelancerInfo freelancerInfo;

    @Before
    public void setUp() throws Exception {
        testContextManager.prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientInfo freelancerClient = steps.clientSteps().createClient(new ClientInfo().withShard(2));
        freelancerInfo =
                steps.freelancerSteps().createFreelancer(new FreelancerInfo().withClientInfo(freelancerClient));
    }

    @Test
    public void requestFreelancerService() {
        Result<Long> result = testedService
                .requestFreelancerService(clientInfo.getChiefUserInfo().getUser(),
                        freelancerInfo.getFreelancer().getFreelancerId());

        FreelancerProject actualProject =
                steps.freelancerSteps().getProject(clientInfo.getShard(), result.getResult());

        FreelancerProject expected = new FreelancerProject()
                .withClientId(clientInfo.getClientId().asLong())
                .withFreelancerId(freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.NEW);

        assertThat(actualProject).is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void requestFreelancerService_clientHasOpenProject_exception() {

        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong());

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        Result<Long> result = testedService
                .requestFreelancerService(clientInfo.getChiefUserInfo().getUser(),
                        freelancerInfo.getFreelancer().getFreelancerId());

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(field("operator")),
                        FreelancerDefects.ProjectsDefects.PROJECTS_ARE_ALREADY_EXIST
                ))));
    }

    private Object incorrectRolesForClient() {
        EnumSet<RbacRole> roles = EnumSet.allOf(RbacRole.class);
        roles.remove(RbacRole.CLIENT);
        return roles.toArray();
    }

    @Test
    @Parameters(method = "incorrectRolesForClient")
    public void requestFreelancerService_incorrectRole(RbacRole role) {

        Result<Long> result = testedService
                .requestFreelancerService(clientInfo.getChiefUserInfo().getUser().withRole(role),
                        freelancerInfo.getFreelancer().getFreelancerId());

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(field("operator")),
                        FreelancerDefects.mustBeClient()
                ))));
    }

    @Test
    public void requestFreelancerService_freelancerNotExist() {

        Result<Long> result = testedService
                .requestFreelancerService(clientInfo.getChiefUserInfo().getUser(), clientInfo.getClientId().asLong());

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(field("freelancerId")),
                        FreelancerDefects.mustBeFreelancer()
                ))));
    }

    @Test
    public void acceptFreelancerProject() {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong());

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        testedService.acceptFreelancerProject(
                new FreelancerProject().withId(project.getId())
                        .withClientId(clientInfo.getClientId().asLong())
                        .withFreelancerId(freelancerInfo.getClientId().asLong()));

        FreelancerProject actualProject = steps.freelancerSteps().getProject(clientInfo.getShard(), project.getId());

        FreelancerProject expected = new FreelancerProject()
                .withClientId(clientInfo.getClientId().asLong())
                .withFreelancerId(freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.INPROGRESS);

        assertThat(actualProject).is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    private Object incorrectStatusesForAccept() {
        return new Object[]{
                FreelancerProjectStatus.INPROGRESS,
                FreelancerProjectStatus.CANCELLEDBYCLIENT,
                FreelancerProjectStatus.CANCELLEDBYFREELANCER
        };
    }

    @Test
    @Parameters(method = "incorrectStatusesForAccept")
    public void acceptFreelancerProject_incorrectStatus(FreelancerProjectStatus status) {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong()).withStatus(status);

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        Result<FreelancerProjectIdentity> result = testedService.acceptFreelancerProject(
                new FreelancerProject().withId(project.getId())
                        .withClientId(clientInfo.getClientId().asLong())
                        .withFreelancerId(freelancerInfo.getClientId().asLong()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(),
                        FreelancerDefects.StatusDefects.TRANSITION_IS_NOT_AVAILABLE
                ))));
    }

    @Test
    public void acceptFreelancerProject_incorrectProject() {

        Result<FreelancerProjectIdentity> result = testedService.acceptFreelancerProject(new FreelancerProject());

        MatcherAssert.assertThat(result.getValidationResult(), CoreMatchers.allOf(
                hasDefectDefinitionWith(validationError(path(field(FreelancerProjectIdentity.FREELANCER_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.CLIENT_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.ID)),
                        CommonDefects.notNull()))
        ));
    }

    @Test
    public void cancelFreelancerProjectByClient() {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong()).withStatus(FreelancerProjectStatus.INPROGRESS);

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        testedService.cancelFreelancerProjectByClient(
                new FreelancerProject().withId(project.getId())
                        .withClientId(clientInfo.getClientId().asLong())
                        .withFreelancerId(freelancerInfo.getClientId().asLong()));

        FreelancerProject actualProject = steps.freelancerSteps().getProject(clientInfo.getShard(), project.getId());

        FreelancerProject expected = new FreelancerProject()
                .withClientId(clientInfo.getClientId().asLong())
                .withFreelancerId(freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.CANCELLEDBYCLIENT);

        assertThat(actualProject).is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    private Object incorrectStatusesForCancel() {
        return new Object[]{
                FreelancerProjectStatus.CANCELLEDBYCLIENT,
                FreelancerProjectStatus.CANCELLEDBYFREELANCER
        };
    }

    @Test
    @Parameters(method = "incorrectStatusesForCancel")
    public void cancelFreelancerProjectByClient_incorrectStatuses(FreelancerProjectStatus status) {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong()).withStatus(status);

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        Result<FreelancerProjectIdentity> result =
                testedService.cancelFreelancerProjectByClient(
                        new FreelancerProject().withId(project.getId())
                                .withClientId(clientInfo.getClientId().asLong())
                                .withFreelancerId(freelancerInfo.getClientId().asLong()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(),
                        FreelancerDefects.StatusDefects.TRANSITION_IS_NOT_AVAILABLE
                ))));
    }

    @Test
    public void cancelFreelancerProjectByClient_incorrectProject() {

        Result<FreelancerProjectIdentity> result =
                testedService.cancelFreelancerProjectByClient(new FreelancerProject());

        MatcherAssert.assertThat(result.getValidationResult(), CoreMatchers.allOf(
                hasDefectDefinitionWith(validationError(path(field(FreelancerProjectIdentity.FREELANCER_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.CLIENT_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.ID)),
                        CommonDefects.notNull()))
        ));
    }

    @Test
    public void cancelFreelancerProjectByFreelancer() {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong()).withStatus(FreelancerProjectStatus.INPROGRESS);

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        testedService.cancelFreelancerProjectByFreelancer(
                new FreelancerProject().withId(project.getId())
                        .withClientId(clientInfo.getClientId().asLong())
                        .withFreelancerId(freelancerInfo.getClientId().asLong()));

        FreelancerProject actualProject = steps.freelancerSteps().getProject(clientInfo.getShard(), project.getId());

        FreelancerProject expected = new FreelancerProject()
                .withClientId(clientInfo.getClientId().asLong())
                .withFreelancerId(freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.CANCELLEDBYFREELANCER);

        assertThat(actualProject).is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Parameters(method = "incorrectStatusesForCancel")
    public void cancelFreelancerProjectByFreelancer_incorrectStatuses(FreelancerProjectStatus status) {
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong()).withStatus(status);

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));

        Result<FreelancerProjectIdentity> result =
                testedService.cancelFreelancerProjectByFreelancer(
                        new FreelancerProject().withId(project.getId())
                                .withClientId(clientInfo.getClientId().asLong())
                                .withFreelancerId(freelancerInfo.getClientId().asLong()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(),
                        FreelancerDefects.StatusDefects.TRANSITION_IS_NOT_AVAILABLE
                ))));
    }

    @Test
    public void cancelFreelancerProjectByFreelancer_incorrectProject() {

        Result<FreelancerProjectIdentity> result =
                testedService.cancelFreelancerProjectByFreelancer(new FreelancerProject());

        MatcherAssert.assertThat(result.getValidationResult(), CoreMatchers.allOf(
                hasDefectDefinitionWith(validationError(path(field(FreelancerProjectIdentity.FREELANCER_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.CLIENT_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(field(FreelancerProjectIdentity.ID)),
                        CommonDefects.notNull()))
        ));
    }
}
