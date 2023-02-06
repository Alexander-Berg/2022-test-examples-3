package ru.yandex.direct.jobs.freelancers.certimport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerCertificateService;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.expert.client.ExpertClient;
import ru.yandex.direct.expert.client.model.Certificate;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.support.PeriodicJobWrapper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT_PRO;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.METRIKA;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.FREELANCER_CERTIFICATES_UPDATING_ENABLED;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_AFTER;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_BEFORE;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.jobs.freelancers.certimport.FreelancerCertificatesUpdateJob.LAST_TIME_PROPERTY_KEY_PATTERN;
import static ru.yandex.direct.scheduler.support.DirectShardedJob.SHARD_PARAM;


@JobsTest
@ExtendWith(SpringExtension.class)
class FreelancerCertificatesUpdateJobTest {

    private static final String FIRST_NAME = "Имя";
    private static final String SECOND_NAME = "Елкин";
    private static final String YO_SECOND_NAME = "Ёлкин";
    private static final long FIRST_CERT_ID = 100501L;
    private static final long SECOND_CERT_ID = 100502L;
    private static final long THIRD_CERT_ID = 100503L;
    private static final LocalDateTime YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0);
    private static final LocalDateTime MONTH_AGO = LocalDate.now().minusMonths(1).atTime(0, 0);

    @Autowired
    FreelancerService freelancerService;
    @Autowired
    private Steps steps;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FreelancerRepository freelancerRepository;
    @Autowired
    private FreelancerCertificateService freelancerCertificateService;
    @Autowired
    BlackboxUserService blackboxUserService;
    @Autowired
    ExpertClient expertClient;

    private FreelancerCertificatesUpdateService freelancerCertificatesUpdateService;
    private final Map<Long, List<Certificate>> extCertsByUid = new HashMap<>();
    private Long freelancerUid;
    private long freelancerId;
    private Integer shard;
    private String certLogin;
    private long certUid;

    @BeforeEach
    void beforeEach() {
        User certUser = steps.userSteps().createUserInBlackboxStub();
        certLogin = certUser.getLogin();
        certUid = certUser.getUid();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer freelancer = defaultFreelancer(clientInfo.getClientId().asLong());
        freelancer.withFirstName(FIRST_NAME)
                .withSecondName(SECOND_NAME)
                .withCertLogin(certLogin)
                .withIsSearchable(false); //фрилансеры с невалидными сертификатами исключаются из списка
        FreelancerInfo freelancerInfo = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(freelancer);
        steps.freelancerSteps().createFreelancer(freelancerInfo);
        freelancerUid = clientInfo.getUid();
        freelancerId = clientInfo.getClientId().asLong();
        shard = clientInfo.getShard();

        FreelancerRepository spyFreelancerRepository = Mockito.spy(freelancerRepository);
        freelancerCertificatesUpdateService =
                new FreelancerCertificatesUpdateService(spyFreelancerRepository, freelancerCertificateService);
        when(spyFreelancerRepository.getAllEnabledFreelancers(shard)).thenAnswer(
                ignored -> freelancerRepository.getByIds(shard, singleton(freelancerId))
        );
        when(expertClient.getCertificates(anyList())).thenAnswer(
                invocation -> {
                    List<Long> uids = invocation.getArgument(0);
                    Set<Long> uidSet = new HashSet<>(uids);
                    return EntryStream.of(extCertsByUid)
                            .filterKeys(uidSet::contains)
                            .toMap();
                });
        when(blackboxUserService.getUidsByLogins((List<String>) argThat(contains(certLogin))))
                .thenAnswer(args -> singletonMap(certLogin, certUid));

        String jobName = FREELANCER_CERTIFICATES_UPDATING_ENABLED.getName();
        String trueAsString = Boolean.toString(true);
        ppcPropertiesSupport.set(jobName, trueAsString);
    }

    @AfterEach
    public void afterEach() {
        //Чтобы исключить влияние на другие тесты.
        clearInvocations(expertClient);
    }

    @Test
    void execute_successReplaceCertificates() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT, DIRECT_TYPE_BEFORE);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);
        addCertificate(certUid, THIRD_CERT_ID, YO_SECOND_NAME, METRIKA, YESTERDAY);

        FreelancerCertificate[] expectedCertificates = {
                new FreelancerCertificate()
                        .withCertId(FIRST_CERT_ID)
                        .withType(DIRECT),
                new FreelancerCertificate()
                        .withCertId(SECOND_CERT_ID)
                        .withType(DIRECT_PRO),
                new FreelancerCertificate()
                        .withCertId(THIRD_CERT_ID)
                        .withType(METRIKA)};

        executeJob();

        List<FreelancerCertificate> actualCertificates = freelancerRepository.getByIds(shard, singleton(freelancerId))
                .get(0)
                .getCertificates();
        assertThat(actualCertificates).as("actual certificates")
                .containsExactlyInAnyOrder(expectedCertificates);
    }

    @Test
    public void execute_whenThereIsDuplicateCertsAndFirstIsNewer_success() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, MONTH_AGO);

        FreelancerCertificate expectedCertificate = new FreelancerCertificate()
                .withCertId(FIRST_CERT_ID)
                .withType(DIRECT_PRO);

        executeJob();

        List<FreelancerCertificate> actualCertificates = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0)
                .getCertificates();
        assertThat(actualCertificates).as("actual certificates")
                .containsOnly(expectedCertificate);
    }

    @Test
    public void execute_whenThereIsDuplicateCertsAndSecondIsNewer_success() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, MONTH_AGO);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);

        FreelancerCertificate expectedCertificate = new FreelancerCertificate()
                .withCertId(SECOND_CERT_ID)
                .withType(DIRECT_PRO);

        executeJob();

        List<FreelancerCertificate> actualCertificates = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0)
                .getCertificates();
        assertThat(actualCertificates).as("actual certificates")
                .containsOnly(expectedCertificate);
    }

    @Test
    public void execute_whenCertLoginHasOtherSecondName_getEmpty() {
        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME + "еще", DIRECT, YESTERDAY);

        executeJob();

        List<FreelancerCertificate> actualCertificates = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0)
                .getCertificates();
        assertThat(actualCertificates).as("actual certificates").isEmpty();
    }

    @Test
    public void execute_whenDirectProIsCert_success() {
        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME, DIRECT_PRO, DIRECT_TYPE_BEFORE);

        FreelancerCertificate expected = new FreelancerCertificate()
                .withCertId(FIRST_CERT_ID)
                .withType(DIRECT_PRO);

        executeJob();

        Freelancer freelancer =
                freelancerRepository.getByIds(shard, singleton(freelancerId)).stream().findFirst().get();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(freelancer.getIsSearchable()).as("Есть в общем списке").isEqualTo(true);
        sa.assertThat(freelancer.getCertificates()).containsExactly(expected);
        sa.assertAll();
    }

    @Test
    public void execute_whenDirectIsCert_success() {
        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME, DIRECT, DIRECT_TYPE_BEFORE);

        FreelancerCertificate expected = new FreelancerCertificate()
                .withCertId(FIRST_CERT_ID)
                .withType(DIRECT);

        executeJob();

        Freelancer freelancer =
                freelancerRepository.getByIds(shard, singleton(freelancerId)).stream().findFirst().get();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(freelancer.getIsSearchable()).as("Есть в общем списке").isEqualTo(true);
        sa.assertThat(freelancer.getCertificates()).containsExactly(expected);
        sa.assertAll();
    }

    @Test
    public void execute_whenDirectIsNotCertNotSearchable_getEmpty() {
        Freelancer freelancer =
                freelancerRepository.getByIds(shard, singleton(freelancerId)).stream().findFirst().get();

        ModelChanges<FreelancerBase> modelChanges = new ModelChanges<>(freelancerId, FreelancerBase.class)
                .process(true, FreelancerBase.IS_SEARCHABLE);

        AppliedChanges<FreelancerBase> freelancerBaseAppliedChanges = modelChanges.applyTo(freelancer);
        freelancerRepository.updateFreelancer(shard, List.of(freelancerBaseAppliedChanges));

        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME, DIRECT, DIRECT_TYPE_AFTER);

        executeJob();

        freelancer = freelancerRepository.getByIds(shard, singleton(freelancerId)).stream().findFirst().get();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(freelancer.getIsSearchable()).as("Исключен из общего списка").isEqualTo(false);
        sa.assertThat(freelancer.getCertificates()).isEmpty();
        sa.assertAll();
    }

    @Test
    public void execute_whenDirectIsNotCert_getEmpty() {
        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME, DIRECT, DIRECT_TYPE_AFTER);

        executeJob();

        List<FreelancerCertificate> actualCertificates = freelancerRepository.getByIds(shard, singleton(freelancerId))
                .get(0)
                .getCertificates();
        assertThat(actualCertificates).as("actual certificates")
                .isEmpty();
    }

    @Test
    void execute_freelancerNotSearchableWhenOnlyMetrikaCert() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, METRIKA, YESTERDAY);

        FreelancerCertificate expectedCertificate =
                new FreelancerCertificate()
                        .withCertId(FIRST_CERT_ID)
                        .withType(METRIKA);

        executeJob();

        Freelancer freelancer = freelancerRepository.getByIds(shard, singleton(freelancerId)).get(0);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(freelancer.getIsSearchable()).as("Исключен из общего списка").isEqualTo(false);
        sa.assertThat(freelancer.getCertificates()).containsExactly(expectedCertificate);
    }

    private void executeJob() {
        String propertyKey = String.format(LAST_TIME_PROPERTY_KEY_PATTERN, shard);
        ppcPropertiesSupport.set(propertyKey, "0");

        FreelancerCertificatesUpdateJob job =
                new FreelancerCertificatesUpdateJob(ppcPropertiesSupport, freelancerCertificatesUpdateService);
        TaskParametersMap shardContext = TaskParametersMap.of(SHARD_PARAM, String.valueOf(shard));
        new PeriodicJobWrapper(job).execute(shardContext);
    }

    private void addCertificate(Long uid,
                                long certId,
                                String secondName,
                                FreelancerCertificateType certificateType,
                                LocalDateTime confirmedDate) {
        Certificate certificate = new Certificate();
        certificate.setFirstname(FIRST_NAME);
        certificate.setLastname(secondName);
        certificate.setCertId((int) certId);
        certificate.setActive(1);
        certificate.setConfirmedDate(confirmedDate);
        Certificate.Exam exam = new Certificate.Exam();
        exam.setSlug(certificateType.name());
        certificate.setExam(exam);
        List<Certificate> certificates = extCertsByUid.computeIfAbsent(uid, k -> new ArrayList<>());
        certificates.add(certificate);
    }

}
