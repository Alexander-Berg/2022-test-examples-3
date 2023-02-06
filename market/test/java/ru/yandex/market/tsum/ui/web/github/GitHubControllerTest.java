package ru.yandex.market.tsum.ui.web.github;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.GitHubSettings;
import ru.yandex.market.tsum.release.dao.GithubMergeSettings;
import ru.yandex.market.tsum.release.dao.GithubMergeSettingsDao;
import ru.yandex.market.tsum.release.dao.ReleasePipeInfo;
import ru.yandex.market.tsum.ui.auth.TsumAuthentication;
import ru.yandex.market.tsum.ui.web.TestGitHubClientContext;
import ru.yandex.market.tsum.ui.web.github.view_model.GithubMergeSettingsViewModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, ProjectsDao.class, GithubMergeSettingsDao.class,
    GitHubController.class, TestGitHubClientContext.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GitHubControllerTest {
    private final static String ALLOWED_ROLE = "ALLOWED_ROLE";

    @Autowired
    private GitHubController controller;

    @Autowired
    private GithubMergeSettingsDao githubMergeSettingsDao;

    @Autowired
    private ProjectsDao projectsDao;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Before
    public void setup() {
        githubMergeSettingsDao.save(new GithubMergeSettings("repo1", true));
        githubMergeSettingsDao.save(new GithubMergeSettings("repo2", true));
        githubMergeSettingsDao.save(new GithubMergeSettings("repo3", false));
        githubMergeSettingsDao.save(new GithubMergeSettings("repo4", true));

        List<ProjectEntity> projects = Arrays.asList(
            createProjectEntity(
                "project1",
                Arrays.asList(
                    createDeliveryMachine(
                        "repo1",
                        "stage-group-id-1"
                    )
                )
            ),
            createProjectEntity(
                "project2",
                Arrays.asList(
                    createDeliveryMachine(
                        "repo2",
                        "stage-group-id-2"
                    )
                )
            ),
            createProjectEntity(
                "project3",
                Arrays.asList(
                    createDeliveryMachine(
                        "repo2",
                        "stage-group-id-3"
                    )
                )
            ),
            createProjectEntity(
                "project4",
                Arrays.asList(
                    createDeliveryMachine(
                        "repo3",
                        "stage-group-id-4"
                    ),
                    createDeliveryMachine(
                        "repo4",
                        "stage-group-id-5"
                    )
                )
            )
        );

        mongoTemplate.insertAll(projects);
    }

    @Test
    public void getMergeSettings__oneProject() {
        checkGetMergeSettings(ImmutableMap.of(
            "repo1", 1
        ));
    }

    @Test
    public void getMergeSettings__oneRepoTwoProjects() {
        checkGetMergeSettings(ImmutableMap.of(
            "repo2", 2
        ));
    }

    @Test
    public void getMergeSettings__oneRepoProjectWithTwoDeliveryMachines() {
        checkGetMergeSettings(ImmutableMap.of(
            "repo3", 1
        ));
    }

    @Test
    public void getMergeSettings__allRepos() {
        checkGetMergeSettings(ImmutableMap.of(
            "repo1", 1,
            "repo2", 2,
            "repo3", 1,
            "repo4", 1
        ));
    }

    @Test
    public void getMergeSettings__nonExistingRepository() {
        List<GithubMergeSettingsViewModel> viewModels = controller.getMergeSettings("non-existing");
        assertEquals(0, viewModels.size());
    }

    @Test
    public void updateMergeSettings__notAllowedWithoutPermissions() {
        TsumUser user = new TsumUser("userWithoutAccess", Collections.emptySet());
        setSecurityContextWithUser(user);

        ResponseEntity<?> responseEntity = controller.updateMergeSettings("repo1", false);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    }

    @Test
    public void updateMergeSettings__allowedWithPermissions() {
        TsumUser user = new TsumUser("userWithAccess", Sets.newHashSet(ALLOWED_ROLE));
        setSecurityContextWithUser(user);

        ResponseEntity<?> responseEntity = controller.updateMergeSettings("repo1", false);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void updateMergeSettings__shouldUpdateRepoSettings() {
        TsumUser user = new TsumUser("userWithAccess", Sets.newHashSet(ALLOWED_ROLE));
        setSecurityContextWithUser(user);

        String repositoryId = "repo2";

        checkRepositoryAutomergeSettings(repositoryId, true);

        ResponseEntity<?> responseEntity = controller.updateMergeSettings(repositoryId, false);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        checkRepositoryAutomergeSettings(repositoryId, false);
    }

    private void checkRepositoryAutomergeSettings(String repositoryId, boolean automergeEnabled) {
        List<GithubMergeSettingsViewModel> viewModels = controller.getMergeSettings(repositoryId);

        GithubMergeSettingsViewModel viewModel = viewModels.get(0);

        assertEquals(automergeEnabled, viewModel.isAutomergeEnabled());
    }

    private void checkGetMergeSettings(Map<String, Integer> repositoryId2DeliveryMachineCountMap) {
        String repositoryIds = String.join(",", repositoryId2DeliveryMachineCountMap.keySet());

        List<GithubMergeSettingsViewModel> viewModels = controller.getMergeSettings(repositoryIds);

        // check if count of viewModels match count of requested repository ids
        assertEquals(repositoryId2DeliveryMachineCountMap.keySet().size(), viewModels.size());

        repositoryId2DeliveryMachineCountMap.forEach((String repositoryId, Integer expectedDeliveryMachineCount) -> {
            List<GithubMergeSettingsViewModel.DeliveryMachineInfo> deliveryMachineInfos =
                getDeliveryMachinesForRepository(viewModels, repositoryId);

            assertEquals((int) expectedDeliveryMachineCount, deliveryMachineInfos.size());
        });
    }

    private List<GithubMergeSettingsViewModel.DeliveryMachineInfo> getDeliveryMachinesForRepository(
        List<GithubMergeSettingsViewModel> viewModels, String repositoryId) {

        return viewModels.stream()
            .filter(vm -> vm.getRepositoryId().equals(repositoryId))
            .flatMap(m -> m.getDeliveryMachineInfoList().stream())
            .collect(Collectors.toList());
    }

    private DeliveryMachineEntity createDeliveryMachine(String repositoryId, String stageGroupId) {
        GitHubSettings gitHubSettings = new GitHubSettings(repositoryId);


        ReleasePipeInfo pipeInfo = mock(ReleasePipeInfo.class);
        DeliveryMachineSettings settings = DeliveryMachineSettings.builder()
            .withStageGroupId(stageGroupId)
            .withGithubSettings(gitHubSettings)
            .withPipeline(pipeInfo)
            .build();

        return new DeliveryMachineEntity(settings, mongoTemplate.getConverter());
    }

    private ProjectEntity createProjectEntity(String projectId, List<DeliveryMachineEntity> deliveryMachines) {
        ProjectEntity projectEntity = new ProjectEntity(
            projectId,
            projectId,
            deliveryMachines
        );

        projectEntity.setAllowedRoles(Arrays.asList(ALLOWED_ROLE));

        return projectEntity;
    }

    private static void setSecurityContextWithUser(TsumUser user) {
        TsumAuthentication authentication = mock(TsumAuthentication.class);
        when(authentication.getDetails()).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
