package ru.yandex.direct.internaltools.tools.freelancer.tool;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.freelancer.model.FreelancerUpdateParameters;
import ru.yandex.direct.internaltools.tools.freelancer.model.IntToolFreelancerCard;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerUpdateToolTest {

    @Autowired
    Steps steps;
    @Autowired
    FreelancerUpdateTool testedTool;
    @Autowired
    FreelancerService freelancerService;
    @Autowired
    ShardHelper shardHelper;

    @Test
    public void getMassData_success_whenUpdateExistingCerts() {
        // подготовка
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long clientId = clientInfo.getClientId().asLong();
        Freelancer freelancer = defaultFreelancer(clientId);
        freelancer.withCertificates(
                singletonList(new FreelancerCertificate().withCertId(1L).withType(FreelancerCertificateType.DIRECT)));
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);
        // проверка
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerInfo.getClientId().asLong())
                .setDirectCertId(2L);
        assertThatCode(() -> testedTool.getMassData(updateParameters))
                .doesNotThrowAnyException();
    }

    @Test
    public void getMassData_successChangeCert_whenUpdateExistingCerts() {
        // подготовка
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long clientId = clientInfo.getClientId().asLong();
        Freelancer freelancer = defaultFreelancer(clientId);
        freelancer.withCertificates(
                singletonList(new FreelancerCertificate().withCertId(1L).withType(FreelancerCertificateType.DIRECT)));
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        // проверка
        long newDirectCertId = 2L;
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerInfo.getClientId().asLong())
                .setDirectCertId(newDirectCertId);

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();
        assertThat(result)
                .extracting(IntToolFreelancerCard::getDirectCertId)
                .containsExactly(newDirectCertId);
    }

    @Test
    public void getMassData_successChangeCert_whenUpdateExistingCertWithDuplicates() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long clientId = clientInfo.getClientId().asLong();
        Freelancer freelancer = defaultFreelancer(clientId);

        // эмулируем проблему с дублями сертификатов
        FreelancerCertificate certificate =
                new FreelancerCertificate().withCertId(1L).withType(FreelancerCertificateType.DIRECT);
        freelancer.withCertificates(asList(certificate, certificate));

        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        // обновление должно не упасть и устранить дубликат
        long newDirectCertId = 2L;
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerInfo.getClientId().asLong())
                .setDirectCertId(newDirectCertId);

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();
        assertThat(result)
                .extracting(IntToolFreelancerCard::getDirectCertId)
                .containsExactly(newDirectCertId);
    }

    @Test
    public void getMassData_successChangeVisibility() {
        // подготовка
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer freelancer =
                defaultFreelancer(clientInfo.getClientId().asLong());
        freelancer.withCertificates(
                singletonList(new FreelancerCertificate().withCertId(1L).withType(FreelancerCertificateType.DIRECT)));
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        // проверка
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerInfo.getClientId().asLong())
                .setVisibility(FreelancerUpdateParameters.Visibility.INVISIBLE);

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();
        assertThat(result)
                .extracting(IntToolFreelancerCard::getSearchable)
                .containsExactly(false);
    }

    @Test
    public void getMassData_successChangeVisibility_whenNoCard() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer freelancer =
                defaultFreelancer(clientInfo.getClientId().asLong())
                        .withCard(null);
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerInfo.getClientId().asLong())
                .setVisibility(FreelancerUpdateParameters.Visibility.INVISIBLE);

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();
        assertThat(result)
                .extracting(IntToolFreelancerCard::getSearchable)
                .containsExactly(false);
    }

    @Test
    public void getMassData_successChangeContacts_whenNoCard() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer freelancer =
                defaultFreelancer(clientInfo.getClientId().asLong())
                        .withCard(null);
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        long freelancerId = freelancerInfo.getClientId().asLong();
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerId)
                .setEmail("ya-freelancer@ya.ru")
                .setPhone("+71234567890");

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();

        Freelancer updatedFreelancer = freelancerService.getFreelancers(singleton(freelancerId)).get(0);
        FreelancerCard updatedCard = updatedFreelancer.getCard();
        assertThat(updatedCard.getContacts())
                .satisfies(contacts -> assertSoftly(softly -> {
                    softly.assertThat(contacts.getEmail()).isEqualTo("ya-freelancer@ya.ru");
                    softly.assertThat(contacts.getPhone()).isEqualTo("+71234567890");
                }));
    }

    @Test
    public void getMassData_success_whenUpdateCertLogin() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Freelancer freelancer = defaultFreelancer(clientInfo.getClientId().asLong());
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);

        long freelancerId = freelancerInfo.getClientId().asLong();
        String newCertLogin = "xyz";
        FreelancerUpdateParameters updateParameters = new FreelancerUpdateParameters()
                .setClientId(freelancerId)
                .setCertLogin(newCertLogin);

        List<IntToolFreelancerCard> result = testedTool.getMassData(updateParameters);
        assertThat(result).isNotEmpty();

        Freelancer updatedFreelancer = freelancerService.getFreelancers(singleton(freelancerId)).get(0);
        String certLogin = updatedFreelancer.getCertLogin();
        assertThat(certLogin).isEqualTo(newCertLogin);
    }
}
