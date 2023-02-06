package ru.yandex.direct.internaltools.tools.freelancer.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.container.FreelancersQueryFilter;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.expert.client.ExpertClient;
import ru.yandex.direct.expert.client.model.Certificate;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.freelancer.model.CertTrouble;
import ru.yandex.direct.rbac.RbacService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_AFTER;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_BEFORE;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.core.testing.data.TestFreelancers.getDefaultFlCertificate;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerExpiredCertsToolTest {

    @Autowired
    private Steps steps;
    @Autowired
    private FreelancerService freelancerService;
    @Autowired
    private UserService userService;
    @Autowired
    private BlackboxUserService blackboxUserService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private ExpertClient expertClient;

    private FreelancerExpiredCertsTool testingTool;
    private final Map<Long, List<Certificate>> extCertsByUid = new HashMap<>();

    private static Certificate convertToExtCert(FreelancerCertificate frlCert) {
        Certificate certificate = new Certificate();
        certificate.setCertId(frlCert.getCertId().intValue());
        Certificate.Exam exam = new Certificate.Exam();
        exam.setSlug(frlCert.getType().name().replace("_", "-"));
        certificate.setExam(exam);
        certificate.setActive(1);
        certificate.setConfirmedDate(DIRECT_TYPE_BEFORE);
        return certificate;
    }

    @Before
    public void setUp() {
        testingTool = new FreelancerExpiredCertsTool(freelancerService, userService, expertClient,
                blackboxUserService, rbacService);

        when(expertClient.getCertificates(anyList())).thenAnswer(
                invocation -> {
                    List<Long> uids = invocation.getArgument(0);
                    Set<Long> uidSet = new HashSet<>(uids);
                    return EntryStream.of(extCertsByUid)
                            .filterKeys(uidSet::contains)
                            .toMap();
                });
    }

    @After
    public void tearDown() {
        extCertsByUid.clear();
    }

    @Test
    public void getMassData_whenCertExpired_success() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        List<FreelancerCertificate> certificates = freelancerInfo.getFreelancer().getCertificates();
        checkState(certificates.size() == 1);
        String typeName = certificates.get(0).getType().name();

        List<CertTrouble> massData = testingTool.getMassData(null);
        checkNotNull(massData);

        CertTrouble actual = StreamEx.of(massData)
                .findAny(trouble -> Objects.equals(trouble.getId(), freelancerInfo.getFreelancerId()))
                .orElseThrow();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual.getLogin()).as("Login")
                    .isEqualTo(freelancerInfo.getClientInfo().getLogin());
            soft.assertThat(actual.getExpiredCertTypes()).as("ExpiredCertTypes")
                    .contains(typeName);
        });
    }

    @Test
    public void getMassData_whenCertNotExpired_success() {
        steps.freelancerSteps().addDefaultFreelancer();
        List<Freelancer> freelancers =
                freelancerService.getFreelancers(FreelancersQueryFilter.activeFreelancers().build());
        for (var frl : freelancers) {
            List<Certificate> certs = mapList(frl.getCertificates(),
                    FreelancerExpiredCertsToolTest::convertToExtCert);
            long uid = rbacService.getChiefByClientId(ClientId.fromLong(frl.getFreelancerId()));
            extCertsByUid.put(uid, certs);
            //отвечаем правильным логином на uid
            doReturn(Optional.of(uid)).when(blackboxUserService).getUidByLogin(frl.getCertLogin());
        }

        List<CertTrouble> massData = testingTool.getMassData(null);

        assertThat(massData).as("Size").isEmpty();
    }

    @Test
    public void getMassData_whenCertDirectNotDiscarded_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer newFreelancer = defaultFreelancer(clientInfo.getClientId().asLong());
        FreelancerCertificate flCert = newFreelancer.getCertificates().get(0);
        flCert.setType(DIRECT);
        FreelancerInfo newFreelancerInfo = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(newFreelancer);
        steps.freelancerSteps().createFreelancer(newFreelancerInfo);
        Certificate extCert = convertToExtCert(flCert);
        extCert.setConfirmedDate(DIRECT_TYPE_BEFORE);
        extCertsByUid.put(clientInfo.getUid(), singletonList(extCert));

        List<CertTrouble> massData = testingTool.getMassData(null);
        List<CertTrouble> troubles = filterList(massData,
                trouble -> Objects.equals(trouble.getId(), newFreelancer.getFreelancerId()));
        assertThat(troubles).as("result").isEmpty();
    }

    @Test
    public void getMassData_whenCertDirectDiscarded_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer newFreelancer = defaultFreelancer(clientInfo.getClientId().asLong());
        FreelancerCertificate flCert = newFreelancer.getCertificates().get(0);
        flCert.setType(DIRECT);
        FreelancerInfo newFreelancerInfo = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(newFreelancer);
        steps.freelancerSteps().createFreelancer(newFreelancerInfo);
        Certificate extCert = convertToExtCert(flCert);
        extCert.setConfirmedDate(DIRECT_TYPE_AFTER);
        extCertsByUid.put(clientInfo.getUid(), singletonList(extCert));

        List<CertTrouble> massData = testingTool.getMassData(null);
        checkNotNull(massData);

        CertTrouble actual = StreamEx.of(massData)
                .findAny(trouble -> Objects.equals(trouble.getId(), newFreelancer.getFreelancerId()))
                .orElseThrow();
        assertThat(actual.getExpiredCertTypes()).as("ExpiredCertTypes")
                .contains(DIRECT.name());
    }

    @Test
    public void getMassData_whenWrongCertTypeInDirect_success() {
        checkState(getDefaultFlCertificate().getType() == FreelancerCertificateType.DIRECT_PRO);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer newFreelancer = defaultFreelancer(clientInfo.getClientId().asLong());
        newFreelancer.withCertificates(singletonList(getDefaultFlCertificate().withType(DIRECT)));
        FreelancerInfo newFreelancerInfo = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(newFreelancer);
        steps.freelancerSteps().createFreelancer(newFreelancerInfo);
        Long freelancerId = newFreelancer.getId();

        Certificate extCert = convertToExtCert(getDefaultFlCertificate());
        extCert.setConfirmedDate(DIRECT_TYPE_AFTER);
        extCertsByUid.put(clientInfo.getUid(), singletonList(extCert));

        Optional<CertTrouble> freelancerTrouble = testingTool.getMassData(null)
                .stream()
                .filter(l -> Objects.equals(l.getId(), freelancerId))
                .findFirst();
        assertThat(freelancerTrouble).as("DIRECT was replaced by DIRECT_PRO").isEmpty();
    }

}
