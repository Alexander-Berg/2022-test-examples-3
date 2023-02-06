package ru.yandex.direct.core.entity.freelancer.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerUpdateServiceTest {

    private static final long NEW_REGION_ID = 200L;

    @Autowired
    Steps steps;

    @Autowired
    FreelancerRepository freelancerRepository;

    @Autowired
    FreelancerUpdateService testedService;

    private FreelancerInfo freelancerInfo;

    @Before
    public void setUp() {
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
    }

    @Test
    public void updateFreelancer_success() {
        ClientId clientId = freelancerInfo.getClientId();
        Long id = freelancerInfo.getFreelancer().getId();
        FreelancerCertificate certificate = new FreelancerCertificate()
                .withCertId(111L)
                .withType(FreelancerCertificateType.DIRECT);
        FreelancerBase freelancer = new FreelancerBase()
                .withFreelancerId(id)
                .withRegionId(NEW_REGION_ID)
                .withIsSearchable(true)
                .withCertificates(singletonList(certificate))
                .withRating(1.5)
                .withStatus(FreelancerStatus.BUSY);
        Result<Long> result = testedService.updateFreelancer(clientId, freelancer);

        assertThat(result.getValidationResult().getErrors()).isEmpty();

        Integer shard = freelancerInfo.getShard();
        Set<Long> ids = singleton(id);
        List<Freelancer> freelancers = freelancerRepository.getByIds(shard, ids);
        Freelancer expectedFreelancer = new Freelancer().withCard(freelancerInfo.getFreelancer().getCard());
        expectedFreelancer
                .withFreelancerId(id)
                .withFirstName(freelancerInfo.getFreelancer().getFirstName())
                .withSecondName(freelancerInfo.getFreelancer().getSecondName())
                .withRegionId(NEW_REGION_ID)
                .withIsSearchable(true)
                .withCertificates(singletonList(certificate))
                .withRating(1.5)
                .withStatus(FreelancerStatus.BUSY);

        assertThat(freelancers).first()
                .is(matchedBy(beanDiffer(expectedFreelancer).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFreelancerCards_nullFreelancer_exception() {
        ClientId clientId = freelancerInfo.getClientId();
        FreelancerBase freelancer = new FreelancerBase();
        assertThatThrownBy(() -> testedService.updateFreelancer(clientId, freelancer))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
