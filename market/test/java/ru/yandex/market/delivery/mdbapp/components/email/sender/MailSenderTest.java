package ru.yandex.market.delivery.mdbapp.components.email.sender;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbapp.components.email.Mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MailSenderTest {

    private static final String REQUEST_URI = "http://ya.ru/send";
    private static final String MAIL_SENDER = "mail-sender@ya.ru";
    private static final String ORDERS_RECEIVER = "toOrders@ya.ru";

    private MockRestServiceServer server;
    private ObjectMapper mapper;
    private MailSender sender;
    private RestTemplate template;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        template = new RestTemplate();
        sender = new MailSender(template, "token", REQUEST_URI);
        sender.setCommonJsonMapper(mapper);
        server = MockRestServiceServer.bindTo(template).build();
    }

    @Test
    public void testMailSent() {
        String response = createResponse("success");
        server.expect(requestTo(REQUEST_URI))
            .andExpect(method(HttpMethod.POST))
            .andExpect(request -> {
                String subject = extractSubject(request);
                assertThat(subject).isEqualToIgnoringCase("mail subject");
            })
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        sender.send(createMail());
        server.verify();
    }

    @Test
    public void whenRestClientExceptionThenRuntimeExceptionShouldBeThrown() {
        template = mock(RestTemplate.class, (Answer) invocation -> {
            throw new RestClientException("ended with failure");
        });
        sender = new MailSender(template, "token", REQUEST_URI);
        assertThatThrownBy(() -> sender.send(createMail()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("ended with failure");
    }

    @Test
    public void whenTemplateReturn500RuntimeExceptionShoudBeThrown() {
        String response = createResponse("failure");
        server.expect(requestTo(REQUEST_URI))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response).contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> sender.send(createMail()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("ended with failure");
    }

    private String extractSubject(ClientHttpRequest request) throws IOException {
        String body = request.getBody().toString();
        String decodedBody = URLDecoder.decode(body, "UTF-8");
        String args = decodedBody
            .split("&")[0]
            .replace("args=", "");
        JsonNode jsonNode = mapper.readTree(args);
        return jsonNode.get("subject").textValue();
    }

    private String createResponse(String success) {
        SenderResponse response = new SenderResponse();
        response.setParams(params(success));
        SenderResponse.Result result = response.new Result();
        result.setStatus(success);
        result.setMessage("ended with " + success);
        response.setResult(result);
        return mapper.valueToTree(response).toString();
    }

    private static JsonNode params(String success) {
        return JsonNodeFactory.instance.objectNode().put(success, "params");
    }

    private static Mail createMail() {
        return new Mail(StandardCharsets.UTF_8, MAIL_SENDER, ORDERS_RECEIVER, MAIL_SENDER,
            "mail subject", "body");
    }
}
