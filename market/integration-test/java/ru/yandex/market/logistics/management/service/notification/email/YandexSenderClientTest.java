package ru.yandex.market.logistics.management.service.notification.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeDto;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexSenderClientTest extends AbstractContextualTest {

    private static final String URL = "https://sender-test/template-id/send";
    private static final String TEMPLATE_ID = "template-id";
    private static final String EMAIL_TO = "test@test.test";

    @Autowired
    @Qualifier("yandexSenderRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private YandexSenderClient yandexSenderClient;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setup() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void testSentOk() throws JsonProcessingException {
        mockRestServiceServer.expect(requestTo(equalTo(URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_ok_response.json"),
                MediaType.APPLICATION_JSON));

        CargoTypeDto dayOffDto = getDto();
        yandexSenderClient.send(dayOffDto, TEMPLATE_ID, EMAIL_TO);
        mockRestServiceServer.verify();
    }

    @Test
    void testSentError() throws JsonProcessingException {
        mockRestServiceServer.expect(requestTo(equalTo(URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_error_response.json"),
                MediaType.APPLICATION_JSON));

        CargoTypeDto dayOffDto = getDto();
        assertThrows(YandexSenderException.class, () -> yandexSenderClient.send(dayOffDto, TEMPLATE_ID, EMAIL_TO));
        mockRestServiceServer.verify();
    }

    @Test
    void testSentOkWithRetries() throws JsonProcessingException {
        mockRestServiceServer.expect(times(1), requestTo(equalTo(URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_ok_response.json"),
                MediaType.APPLICATION_JSON));

        CargoTypeDto dayOffDto = getDto();
        yandexSenderClient.sendWithRetries(dayOffDto, TEMPLATE_ID, EMAIL_TO);

        mockRestServiceServer.verify();
    }

    @Test
    void testSentErrorWithRetries() throws JsonProcessingException {
        mockRestServiceServer.expect(times(3), requestTo(equalTo(URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_error_response.json"),
                MediaType.APPLICATION_JSON));

        CargoTypeDto dayOffDto = getDto();
        yandexSenderClient.sendWithRetries(dayOffDto, TEMPLATE_ID, EMAIL_TO);

        mockRestServiceServer.verify();
    }

    private CargoTypeDto getDto() {
        return new CargoTypeDto(123L, 10, "description");
    }

    private MultiValueMap<String, String> getFormData() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(getDto());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("args", payload);
        body.add("to_email", EMAIL_TO);
        return body;
    }
}
