package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectIdentity;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.projectNotFound;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.transitionIsNotAvailable;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.error;

public class FreelancerValidationServiceTransitionsTest {
    private static final long VALID_ID = 1L;

    private FreelancerValidationService freelancerValidationService;

    ShardHelper shardHelper = mock(ShardHelper.class);

    FreelancerProjectRepository freelancerProjectRepository = mock(FreelancerProjectRepository.class);
    FreelancerRepository freelancerRepository = mock(FreelancerRepository.class);
    FeatureService featureService = mock(FeatureService.class);

    @Before
    public void setUp() {
        doReturn(1).when(shardHelper).getShardByClientId(any());
        freelancerValidationService =
                new FreelancerValidationService(shardHelper, freelancerProjectRepository, freelancerRepository,
                        featureService);
    }

    @Test
    public void validateProjectRequest_absentProject() {
        FreelancerProjectIdentity projectIdentity = new FreelancerProject();
        Map<Long, FreelancerProject> existingProjects = new HashMap<>();

        ValidationResult<Long, Defect> vr = freelancerValidationService
                .validateChangeProjectStatusByFreelancer(projectIdentity, FreelancerProjectStatus.NEW,
                        existingProjects);

        assertThat(vr).has(error(projectNotFound()));
    }


    @Test
    public void validateProjectRequest_InvalidTransitionFreelancer() {
        FreelancerProject project =
                new FreelancerProject().withId(VALID_ID).withStatus(FreelancerProjectStatus.INPROGRESS);
        Map<Long, FreelancerProject> existingProjects = new HashMap<>();
        existingProjects.put(project.getId(), project);

        ValidationResult<Long, Defect> vr = freelancerValidationService
                .validateChangeProjectStatusByFreelancer(project, FreelancerProjectStatus.NEW, existingProjects);

        assertThat(vr).has(error(transitionIsNotAvailable(project.getStatus())));
    }

    @Test
    public void validateProjectRequest_InvalidTransitionClient() {
        FreelancerProject project =
                new FreelancerProject().withId(VALID_ID).withStatus(FreelancerProjectStatus.INPROGRESS);
        Map<Long, FreelancerProject> existingProjects = new HashMap<>();
        existingProjects.put(project.getId(), project);

        ValidationResult<Long, Defect> vr = freelancerValidationService
                .validateChangeProjectStatusByClient(project, FreelancerProjectStatus.NEW, existingProjects);

        assertThat(vr).has(error(transitionIsNotAvailable(project.getStatus())));
    }
}
