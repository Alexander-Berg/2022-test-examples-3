package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.agencyClientCantRequestService;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.clientIsAlreadyFreelancer;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.mustBeClient;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.mustBeFreelancer;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.projectsAreAlreadyExist;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.error;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;

public class FreelancerValidationServiceProjectRequestTest {
    private static final long VALID_ID = 1L;
    private static final long INVALID_ID = -1L;

    private FreelancerValidationService freelancerValidationService;

    private ShardHelper shardHelper = mock(ShardHelper.class);

    private FreelancerProjectRepository freelancerProjectRepository = mock(FreelancerProjectRepository.class);
    private FreelancerRepository freelancerRepository = mock(FreelancerRepository.class);
    private FeatureService featureService = mock(FeatureService.class);

    @Before
    public void setUp() {
        doReturn(1).when(shardHelper).getShardByClientId(any());
        freelancerValidationService =
                new FreelancerValidationService(shardHelper, freelancerProjectRepository, freelancerRepository,
                        featureService);
    }

    @Test
    public void validateProjectRequest_notClient() {
        User user = new User().withRole(RbacRole.AGENCY).withClientId(ClientId.fromLong(VALID_ID));

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, VALID_ID);

        assertThat(vr).has(error(mustBeClient()));
    }

    @Test
    public void validateProjectRequest_clientIsFreelancer_error() {
        User user = new User().withRole(RbacRole.CLIENT).withClientId(ClientId.fromLong(VALID_ID));
        doReturn(Collections.singletonList(1L)).when(freelancerRepository).getByIds(anyInt(), any());

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, VALID_ID);

        assertThat(vr).has(error(clientIsAlreadyFreelancer()));
    }

    @Test
    public void validateProjectRequest_agencyClient_error() {
        User user = new User()
                .withRole(RbacRole.CLIENT)
                .withAgencyClientId(VALID_ID)
                .withClientId(ClientId.fromLong(VALID_ID));

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, VALID_ID);

        assertThat(vr).has(error(agencyClientCantRequestService()));
    }

    @Test
    public void validateProjectRequest_HasProjects() {
        FreelancerProject existingProject =
                new FreelancerProject().withStatus(FreelancerProjectStatus.INPROGRESS).withId(VALID_ID);
        doReturn(Collections.singletonList(existingProject)).when(freelancerProjectRepository)
                .get(anyInt(), any(), any());

        User user = new User().withRole(RbacRole.CLIENT).withClientId(ClientId.fromLong(VALID_ID));

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, VALID_ID);

        assertThat(vr).has(error(projectsAreAlreadyExist(Collections.singletonList(existingProject.getId()))));
    }

    @Test
    public void validateProjectRequest_InvalidFreelancerId() {
        User user = new User().withRole(RbacRole.CLIENT).withClientId(ClientId.fromLong(VALID_ID));

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, INVALID_ID);

        assertThat(vr).has(error(validId()));
    }

    @Test
    public void validateProjectRequest_NotAFreelancer() {
        doReturn(Collections.emptyList()).when(freelancerRepository).getByIds(anyInt(), any());

        User user = new User().withRole(RbacRole.CLIENT).withClientId(ClientId.fromLong(VALID_ID));

        ValidationResult<Void, Defect> vr = freelancerValidationService.validateProjectRequest(user, VALID_ID);

        assertThat(vr).has(error(mustBeFreelancer()));
    }
}
