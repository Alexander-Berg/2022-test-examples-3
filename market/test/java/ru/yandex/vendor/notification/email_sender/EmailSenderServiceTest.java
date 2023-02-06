package ru.yandex.vendor.notification.email_sender;

import com.amazonaws.util.Base64;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.yandex.vendor.notification.email_sender.parser.email.IEmailSenderParser;
import ru.yandex.vendor.notification.email_sender.parser.sender.EmailSenderResponseParser;
import ru.yandex.vendor.notification.email_sender.parser.sender.IEmailSenderResponseParser;
import ru.yandex.vendor.notification.templates.DefaultEmailTemplate;
import ru.yandex.vendor.notification.templates.IEmailTemplate;
import ru.yandex.vendor.notification.email_sender.EmailSenderResultContainer.Status;
import ru.yandex.vendor.notification.templates.ActiveBrandEditorRequestsTemplate;
import ru.yandex.vendor.util.IRestClient;
import ru.yandex.vendor.util.RestTemplateRestClient;
import ru.yandex.vendor.util.Utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.String.*;
import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static ru.yandex.vendor.notification.email_sender.EmailSenderClient.SENDER_SEND_HTTP_PATH_FORMAT;

@Ignore
@RunWith(JUnit4.class)
public class EmailSenderServiceTest {

    private static final IEmailTemplate TEMPLATE = new DefaultEmailTemplate("qweqwe", emptyMap());
    private static final EmailAddress ADDRESS = new EmailAddress("qwe@rty.com");

    private final RestTemplate restTemplateMock = createRestTemplateMock();
    private final IRestClient restClientMock = createRestClientMock(restTemplateMock);
    private final TestEmailSenderClient emailSenderClient = createEmailSenderClient(restClientMock);

    private final EmailSenderService senderService = createEmailSenderService(emailSenderClient);

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
            "       \"task_id\":\"qwerty\"" +
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

    @Rule
    public final ExpectedException expectedExceptionRule = ExpectedException.none();

    @Test
    public void send_email_with_enum_type_is_available() throws Exception {
        IEmailTemplate template = new ActiveBrandEditorRequestsTemplate(emptyList(), "");
        EmailAddress email = new EmailAddress("vsubhuman@yandex-team.ru");
        EmailSendType type = EmailSendType.SYNC;
        senderService.send(template, email, type);
    }

    @Test
    public void send_returns_result() throws Exception {
        EmailSenderResultContainer result = senderService.send(TEMPLATE, ADDRESS, EmailSendType.SYNC);
        assertNotNull(result);
        assertNotNull(result.getStatus());
    }

    @Test
    public void npe_is_thrown_on_no_template() throws Exception {
        expectedExceptionRule.expect(NullPointerException.class);
        expectedExceptionRule.expectMessage("template cannot be null!");
        senderService.send(null, ADDRESS, EmailSendType.SYNC);
    }

    @Test
    public void npe_is_thrown_on_no_address() throws Exception {
        expectedExceptionRule.expect(NullPointerException.class);
        expectedExceptionRule.expectMessage("emailAddress cannot be null!");
        senderService.send(TEMPLATE, null, EmailSendType.SYNC);
    }

    @Test
    public void npe_is_thrown_on_no_type() throws Exception {
        expectedExceptionRule.expect(NullPointerException.class);
        expectedExceptionRule.expectMessage("sendType cannot be null!");
        senderService.send(TEMPLATE, ADDRESS, null);
    }

    @Test
    public void send_with_no_type_is_sync() throws Exception {
        IEmailSenderService service = mock(EmailSenderService.class);
        when(service.send(any(IEmailTemplate.class), any(EmailAddress.class))).thenCallRealMethod();
        EmailSenderResultContainer sendResult = EmailSenderResultContainer.ok("zzz");
        when(service.send(TEMPLATE, ADDRESS, EmailSendType.SYNC)).thenReturn(sendResult);
        EmailSenderResultContainer result = service.send(TEMPLATE, ADDRESS);
        assertSame(sendResult, result);
    }

    @Test
    public void sendAsync_is_async() throws Exception {
        EmailSenderService service = mock(EmailSenderService.class);
        when(service.sendAsync(any(IEmailTemplate.class), any(EmailAddress.class))).thenCallRealMethod();
        EmailSenderResultContainer sendResult = EmailSenderResultContainer.ok("zzz");
        when(service.send(TEMPLATE, ADDRESS, EmailSendType.ASYNC)).thenReturn(sendResult);
        EmailSenderResultContainer result = service.sendAsync(TEMPLATE, ADDRESS);
        assertSame(sendResult, result);
    }

    @Test
    public void sender_calls_rest_template() throws Exception {
        senderService.send(TEMPLATE, ADDRESS);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + new String(Base64.encode(emailSenderClient.getSenderToken().getBytes())));
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("async", "false");
        body.add("args", "{}");
        IRestClient.Request<String> request = new IRestClient.Request<>(
            format(SENDER_SEND_HTTP_PATH_FORMAT, TEMPLATE.getKey()), String.class, headers)
                .addParameter("to_email", ADDRESS.getAddress());
        verify(restClientMock).postForObject(request, body);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sender_returns_ok_result_if_template_returned_success() throws Exception {
        doReturn(SUCCESS_RESPONSE_STR).when(restClientMock).postForObject(any(), any());
        EmailSenderResultContainer<EmailSendResult> result = senderService.send(TEMPLATE, ADDRESS);
        System.out.println(result);
        assertEquals(Status.OK, result.getStatus());
        assertEquals("qwerty", result.getValue().getTaskId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sender_returns_error_result_if_template_returned_fail() throws Exception {
        doReturn(FAIL_RESPONSE_STR).when(restClientMock).postForObject(any(), any());
        EmailSenderResultContainer result = senderService.send(TEMPLATE, ADDRESS);
        assertEquals(Status.ERROR, result.getStatus());
        Exception error = result.getError();
        assertThat(error, instanceOf(EmailSenderResponseException.class));
        assertEquals("qwerty", error.getMessage());
    }

    @Test
    @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
    public void sender_returns_error_result_if_template_throws_exception() throws Exception {
        Exception error = new RuntimeException("qweqwe");
        doThrow(error).when(restClientMock).postForObject(any(), any());
        EmailSenderResultContainer result = senderService.send(TEMPLATE, ADDRESS);
        assertEquals(Status.ERROR, result.getStatus());
        assertSame(error, result.getError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sender_calls_rest_template_with_specified_body_as_args() throws Exception {
        Object body = singletonMap("qwe", 42);
        senderService.send(new DefaultEmailTemplate("qweqwe", body), ADDRESS);
        ArgumentCaptor<MultiValueMap<String,Object>> requestCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(MultiValueMap.class);
        verify(restClientMock).postForObject(any(), requestCaptor.capture());
        MultiValueMap<String, Object> request = requestCaptor.getValue();
        Object args = request.getFirst("args");
        assertEquals("{\"qwe\":42}", args);
    }

    private static EmailSenderService createEmailSenderService(EmailSenderClient emailSenderClient) {
        return new EmailSenderService(emailSenderClient, new EmailSenderCampaignService(Collections.emptyMap()));
    }

    private static IRestClient createRestClientMock(RestTemplate restTemplate) {
        RestTemplateRestClient restClient = mock(RestTemplateRestClient.class);
        Mockito.doCallRealMethod().when(restClient).setRestTemplate(any());
        restClient.setRestTemplate(restTemplate);
        return restClient;
    }

    private static TestEmailSenderClient createEmailSenderClient(IRestClient restClient) {
        return new TestEmailSenderClient(restClient, new EmailSenderResponseParser(), "QWE");
    }

    @SuppressWarnings("unchecked")
    private static RestTemplate createRestTemplateMock() {
        RestTemplate mock = mock(RestTemplate.class);
        doReturn(FAIL_RESPONSE_STR).when(mock).postForObject(anyString(), any(), eq(String.class), anyMap());
        return mock;
    }

    public static class TestBean {

        public final long id;
        public final List<String> list;

        public TestBean(long id, Collection<String> list) {
            this.id = id;
            this.list = Utils.collectionToUnmodifiableList(list);
        }
    }

    static class TestEmailSenderClient extends EmailSenderClient {

        private String senderToken;

        public TestEmailSenderClient(IRestClient restClient, IEmailSenderResponseParser parser, String senderToken) {
            super(restClient, parser, senderToken);
            this.senderToken = senderToken;
        }

        public String getSenderToken() {
            return senderToken;
        }
    }
}
