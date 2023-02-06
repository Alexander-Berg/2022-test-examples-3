package ru.yandex.market.tpl.common.sms;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = YaSmsProperties.class)
@TestPropertySource(properties = {
        "external.ya-sms.url=http://phone-passport-test.yandex.ru",
        "external.ya-sms.sender=market",
        "external.ya-sms.connectTimeoutMillis=1000",
        "external.ya-sms.readTimeoutMillis=2000",
        "external.ya-sms.maxTotal=10",
        "external.ya-sms.tvmClientId=1"
})
@Import(value = {MockTestConfiguration.class})
class YaSmsClientTest {

    private static final String PHONE = "+79779569483";
    private static final String TEXT = "Тестовая СМС";

    private MockRestServiceServer server;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("yaSmsCommonClient")
    private SmsClient yaSmsClient;

    @BeforeEach
    void initMockServer() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    private static List<Arguments> paymentCheckResults() {
        return List.of(
                Arguments.of("sms/ok.xml", SmsResult.ok(PHONE, TEXT)),
                Arguments.of("sms/error.xml", new SmsResult(PHONE, TEXT, SmsResultCode.LIMITEXCEEDED, "error-text"))
        );
    }

    @ParameterizedTest
    @MethodSource("paymentCheckResults")
    void send(String responseFile, SmsResult expectedResult) {
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(Matchers.endsWith("http://phone-passport-test.yandex" +
                        ".ru/sendsms?sender=market&route=market&phone=+79779569483&text=Тестовая%20СМС&utf8=1"))
        )
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(new ClassPathResource(responseFile), MediaType.APPLICATION_XML));

        Assertions.assertThat(yaSmsClient.send(PHONE, TEXT)).isEqualTo(expectedResult);
    }

}
