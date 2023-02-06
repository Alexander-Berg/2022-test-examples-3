package ru.yandex.direct.core.entity.freelancer.service.utils;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.IntStreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.expert.client.model.Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DIRECT_TYPE_BEFORE;

public class FreelancerCertificatesUtilsTest {

    @Test
    public void convertSupportedCertificates_success() {
        //Ожидаемый результат
        FreelancerCertificateWithNames[] expected = getNewFreelancerCertificates();
        //Готовим тестовый набор
        List<Certificate> certificates = new ArrayList<>();
        for (FreelancerCertificateWithNames flCert : getNewFreelancerCertificates()) {
            //валидные
            Certificate activeCert = convertFromFlCertificate(flCert);
            certificates.add(activeCert);
            //неактивные
            Certificate notActiveCert = convertFromFlCertificate(flCert);
            notActiveCert.setCertId((notActiveCert.getCertId() + 10000));
            notActiveCert.setActive(0);
            certificates.add(notActiveCert);
            //фейковые типы
            Certificate fakeCert = convertFromFlCertificate(flCert);
            fakeCert.setCertId((fakeCert.getCertId() + 20000));
            fakeCert.getExam().setSlug("FAKE_NAME_" + fakeCert.getExam().getSlug());
            certificates.add(fakeCert);
        }
        //Конвертируем
        List<FreelancerCertificateWithNames> actual =
                FreelancerCertificatesUtils.convertSupportedCertificates(certificates);

        //Сверяем ожидания и полученный результат
        assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    private FreelancerCertificateWithNames[] getNewFreelancerCertificates() {
        FreelancerCertificateType[] freelancerCertificateTypes = FreelancerCertificateType.values();
        return IntStreamEx.range(0, freelancerCertificateTypes.length)
                .boxed()
                .map(index -> {
                    FreelancerCertificate freelancerCertificate = new FreelancerCertificate()
                            .withCertId(333L + index)
                            .withType(freelancerCertificateTypes[index]);
                    return new FreelancerCertificateWithNames()
                            .withFreelancerCertificate(freelancerCertificate)
                            .withConfirmedDate(DIRECT_TYPE_BEFORE)
                            .withFirstName("Имя")
                            .withSecondName("Фамилия");
                })
                .toArray(FreelancerCertificateWithNames[]::new);
    }

    private Certificate convertFromFlCertificate(FreelancerCertificateWithNames freelancerCertificate) {
        Certificate certificate = new Certificate();
        certificate.setFirstname(freelancerCertificate.getFirstName());
        certificate.setLastname(freelancerCertificate.getSecondName());
        certificate.setCertId(freelancerCertificate.getFreelancerCertificate().getCertId().intValue());
        certificate.setActive(1);
        certificate.setConfirmedDate(DIRECT_TYPE_BEFORE);
        Certificate.Exam exam = new Certificate.Exam();
        String slug = freelancerCertificate.getFreelancerCertificate().getType().name().replace("_", "-").toLowerCase();
        exam.setSlug(slug);
        certificate.setExam(exam);
        return certificate;
    }
}
