package ru.yandex.market.pers.notify.external.sender;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.pers.notify.model.MailTemplate;
import ru.yandex.market.pers.notify.model.SenderAccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SenderClientTest {
    private static final String SEND_TRANSACTIONAL_RESULT_AS_STRING = "{status=OK, " +
            "message_id=<20180717140600.588634.aa9a6dc4688c4198b22a82b00c23a91f@ui-1.testing.ysendercloud>, " +
            "task_id=b86a3e84-ffb9-4a5f-9be2-1357a1db18e0}";

    private static final Map<String, Object> SEND_TRANSACTIONAL_RESULT_AS_MAP = new HashMap<String, Object>(){{
        put("status", "OK");
        put("message_id", "<20180717140600.588634.aa9a6dc4688c4198b22a82b00c23a91f@ui-1.testing.ysendercloud>");
        put("task_id", "b86a3e84-ffb9-4a5f-9be2-1357a1db18e0");
    }};

    private static CloseableHttpClient httpClient;
    private static SenderClient senderClient;

    private static MailTemplate mailTemplate;

    @BeforeAll
    static void setUp() {
        httpClient = mock(CloseableHttpClient.class);

        senderClient = new SenderClient(new SenderHttpClientProperties(Collections.emptyMap()),
            "example.com", 0, 0, 0, 1);
        senderClient.putClientForAccount(SenderAccount.MARKET, httpClient);

        mailTemplate = MailTemplate.of("some_id", SenderAccount.MARKET);
    }

    @Test
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    void testExtractSendTransactionalResult() throws Exception {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        when(responseMock.getEntity().getContent())
                .thenReturn(SenderClientTest.class.getResourceAsStream("/data/sender/send_transactional_response.json"));

        SenderClient.SenderBasicResultResponse response = SenderClient.extractSenderResult(responseMock);

        assertEquals(SEND_TRANSACTIONAL_RESULT_AS_STRING, response.asString());
        assertEquals(SEND_TRANSACTIONAL_RESULT_AS_MAP, response.asMap());
    }

    @Test
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    void testExtractGetUnsubscribedListContentResult() throws IOException {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        when(responseMock.getEntity().getContent()).thenReturn(
            SenderClientTest.class.getResourceAsStream("/data/sender/get_unsubscribed_emails_response.json"));
        Set<String> emails = SenderClient.extractGetUnsubscribedEmailsSenderResult(responseMock);
        assertEquals(ImmutableSet.of("test_1@example.com", "test_2@example.com"), emails);
    }

    @Test
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    void testExtractDeleteFromUnsubscribedListResult() throws IOException {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        when(responseMock.getEntity().getContent()).thenReturn(
                SenderClientTest.class.getResourceAsStream("/data/sender/delete_from_unsubscribed_list_response.json"));

        SenderClient.SenderBasicResultResponse response = SenderClient.extractSenderResult(responseMock);

        assertEquals("{status=ok}", response.asString());
        assertEquals(Collections.singletonMap("status", "ok"), response.asMap());
    }

    @Test
    void testSendTransactionMailSuccessWithMessageIdReturned() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_OK, "{ \"result\": { \"message_id\": \"some_id\" } }");

        assertEquals(SenderClient.SendTransactionalResponseStatus.SUCCESS, response.getStatus());
        assertEquals("some_id", response.getMessageId());
    }

    @Test
    void testSendTransactionMailSuccessWithNoMessageIdReturned() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_OK, "{ \"result\": {} }");

        assertEquals(SenderClient.SendTransactionalResponseStatus.SUCCESS, response.getStatus());
        assertNull(response.getMessageId());
    }

    @Test
    void testSendTransactionMailSuccessWithoutResultReturned() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_OK, "{}");

        assertEquals(SenderClient.SendTransactionalResponseStatus.SUCCESS, response.getStatus());
        assertNull(response.getMessageId());
    }

    @Test
    void testSendTransactionMailSuccessWithStringResultReturned() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_OK, "{ \"result\": \"success\" }");

        assertEquals(SenderClient.SendTransactionalResponseStatus.SUCCESS, response.getStatus());
        assertNull(response.getMessageId());
    }

    @Test
    void testSendTransactionMailClientError() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_BAD_REQUEST, "{}");

        assertEquals(SenderClient.SendTransactionalResponseStatus.CLIENT_ERROR, response.getStatus());
        assertNull(response.getMessageId());
    }

    @Test
    void testSendTransactionMailServerError() throws Exception {
        SenderClient.SendTransactionalResponse response =
            sendMail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "{}");

        assertEquals(SenderClient.SendTransactionalResponseStatus.SERVER_ERROR, response.getStatus());
        assertNull(response.getMessageId());
    }

    @Test
    void testSuccessReturnedGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_OK, "{ \"id\": \"123\" }");

        assertEquals("123", campaignId);
    }

    @Test
    void testNotSuccessCodeWhenGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_BAD_REQUEST, "{}");

        assertNull(campaignId);
    }

    @Test
    void testEmptyBodyReturnedGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_OK, "{}");

        assertNull(campaignId);
    }

    @Test
    void testNullIdReturnedGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_OK, "{ \"id\": null }");

        assertNull(campaignId);
    }

    @Test
    void testBlankIdReturnedGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_OK, "{ \"id\": \" \" }");

        assertNull(campaignId);
    }

    @Test
    void testNotStringIdReturnedGetCampaignId() throws Exception {
        String campaignId = getCampaignId(HttpStatus.SC_OK, "{ \"id\": {} }");

        assertNull(campaignId);
    }

    private static SenderClient.SendTransactionalResponse sendMail(int responseStatus, String responseBody)
        throws Exception {
        mockHttpClient(responseStatus, responseBody);
        return senderClient.sendTransactionalMail("example@example.com", mailTemplate, false, null, null);
    }

    private static String getCampaignId(int responseStatus, String responseBody)
        throws Exception {
        mockHttpClient(responseStatus, responseBody);
        return senderClient.getCampaignId(mailTemplate);
    }

    private static void mockHttpClient(int responseStatus, String responseBody) throws Exception {
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(responseStatus);
        when(httpResponse.getEntity().getContent()).thenReturn(
            new ByteArrayInputStream(responseBody.getBytes(Charset.forName("utf-8"))));

        when(httpClient.execute(any(), (HttpRequest) any(), (HttpContext) any())).thenReturn(httpResponse);
    }
}