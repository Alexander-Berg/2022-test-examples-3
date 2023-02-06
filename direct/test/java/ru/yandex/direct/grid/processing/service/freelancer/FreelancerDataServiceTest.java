package ru.yandex.direct.grid.processing.service.freelancer;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.freelancer.GdClientProject;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancer;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerCertificateType;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerFilter;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerFull;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerProjectFilter;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerSkill;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerSkillOfferDuration;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancer;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerSkill;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerSkillItem;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class FreelancerDataServiceTest {

    private final TestContextManager testContextManager = new TestContextManager(FreelancerDataServiceTest.class);

    @Autowired
    Steps steps;

    @Autowired
    FreelancerDataService testedService;

    private FreelancerInfo defaultFreelancerInfo;

    @Before
    public void setUp() throws Exception {
        this.testContextManager.prepareTestInstance(this);
        defaultFreelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
    }

    @Test
    public void getFreelancersById_success() {
        ClientId freelancerId = ClientId.fromLong(defaultFreelancerInfo.getFreelancer().getFreelancerId());
        List<GdFreelancer> actual =
                testedService.getFreelancers(
                        new GdFreelancerFilter().withFreelancerIds(singletonList(freelancerId.asLong())),
                        GdFreelancerFull::new, null);
        assertThat(actual).isNotEmpty();
    }

    @Test
    public void getFreelancersByLogin_success() {
        String login = defaultFreelancerInfo.getClientInfo().getLogin();
        List<GdFreelancer> actual =
                testedService.getFreelancers(
                        new GdFreelancerFilter().withLogins(singletonList(login)),
                        GdFreelancerFull::new, null);
        assertThat(actual).isNotEmpty();
    }

    @Test
    public void getFreelancersByLogin_nullResult_exception() {
        String login = "illegal_login";
        assertThatThrownBy(() -> testedService.getFreelancers(
                new GdFreelancerFilter().withLogins(singletonList(login)),
                GdFreelancerFull::new, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void getFreelancers_notEmpty_onNull() {
        assertThat(testedService.getFreelancers(null, GdFreelancerFull::new, null)).isNotEmpty();
    }

    @Test
    public void getFreelancers_skipFreelancer_whenNoCertificates() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long freelancerId = clientInfo.getClientId().asLong();

        // Создаём специалиста без сертификата Директа
        Freelancer flWithoutCert = defaultFreelancer(freelancerId);
        FreelancerCertificate metrikaCert =
                new FreelancerCertificate().withCertId(1L).withType(FreelancerCertificateType.METRIKA);
        flWithoutCert.setCertificates(singletonList(metrikaCert));

        FreelancerInfo flNoCert = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(flWithoutCert);
        steps.freelancerSteps().createFreelancer(flNoCert);

        Condition<GdFreelancerFull> freelancerWithDirectCert =
                new Condition<>(s -> StreamEx.of(s.getCertificates()).anyMatch(
                        c -> c.getType() == GdFreelancerCertificateType.DIRECT
                                || c.getType() == GdFreelancerCertificateType.DIRECT_PRO),
                        "freelancer with direct certificate");

        List<GdFreelancerFull> actualFreelancers = testedService.getFreelancers(null, GdFreelancerFull::new, null);

        assertThat(actualFreelancers)
                .are(freelancerWithDirectCert);
    }

    @Test
    public void getFreelancersCount_moreThanZero() {
        // В тестовой БД заранее нельзя предсказать общее число специалистов
        // Мы уверены лишь в том, что есть как минимум один фрилансер, созданный в @Before для этого теста
        assertThat(testedService.getFreelancersCount()).isGreaterThan(0);
    }

    @Test
    public void shuffleFreelancerListTest_differentOperatorUid() {
        Long firstOperatorUid = 1L;
        Long secondOperatorUid = 2L;
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        List<GdFreelancer> firstRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, firstOperatorUid);
        List<GdFreelancer> secondRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, secondOperatorUid);

        assertThat(firstRequest)
                .as("Ожидаем, что разным пользователям фрилансеры вернутся в разном порядке")
                // (сейчас на наших данных текущая реализация возвращает разные списки)
                .isNotEqualTo(secondRequest);
    }

    @Test
    public void shuffleFreelancerListTest_equalOperatorUid() {
        Long operatorUid = 1L;
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        List<GdFreelancer> firstRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, operatorUid);
        List<GdFreelancer> secondRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, operatorUid);

        assertThat(firstRequest)
                .as("Одному и тому же пользователю фрилансеры должны возвращаться в одном порядке")
                .isEqualTo(secondRequest);
    }

    @Test
    public void shuffleFreelancerListTest_addNewFreelancer() {
        Long operatorUid = 1L;
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        steps.freelancerSteps().addDefaultFreelancer();
        List<GdFreelancer> firstRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, operatorUid);

        defaultFreelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        List<GdFreelancer> secondRequest =
                testedService.getFreelancers(null, GdFreelancerFull::new, operatorUid);

        assertThat(firstRequest.size()).isNotEqualTo(secondRequest.size());
        assertThat(firstRequest)
                .as("Ожидаем, что при изменении набора фрилансеров порядок фрилансеров изменится")
                // (сейчас на наших данных текущая реализация возвращает разные списки)
                .isNotEqualTo(secondRequest.subList(0, firstRequest.size()));
    }

    @Test
    public void getClientProjects_success() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        long clientId = client.getClientId().asLong();
        long freelancerId = defaultFreelancerInfo.getClientId().asLong();
        LocalDateTime now = LocalDateTime.now();
        long projectId = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        FreelancerProject project = new FreelancerProject()
                .withId(projectId)
                .withClientId(clientId)
                .withFreelancerId(freelancerId)
                .withCreatedTime(now)
                .withUpdatedTime(now)
                .withStartedTime(now)
                .withStatus(FreelancerProjectStatus.INPROGRESS);
        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withClientInfo(client)
                .withFreelancerInfo(defaultFreelancerInfo)
                .withProject(project);
        steps.freelancerSteps().createProject(projectInfo);
        GdFreelancerProjectFilter gdFilter = new GdFreelancerProjectFilter()
                .withFreelancerId(freelancerId)
                .withIsActive(true)
                .withFirst(1);
        List<GdClientProject> clientProjects = testedService.getClientProjects(clientId, gdFilter);
        checkState(!clientProjects.isEmpty());
        GdClientProject gdClientProject = clientProjects.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdClientProject.getClientId()).describedAs("clientId").isEqualTo(clientId);
            softly.assertThat(gdClientProject.getFreelancerId()).describedAs("freelancerId").isEqualTo(freelancerId);
        });
    }

    @Test
    public void getClientProjects_byFreelancerLogin() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        UserInfo freelancerUser = defaultFreelancerInfo.getClientInfo().getChiefUserInfo();
        long clientId = client.getClientId().asLong();
        long freelancerId = defaultFreelancerInfo.getClientId().asLong();
        LocalDateTime now = LocalDateTime.now();
        long projectId = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        FreelancerProject project = new FreelancerProject()
                .withId(projectId)
                .withClientId(clientId)
                .withFreelancerId(freelancerId)
                .withCreatedTime(now)
                .withUpdatedTime(now)
                .withStartedTime(now)
                .withStatus(FreelancerProjectStatus.INPROGRESS);
        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withClientInfo(client)
                .withFreelancerInfo(defaultFreelancerInfo)
                .withProject(project);
        steps.freelancerSteps().createProject(projectInfo);
        GdFreelancerProjectFilter gdFilter = new GdFreelancerProjectFilter()
                .withFreelancerLogin(freelancerUser.getUser().getLogin())
                .withIsActive(true)
                .withFirst(1);
        List<GdClientProject> clientProjects = testedService.getClientProjects(clientId, gdFilter);
        checkState(!clientProjects.isEmpty());
        GdClientProject gdClientProject = clientProjects.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdClientProject.getClientId()).describedAs("clientId").isEqualTo(clientId);
            softly.assertThat(gdClientProject.getFreelancerId()).describedAs("freelancerId").isEqualTo(freelancerId);
        });
    }

    @Test
    public void updateFreelancer_success() {
        ClientId clientId = defaultFreelancerInfo.getClientId();
        GdUpdateFreelancer input = new GdUpdateFreelancer()
                .withRegionId(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
        GdFreelancerFull updatedFreelancer = testedService.updateFreelancer(clientId, input);

        assertThat(updatedFreelancer.getRegionId()).describedAs("regionId")
                .isEqualTo(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
    }

    @Test
    @Parameters(method = "correctDurations")
    public void updateFreelancerSkills_success(GdFreelancerSkillOfferDuration gdDuration) {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        Long testSkillId = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH.getSkillId();
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        GdUpdateFreelancerSkillItem skillItem = new GdUpdateFreelancerSkillItem()
                .withSkillId(testSkillId)
                .withPrice(10L)
                .withDuration(gdDuration);
        GdUpdateFreelancerSkill input = new GdUpdateFreelancerSkill()
                .withSkills(singletonList(skillItem));

        GdFreelancerSkill gdUpdateFreelancerSkill =
                testedService.updateFreelancerSkills(ClientId.fromLong(freelancerId), input).get(0);
        assertThat(gdUpdateFreelancerSkill.getDuration()).isEqualTo(gdDuration);
    }

    @SuppressWarnings("unused")
    private Object[] correctDurations() {
        return GdFreelancerSkillOfferDuration.values();
    }

    @Test
    @Parameters(method = "correctDays")
    //todo mariachernova: Проверка, что фронт не ломается, потом удалить
    public void updateFreelancerSkills_setDays_success(String days) {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        Long testSkillId = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH.getSkillId();
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        GdUpdateFreelancerSkillItem skillItem = new GdUpdateFreelancerSkillItem()
                .withSkillId(testSkillId)
                .withPrice(10L)
                .withDays(days);
        GdUpdateFreelancerSkill input = new GdUpdateFreelancerSkill()
                .withSkills(singletonList(skillItem));

        GdFreelancerSkill gdUpdateFreelancerSkill =
                testedService.updateFreelancerSkills(ClientId.fromLong(freelancerId), input).get(0);
        EnumSet<GdFreelancerSkillOfferDuration> correctDurations = EnumSet.allOf(GdFreelancerSkillOfferDuration.class);
        correctDurations.remove(GdFreelancerSkillOfferDuration.NOT_DEFINED);
        assertThat(gdUpdateFreelancerSkill.getDuration()).isIn(correctDurations);
    }

    @SuppressWarnings("unused")
    Object[] correctDays() {
        return new String[]{"1-3", "3-7", "7-14", "14-28", "28-90", "90-"};
    }

    @Test
    @Parameters(method = "incorrectDays")
    //todo mariachernova: Проверка, что фронт не ломается, потом удалить
    public void updateFreelancerSkills_setUnknownDays_success(String days) {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        Long testSkillId = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH.getSkillId();
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        GdUpdateFreelancerSkillItem skillItem = new GdUpdateFreelancerSkillItem()
                .withSkillId(testSkillId)
                .withPrice(10L)
                .withDays(days);
        GdUpdateFreelancerSkill input = new GdUpdateFreelancerSkill()
                .withSkills(singletonList(skillItem));

        GdFreelancerSkill gdUpdateFreelancerSkill =
                testedService.updateFreelancerSkills(ClientId.fromLong(freelancerId), input).get(0);
        assertThat(gdUpdateFreelancerSkill.getDuration()).isEqualTo(GdFreelancerSkillOfferDuration.NOT_DEFINED);
        assertThat(gdUpdateFreelancerSkill.getDays()).isEqualTo("28-90");
    }

    @SuppressWarnings("unused")
    Object[] incorrectDays() {
        return new String[]{"", "unknown", null};
    }
}
