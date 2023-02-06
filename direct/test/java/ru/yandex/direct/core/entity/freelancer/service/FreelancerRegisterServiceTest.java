package ru.yandex.direct.core.entity.freelancer.service;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.dbutil.model.ClientId.fromLong;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class FreelancerRegisterServiceTest {

    @Autowired
    Steps steps;
    @Autowired
    FreelancerService freelancerService;
    @Autowired
    FreelancerRegisterService freelancerRegisterService;

    private ClientInfo clientInfo;
    private long superUid;
    private long clientId;
    private Freelancer freelancer;

    @Before
    public void setUp() throws Exception {
        ClientInfo superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        superUid = superClient.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId().asLong();
        freelancer = TestFreelancers.defaultFreelancer(clientId);
    }

    @Test
    public void registerFreelancer_success() {
        freelancer.withCard(null);
        freelancer.withCertificates(emptyList());

        freelancerRegisterService.registerFreelancer(fromLong(clientId), superUid, freelancer);

        Freelancer actualFreelancer = freelancerService.getFreelancers(singletonList(clientId)).get(0);

        Freelancer expectedFreelancer = TestFreelancers.defaultFreelancer(clientId);
        expectedFreelancer.withCard(null);
        expectedFreelancer.withCertificates(emptyList());

        assertThat(actualFreelancer)
                .is(matchedBy(beanDiffer(expectedFreelancer).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void registerFreelancer_failure_whenUsersCountExceeded() {
        steps.userSteps().createRepresentative(clientInfo);

        Result<Freelancer> result =
                freelancerRegisterService.registerFreelancer(fromLong(clientId), superUid, freelancer);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.isSuccessful()).isFalse();
            soft.assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(field(Freelancer.FREELANCER_ID)), FreelancerDefects.usersCountExceeded()))));
        });
    }

    @Test
    public void registerFreelancer_failure_whenDuplicatedCertificate() {
        freelancer.withCertificates(List.of(
                new FreelancerCertificate()
                        .withCertId(111L)
                        .withType(FreelancerCertificateType.DIRECT_PRO),
                new FreelancerCertificate()
                        .withCertId(222L)
                        .withType(FreelancerCertificateType.DIRECT_PRO)
        ));

        Result<Freelancer> result =
                freelancerRegisterService.registerFreelancer(fromLong(clientId), superUid, freelancer);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.isSuccessful()).isFalse();
            soft.assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(field(Freelancer.CERTIFICATES), index(0)), CollectionDefects.duplicatedElement()))));
        });
    }

}
