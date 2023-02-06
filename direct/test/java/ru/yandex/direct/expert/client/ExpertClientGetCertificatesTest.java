package ru.yandex.direct.expert.client;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.direct.expert.client.model.Certificate;
import ru.yandex.direct.test.utils.assertj.Conditions;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class ExpertClientGetCertificatesTest extends ExpertClientTestBase {
    private Long uid = 111222L;

    @Test
    public void getCertificates_CorrectUid() {
        List<Certificate> certificates = expertClient.getCertificates(uid);
        Certificate expected = createTestCertificate();
        softAssertions.assertThat(certificates).hasSize(1);
        softAssertions.assertThat(certificates.stream().findFirst().orElse(null))
                .is(Conditions.matchedBy(beanDiffer(expected)));
    }

    private Certificate createTestCertificate() {
        String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);

        Certificate certificate = new Certificate();
        certificate.setCertId(154388);

        certificate.setCertType("cert");
        certificate.setDueDate(LocalDateTime.parse("2019-10-29T21:00:00.000Z", formatter));
        certificate.setConfirmedDate(LocalDateTime.parse("2018-10-30T09:14:22.170Z", formatter));
        certificate.setFirstname("Ivan");
        certificate.setLastname("Ivanov");
        certificate.setActive(1);

        certificate.setImagePath("https://avatars.mdst.yandex.net/get-expert/3702/1540890867558_154388/orig");
        certificate.setPreviewImagePath("");

        Certificate.Exam exam = new Certificate.Exam();
        exam.setSlug("direct");
        exam.setId(1);

        Certificate.Service service = new Certificate.Service();
        service.setId(1);
        service.setCode("direct");
        service.setTitle("Яндекс.Директ");

        certificate.setService(service);
        certificate.setExam(exam);
        return certificate;
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getHeader(TvmHeaders.SERVICE_TICKET)).isEqualTo(TICKET_BODY);
                softAssertions.assertThat(request.getHeader("Content-type")).isEqualTo("application/json");
                softAssertions.assertThat(request.getPath()).isEqualTo("/certificates/findByUids");
                softAssertions.assertThat(request.getBody().readString(Charset.defaultCharset()))
                        .contains(uid.toString());
                return new MockResponse().setBody(JsonUtils.toJson(ImmutableMap.of(uid,
                        Collections.singletonList(createTestCertificate()))));
            }
        };
    }
}
