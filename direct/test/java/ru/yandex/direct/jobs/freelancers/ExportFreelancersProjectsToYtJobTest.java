package ru.yandex.direct.jobs.freelancers;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.ENABLE_EXPORT_FREELANCERS_PROJECTS_TO_YT;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


@JobsTest
@ExtendWith(SpringExtension.class)
class ExportFreelancersProjectsToYtJobTest {
    @Autowired
    private Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private FreelancerProjectRepository freelancerProjectRepository;
    @Autowired
    private FreelancerService freelancerService;
    @Autowired
    private UserService userService;

    private YtProvider ytProvider;
    private ExportFreelancersProjectsToYtJob job;

    private FreelancerInfo freelancerInfo;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private FreelancerProject project1;
    private FreelancerProject project2;

    @BeforeEach
    void before() {
        ytProvider = mock(YtProvider.class);
        when(ytProvider.getClusterConfig(any())).thenReturn(mock(YtClusterConfig.class));

        DirectExportYtClustersParametersSource parametersSourceMock =
                mock(DirectExportYtClustersParametersSource.class);
        when(parametersSourceMock.convertStringToParam(any())).thenReturn(YtCluster.HAHN);

        job = new ExportFreelancersProjectsToYtJob(
                ppcPropertiesSupport,
                parametersSourceMock,
                ytProvider,
                shardHelper,
                freelancerProjectRepository,
                freelancerService,
                userService);

        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();

        clientInfo1 = steps.clientSteps().createDefaultClient();
        clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(2));

        project1 = addProject(clientInfo1, FreelancerProjectStatus.INPROGRESS, true);
        project2 = addProject(clientInfo2, FreelancerProjectStatus.CANCELLEDBYCLIENT, true);
    }

    @Test
    void isJobEnabled_whenPropertyIsFalse() {
        setEnableExportProperty(false);
        boolean isJobEnabled = job.isJobEnabled();
        assertThat(isJobEnabled, is(false));
    }

    @Test
    void isJobEnabled_whenPropertyIsTrue() {
        setEnableExportProperty(true);
        boolean isJobEnabled = job.isJobEnabled();
        assertThat(isJobEnabled, is(true));
    }

    @Test
    void getFreelancersProjectsTest() {
        FreelancerProject project3 = addProject(clientInfo1, FreelancerProjectStatus.NEW, false);
        FreelancerProject project4 = addProject(clientInfo1, FreelancerProjectStatus.CANCELLEDBYCLIENT, false);
        FreelancerProject project5 = addProject(clientInfo2, FreelancerProjectStatus.CANCELLEDBYFREELANCER, true);
        FreelancerProject project6 = addProject(clientInfo2, FreelancerProjectStatus.CANCELLEDBYFREELANCER, false);

        List<FreelancerProject> projects = job.getFreelancersProjects(singletonList(freelancerInfo.getFreelancerId()));
        assertThat(projects, containsInAnyOrder(project1, project2, project5));
    }

    @Test
    void convertToYtTest() {
        List<FreelancerProject> projects = job.getFreelancersProjects(singletonList(freelancerInfo.getFreelancerId()));
        List<YtFreelancerProject> ytProjects = job.convertToYt(projects);

        String freelancerLogin = freelancerInfo.getClientInfo().getLogin();
        Freelancer freelancer = freelancerInfo.getFreelancer();
        String freelancerName = freelancer.getFirstName() + " " + freelancer.getSecondName();

        List<YtFreelancerProject> expectedProjects = asList(
                new YtFreelancerProject()
                        .withProjectId(project1.getId())
                        .withFreelancerClientId(freelancer.getId())
                        .withCustomerClientId(project1.getClientId())
                        .withStartDate(project1.getStartedTime())
                        .withFinishDate(null)
                        .withFreelancerName(freelancerName)
                        .withFreelancerUid(freelancerInfo.getClientInfo().getUid())
                        .withFreelancerLogin(freelancerLogin),

                new YtFreelancerProject()
                        .withProjectId(project2.getId())
                        .withFreelancerClientId(freelancer.getId())
                        .withCustomerClientId(project2.getClientId())
                        .withStartDate(project2.getStartedTime())
                        .withFinishDate(project2.getUpdatedTime())
                        .withFreelancerName(freelancerName)
                        .withFreelancerUid(freelancerInfo.getClientInfo().getUid())
                        .withFreelancerLogin(freelancerLogin));

        assertThat(ytProjects,
                containsInAnyOrder(mapList(expectedProjects, expectedProject ->
                        beanDiffer(expectedProject).useCompareStrategy(allFields()))));
    }

    @Test
    void execute_whenTableDontExist_createTable() {
        setEnableExportProperty(true);

        // mock для ytProvider -> ytOperator -> yt -> cypress
        YtOperator ytOperator = mockYtOperator(ytProvider);
        Cypress cypress = mockCypress(ytOperator);
        YtDynamicOperator ytDynamicOperator = mockYtDynamicOperator(ytProvider);

        // таблица не существует
        when(cypress.exists(any(YPath.class))).thenReturn(false);

        job.execute();
        verify(cypress).create(any(), eq(CypressNodeType.TABLE), eq(true), eq(false), any());
        verify(ytOperator).mount(any(), anyInt());
        verify(ytDynamicOperator).runInTransaction(any());
    }

    @Test
    void execute_whenTableExists_dontCreateTable() {
        setEnableExportProperty(true);

        // mock для ytProvider -> ytOperator -> yt -> cypress
        YtOperator ytOperator = mockYtOperator(ytProvider);
        Cypress cypress = mockCypress(ytOperator);
        YtDynamicOperator ytDynamicOperator = mockYtDynamicOperator(ytProvider);

        // таблица существует
        when(cypress.exists(any(YPath.class))).thenReturn(true);

        job.execute();
        verify(cypress, never()).create(any(), eq(CypressNodeType.TABLE), eq(true), eq(false), any());
        verify(ytOperator, never()).mount(any(), anyInt());
        verify(ytDynamicOperator).runInTransaction(any());
    }

    private YtOperator mockYtOperator(YtProvider ytProvider) {
        YtOperator ytOperator = mock(YtOperator.class);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);
        return ytOperator;
    }

    private Cypress mockCypress(YtOperator ytOperator) {
        Yt yt = mock(Yt.class);
        when(ytOperator.getYt()).thenReturn(yt);

        Cypress cypress = mock(Cypress.class);
        when(yt.cypress()).thenReturn(cypress);
        return cypress;
    }

    private YtDynamicOperator mockYtDynamicOperator(YtProvider ytProvider) {
        YtDynamicOperator ytDynamicOperator = mock(YtDynamicOperator.class);
        when(ytProvider.getDynamicOperator(any())).thenReturn(ytDynamicOperator);
        YtClusterConfig ytClusterConfig = mock(YtClusterConfig.class);
        when(ytProvider.getClusterConfig(any())).thenReturn(ytClusterConfig);
        when(ytClusterConfig.getHome()).thenReturn("//home/direct");
        return ytDynamicOperator;
    }

    private void setEnableExportProperty(boolean isEnabled) {
        String value = isEnabled ? "1" : "0";
        ppcPropertiesSupport.set(ENABLE_EXPORT_FREELANCERS_PROJECTS_TO_YT.getName(), value);
    }

    private FreelancerProject addProject(ClientInfo clientInfo, FreelancerProjectStatus status, boolean isStarted) {
        FreelancerProject project = defaultFreelancerProject(
                clientInfo.getClientId().asLong(), freelancerInfo.getClientId().asLong()
        ).withStatus(status);

        if (isStarted) {
            project.withStartedTime(LocalDateTime.now().withNano(0));
        }
        steps.freelancerSteps().createProject(
                new FreelancerProjectInfo()
                        .withFreelancerInfo(freelancerInfo)
                        .withClientInfo(clientInfo)
                        .withProject(project));
        return project;
    }
}
