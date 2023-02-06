package ru.yandex.direct.core.entity.freelancer.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerDataServiceGetProjectsByFreelancerTest {

    @Autowired
    Steps steps;

    @Autowired
    FreelancerService testedService;

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private FreelancerInfo freelancerInfo;

    @Before
    public void setUp() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(2));
        ClientInfo freelancerClient = steps.clientSteps().createClient(new ClientInfo().withShard(2));
        freelancerInfo =
                steps.freelancerSteps().createFreelancer(new FreelancerInfo().withClientInfo(freelancerClient));
    }

    @Test
    public void acceptFreelancerProject() {
        FreelancerProject project1 = defaultFreelancerProject(clientInfo1.getClientId().asLong(),
                freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.INPROGRESS)
                .withStartedTime(LocalDateTime.now().withNano(0));
        FreelancerProject project2 = defaultFreelancerProject(clientInfo2.getClientId().asLong(),
                freelancerInfo.getClientId().asLong());

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo1)
                        .withProject(project1));

        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo2)
                        .withProject(project2));

        List<FreelancerProject> resultProjects = testedService
                .getFreelancersProjects(Collections.singletonList(freelancerInfo.getClientId().asLong()));
        List<FreelancerProject> expectedProjects = asList(project1, project2);

        resultProjects.sort(Comparator.comparing(FreelancerProject::getId));
        expectedProjects.sort(Comparator.comparing(FreelancerProject::getId));

        assertThat(resultProjects)
                .has(matchedBy(beanDiffer(expectedProjects).useCompareStrategy(onlyExpectedFields())));
    }
}
