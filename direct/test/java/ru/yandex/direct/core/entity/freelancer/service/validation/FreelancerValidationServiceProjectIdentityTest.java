package ru.yandex.direct.core.entity.freelancer.service.validation;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectIdentity;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.error;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;

public class FreelancerValidationServiceProjectIdentityTest {
    private static final long VALID_ID = 1L;
    private static final long INVALID_ID = -1L;

    private FreelancerValidationService freelancerValidationService;

    @Before
    public void setUp() {
        ShardHelper shardHelper = mock(ShardHelper.class);
        FreelancerProjectRepository freelancerProjectRepository = mock(FreelancerProjectRepository.class);
        FreelancerRepository freelancerRepository = mock(FreelancerRepository.class);
        FeatureService featureService = mock(FeatureService.class);
        freelancerValidationService =
                new FreelancerValidationService(shardHelper, freelancerProjectRepository, freelancerRepository,
                        featureService);
    }

    @Test
    public void validateProjectIdentity_freelancerIdNull() {
        FreelancerProjectIdentity projectIdentity = new FreelancerProject().withId(VALID_ID).withClientId(VALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(notNull()));
    }

    @Test
    public void validateProjectIdentity_clientIdNull() {
        FreelancerProjectIdentity projectIdentity = new FreelancerProject().withId(VALID_ID).withFreelancerId(VALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(notNull()));
    }

    @Test
    public void validateProjectIdentity_idNull() {
        FreelancerProjectIdentity projectIdentity =
                new FreelancerProject().withClientId(VALID_ID).withFreelancerId(VALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(notNull()));
    }

    @Test
    public void validateProjectIdentity_freelancerIdInvalid() {
        FreelancerProjectIdentity projectIdentity =
                new FreelancerProject().withId(VALID_ID).withClientId(VALID_ID).withFreelancerId(INVALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(validId()));
    }

    @Test
    public void validateProjectIdentity_clientIdInvalid() {
        FreelancerProjectIdentity projectIdentity =
                new FreelancerProject().withId(VALID_ID).withClientId(INVALID_ID).withFreelancerId(VALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(validId()));
    }

    @Test
    public void validateProjectIdentity_idInvalid() {
        FreelancerProjectIdentity projectIdentity =
                new FreelancerProject().withId(INVALID_ID).withClientId(VALID_ID).withFreelancerId(VALID_ID);
        ValidationResult<FreelancerProjectIdentity, Defect> vr =
                freelancerValidationService.validateProjectIdentity(projectIdentity);
        assertThat(vr).has(error(validId()));
    }
}
