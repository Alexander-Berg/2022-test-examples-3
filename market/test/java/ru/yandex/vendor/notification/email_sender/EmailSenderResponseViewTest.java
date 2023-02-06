package ru.yandex.vendor.notification.email_sender;

import com.amazonaws.util.StringInputStream;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import ru.yandex.vendor.notification.email_sender.EmailSenderResultContainer.Status;
import ru.yandex.vendor.notification.email_sender.parser.sender.EmailSenderResponseParser;

import java.io.*;
import java.net.URI;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Ignore
@RunWith(JUnit4.class)
public class EmailSenderResponseViewTest {

    private static final String SUCCESS_RESPONSE_STR = "{" +
            "   \"params\": {" +
            "       \"control\": {" +
            "           \"async\":false," +
            "           \"countdown\":null," +
            "           \"expires\":86400," +
            "           \"for_testing\":false" +
            "       }," +
            "       \"source\": {" +
            "           \"to_email\":\"vsubhuman@yandex-team.ru\"," +
            "           \"args\": \"{}\"," +
            "           \"header\":[]," +
            "           \"ignore_empty_email\":false" +
            "       }" +
            "   }," +
            "   \"result\": {" +
            "       \"status\":\"OK\"," +
            "       \"task_id\":\"task-id-12345\"" +
            "   }" +
            "}";

    private static final String FAIL_RESPONSE_STR = "{" +
            "   \"params\": {" +
            "       \"control\": {" +
            "           \"async\":false," +
            "           \"countdown\":null," +
            "           \"expires\":86400," +
            "           \"for_testing\":false" +
            "       }," +
            "       \"source\": {" +
            "           \"to_email\":\"vsubhuman@yandex-team.ru\"," +
            "           \"args\": \"{}\"," +
            "           \"header\":[]," +
            "           \"ignore_empty_email\":false" +
            "       }" +
            "   }," +
            "   \"result\": {" +
            "       \"status\":\"ERROR\"," +
            "       \"error\":\"error-message-12345\"" +
            "   }" +
            "}";

    private final EmailSenderResponseParser emailSenderResponseParser;

    public EmailSenderResponseViewTest() {
        this.emailSenderResponseParser = new EmailSenderResponseParser();
    }

    private static RestTemplate createRestTemplate(String responseBody) throws UnsupportedEncodingException {
        return new RestTemplate() {
            @Override
            protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
                return new ClientHttpRequest() {
                    @Override
                    public ClientHttpResponse execute() throws IOException {
                        return new ClientHttpResponse() {
                            @Override
                            public HttpStatus getStatusCode() throws IOException {
                                return HttpStatus.OK;
                            }

                            @Override
                            public int getRawStatusCode() throws IOException {
                                return 200;
                            }

                            @Override
                            public String getStatusText() throws IOException {
                                return "OK";
                            }

                            @Override
                            public void close() {

                            }

                            @Override
                            public InputStream getBody() throws IOException {
                                return new StringInputStream(responseBody);
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                return headers;
                            }
                        };
                    }

                    @Override
                    public OutputStream getBody() throws IOException {
                        return new ByteArrayOutputStream();
                    }

                    @Override
                    public HttpMethod getMethod() {
                        return method;
                    }

                    @Override
                    public String getMethodValue() {
                        return "POST";
                    }

                    @Override
                    public URI getURI() {
                        return url;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return new HttpHeaders();
                    }
                };
            }
        };
    }

    @Test
    public void view_parses_success_map_into_ok_result() throws Exception {
        EmailSenderResultContainer<EmailSendResult> result = null;
        assertEquals(Status.OK, result.getStatus());
        assertEquals("task-id-12345", result.getValue().getTaskId());
    }

    @Test
    public void view_parses_error_map_into_error_result() throws Exception {
        EmailSenderResultContainer<EmailSendResult> result = null;
        assertEquals(Status.ERROR, result.getStatus());
        Exception error = result.getError();
        assertThat(error, instanceOf(EmailSenderResponseException.class));
        assertEquals("qwe", error.getMessage());
    }

    @Test
    public void rest_template_parses_ok_view() throws Exception {
        RestTemplate restTemplate = createRestTemplate(SUCCESS_RESPONSE_STR);
        String view = restTemplate.postForObject("qwe", "", String.class);
        EmailSenderResultContainer<EmailSendResult> result = null;
        assertEquals(Status.OK, result.getStatus());
        assertEquals("task-id-12345", result.getValue().getTaskId());
    }

    @Test
    public void rest_template_parses_error_view() throws Exception {
        RestTemplate restTemplate = createRestTemplate(FAIL_RESPONSE_STR);
        String view = restTemplate.postForObject("qwe", "", String.class);
        EmailSenderResultContainer<EmailSendResult> result = null;
        assertEquals(Status.ERROR, result.getStatus());
        Exception error = result.getError();
        assertThat(error, instanceOf(EmailSenderResponseException.class));
        assertEquals("error-message-12345", error.getMessage());
    }
}
