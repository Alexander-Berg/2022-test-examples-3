package ru.yandex.market.jmf.module.angry.test;

import com.fasterxml.jackson.databind.JsonNode;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.module.angry.AngrySpaceClient;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SendChatMessageRequest;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SendItemRequest;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SendingErrorResponse;
import ru.yandex.market.jmf.module.angry.exception.AngrySpaceSendingException;
import ru.yandex.market.jmf.module.angry.impl.AngrySpaceClientImpl;
import ru.yandex.market.jmf.module.angry.impl.AngrySpaceTokenSecretSupplier;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AngrySpaceClientTest {

    private AngrySpaceClient angrySpaceClient;
    @Mock
    private HttpClientFactory httpClientFactory;

    @Mock
    private HttpClient httpClient;

    @Mock
    private ObjectSerializeService objectSerializeService;

    @BeforeEach
    void setUp() {
        when(httpClientFactory.create(any())).thenReturn(httpClient);

        angrySpaceClient = new AngrySpaceClientImpl(
                mock(AngrySpaceTokenSecretSupplier.class),
                httpClientFactory,
                objectSerializeService);
    }

    @AfterEach
    void tearDown() {
        reset(httpClient);
        reset(httpClientFactory);
        reset(objectSerializeService);
    }

    // TestPalm https://testpalm.yandex-team.ru/testcase/ocrm-1575
    @Test
    void shouldThrowSendingExceptionOnSendChatMessage400Error() {
        var request = new SendChatMessageRequest();
        request.setText("Hello dude");

        tuneResponseMocks(400, new SendingErrorResponse("ERROR_CODE", "ERROR", null));

        var thrown = Assertions.assertThrows(AngrySpaceSendingException.class, () ->
                angrySpaceClient.sendChatMessage("Any", request));
        assertEquals("Произошла ошибка отправки: ERROR", thrown.getDisplayMessage());
        assertEquals("Sending error: 400 ERROR. data: null", thrown.getMessage());
    }

    // TestPalm https://testpalm.yandex-team.ru/testcase/ocrm-1575
    @Test
    void shouldThrowSendingExceptionOnSendItem500Error() {
        var request = new SendItemRequest();
        request.setText("Hello dude");
        tuneResponseMocks(503, new SendingErrorResponse("ERROR_CODE", "ERROR", null));

        var thrown = Assertions.assertThrows(AngrySpaceSendingException.class, () ->
                angrySpaceClient.sendItem("Any", request));
        assertEquals("Произошла ошибка отправки: ERROR", thrown.getDisplayMessage());
        assertEquals("Sending error: 503 ERROR. data: null", thrown.getMessage());
    }

    // TestPalm https://testpalm.yandex-team.ru/testcase/ocrm-1575
    @Test
    void shouldThrowForbiddenSendingExceptionOnSendItem403Error() {
        var request = new SendItemRequest();
        request.setText("Hello dude");
        tuneResponseMocks(403, new SendingErrorResponse("ERROR_CODE", "ERROR", null));

        var thrown = Assertions.assertThrows(AngrySpaceSendingException.class, () ->
                angrySpaceClient.sendItem("Any", request));
        assertEquals("Произошла ошибка отправки: Недостаточно прав для отправки сообщения", thrown.getDisplayMessage());
        assertEquals("Sending error: 403 Forbidden", thrown.getMessage());
    }

    // TestPalm https://testpalm.yandex-team.ru/testcase/ocrm-1575
    @Test
    void shouldNotThrowExceptionOnOrdinarySendMessage() {
        var request = new SendChatMessageRequest();
        request.setText("Hello dude");
        tuneResponseMocks(200, null);

        when(objectSerializeService.deserialize(ArgumentMatchers.<byte[]>any(), eq(JsonNode.class)))
                .thenReturn(null);

        angrySpaceClient.sendChatMessage("Any", request);
    }

    private void tuneResponseMocks(Integer statusCode, SendingErrorResponse errorResponse) {
        var response = Mockito.mock(Response.class);
        when(response.getStatusCode()).thenReturn(statusCode);
        when(httpClient.execute(any())).thenReturn(new HttpResponse(response));
        when(objectSerializeService.deserialize(ArgumentMatchers.<byte[]>any(), eq(SendingErrorResponse.class)))
                .thenReturn(errorResponse);
    }
}
