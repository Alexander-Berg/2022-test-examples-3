package ru.yandex.direct.internaltools.tools.freelancer.tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import one.util.streamex.EntryStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerStatus;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerRegisterService;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.expert.client.ExpertClient;
import ru.yandex.direct.expert.client.model.Certificate;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.freelancer.model.FreelancerRegisterParameters;
import ru.yandex.direct.internaltools.tools.freelancer.service.IntToolFreelancerConverterService;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT_PRO;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.METRIKA;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_BEFORE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerRegisterToolTest {

    private static final String FIRST_NAME = "Имя";
    private static final String SECOND_NAME = "Елкин";
    private static final String YO_SECOND_NAME = "Ёлкин";
    private static final String INITIAL_RATING = "1.0";
    private static final long FIRST_CERT_ID = 100501L;
    private static final long SECOND_CERT_ID = 100502L;
    private static final long THIRD_CERT_ID = 100503L;
    private static final LocalDateTime YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0);
    private static final LocalDateTime MONTH_AGO = LocalDate.now().minusMonths(1).atTime(0, 0);
    private static final String EMAIL = "smth@ya.ru";

    @Autowired
    Steps steps;
    @Autowired
    FreelancerRegisterService freelancerRegisterService;
    @Autowired
    FreelancerService freelancerService;
    @Autowired
    IntToolFreelancerConverterService converterService;
    @Autowired
    ClientService clientService;
    @Autowired
    UserService userService;
    @Autowired
    YandexSenderClient senderClient;
    @Autowired
    @Qualifier("solomonProject")
    String senderRegisterFreelancerSlug;
    @Autowired
    BlackboxUserService blackboxUserService;
    @Autowired
    ExpertClient expertClient;

    private FreelancerRegisterTool testedTool;

    private User manager;

    private final Map<Long, List<Certificate>> certificatesByUid = new HashMap<>();
    private Long freelancerUid;
    private long freelancerId;
    private String freelancerLogin;
    private String certLogin;
    private long certUid;

    @Before
    public void setUp() {
        testedTool = new FreelancerRegisterTool(freelancerRegisterService,
                freelancerService,
                converterService,
                clientService,
                userService,
                expertClient,
                senderClient,
                senderRegisterFreelancerSlug,
                blackboxUserService);

        ClientInfo freelancerInfo = steps.clientSteps().createDefaultClient();
        freelancerUid = freelancerInfo.getUid();
        freelancerId = freelancerInfo.getClientId().asLong();
        freelancerLogin = freelancerInfo.getLogin();

        UserInfo managerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        manager = managerInfo.getUser();

        User certUser = steps.userSteps().createUserInBlackboxStub();
        certLogin = certUser.getLogin();
        certUid = certUser.getUid();
        when(blackboxUserService.getUidByLogin(argThat(certLogin::equals)))
                .thenReturn(Optional.of(certUid));

        when(expertClient.getCertificates(anyList())).thenAnswer(
                invocation -> {
                    List<Long> uids = invocation.getArgument(0);
                    Set<Long> uidSet = new HashSet<>(uids);
                    return EntryStream.of(certificatesByUid)
                            .filterKeys(uidSet::contains)
                            .toMap();
                });
    }

    @After
    public void tearDown() {
        //Чтобы исключить влияние на другие тесты.
        clearInvocations(expertClient);
    }

    @Test
    public void getMassData_success() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT, DIRECT_TYPE_BEFORE);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);
        addCertificate(certUid, THIRD_CERT_ID, YO_SECOND_NAME, METRIKA, YESTERDAY);

        process();

        Freelancer actualFreelancer = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0);

        FreelancerBase expectedFreelancer = new Freelancer()
                .withId(freelancerId)
                .withFirstName(FIRST_NAME)
                .withSecondName(SECOND_NAME)
                .withStatus(FreelancerStatus.FREE)
                .withIsSearchable(true)
                .withRating(Double.parseDouble(INITIAL_RATING))
                .withCertLogin(null);
        FreelancerCertificate[] expectedCertificates = new FreelancerCertificate[]{
                new FreelancerCertificate()
                        .withCertId(FIRST_CERT_ID)
                        .withType(DIRECT),
                new FreelancerCertificate()
                        .withCertId(SECOND_CERT_ID)
                        .withType(DIRECT_PRO),
                new FreelancerCertificate()
                        .withCertId(THIRD_CERT_ID)
                        .withType(METRIKA)};

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualFreelancer).as("actual freelancer")
                    .is(matchedBy(beanDiffer(expectedFreelancer).useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFreelancer.getCertificates()).as("actual certificates")
                    .containsExactlyInAnyOrder(expectedCertificates);
        });
    }

    @Test
    public void getMassData_whenThereIsDuplicateCertsAndFirstIsNewer_success() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, MONTH_AGO);

        process();

        List<FreelancerCertificate> actualCertificates = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0)
                .getCertificates();
        FreelancerCertificate expectedCertificate = new FreelancerCertificate()
                .withCertId(FIRST_CERT_ID)
                .withType(DIRECT_PRO);
        assertThat(actualCertificates).as("actual certificates")
                .containsOnly(expectedCertificate);
    }

    @Test
    public void getMassData_whenThereIsDuplicateCertsAndSecondIsNewer_success() {
        addCertificate(freelancerUid, FIRST_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, MONTH_AGO);
        addCertificate(certUid, SECOND_CERT_ID, YO_SECOND_NAME, DIRECT_PRO, YESTERDAY);

        process();

        List<FreelancerCertificate> actualCertificates = freelancerService.getFreelancers(singleton(freelancerId))
                .get(0)
                .getCertificates();
        FreelancerCertificate expectedCertificate = new FreelancerCertificate()
                .withCertId(SECOND_CERT_ID)
                .withType(DIRECT_PRO);
        assertThat(actualCertificates).as("actual certificates")
                .containsOnly(expectedCertificate);
    }

    @Test(expected = InternalToolValidationException.class)
    public void getMassData_failure_whenCertLoginHasOtherSecondName() {
        addCertificate(certUid, FIRST_CERT_ID, SECOND_NAME + "еще", DIRECT, YESTERDAY);
        process();
    }

    @Test(expected = InternalToolValidationException.class)
    public void getMassData_failure_whenCertLoginHasNoCertificate() {
        checkState(certificatesByUid.isEmpty());
        process();
    }

    @Test(expected = InternalToolValidationException.class)
    public void getMassData_failure_whenCertLoginHasNoOneDirectCertificate() {
        addCertificate(freelancerUid, FIRST_CERT_ID, SECOND_NAME, METRIKA, YESTERDAY);
        process();
    }

    private void process() {
        FreelancerRegisterParameters registerParameters = new FreelancerRegisterParameters();
        registerParameters.setLogin(freelancerLogin);
        registerParameters.setCertLogin(certLogin);
        registerParameters.setFirstName(FIRST_NAME);
        registerParameters.setSecondName(SECOND_NAME);
        registerParameters.setInitialRating(INITIAL_RATING);
        registerParameters.setOperator(manager);
        registerParameters.setEmail(EMAIL);

        // Вызов validate обязателен, т.к. в нём меняется состояние объекта registerParameters
        ValidationResult<FreelancerRegisterParameters, ?> validationResult = testedTool.validate(registerParameters);
        checkState(!validationResult.hasAnyErrors());
        testedTool.getMassData(registerParameters);
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
        List<Certificate> certificates = certificatesByUid.computeIfAbsent(uid, k -> new ArrayList<>());
        certificates.add(certificate);
    }

}
