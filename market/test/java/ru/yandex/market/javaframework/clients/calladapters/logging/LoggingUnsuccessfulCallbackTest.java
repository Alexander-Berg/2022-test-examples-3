package ru.yandex.market.javaframework.clients.calladapters.logging;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.yandex.market.common.retrofit.HeaderSet;
import ru.yandex.market.javaframework.clients.retry.MJExecuteCall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LoggingUnsuccessfulCallbackTest {

    @Mock
    private Call<Object> call;

    @Mock
    private MJExecuteCall.MJSingleExecution<Object> next;

    @InjectMocks
    private CheckingLogCallsLoggingUnsuccessfulCallback<Object> callback;

    @Test
    public void onResponse_ifResponseIsUnsuccessfulTheCallsNext() {
        setUpCall();
        Response<Object> response = Response.error(
            404,
            ResponseBody.create(MediaType.get("text/html"), "Error"));

        callback.onResponse(call, response);

        verify(next).onResponse(any(), any());
    }

    public void setUpCall() {
        doReturn(new Request.Builder().url("http://example.com").build()).when(call).request();
    }

    @Test
    public void onResponse_ifResponseIsSuccessfulTheCallsNext() {
        Response<Object> response = Response.success(null);

        callback.onResponse(call, response);

        verify(next).onResponse(any(), any());
    }

    @Test
    public void onResponse_ifResponseIsUnsuccessfulTheCallsLog() {
        setUpCall();
        Response<Object> response = Response.error(
            404,
            ResponseBody.create(MediaType.get("text/html"), "Error"));

        callback.onResponse(call, response);

        assertTrue(callback.isCalledLogResponse);
        assertTrue(callback.isCalledLog);
    }

    @Test
    public void onResponse_ifResponseIsSuccessfulTheDoesNotCallsLog() {
        Response<Object> response = Response.success(null);

        callback.onResponse(call, response);

        assertFalse(callback.isCalledLogResponse);
        assertFalse(callback.isCalledLog);
    }

    @Test
    public void onFailure_callsNext() {
        callback.onFailure(call, new RuntimeException());

        verify(next).onFailure(any(), any());
    }

    @Test
    public void buildLogMessage_ifStatusCodeIsNotZeroThenHaveStatusCode() {
        setUpCall();

        String text = callback.buildLogMessage(call, Response.success(null), new RuntimeException());

        assertTrue(text.contains("HTTP 200"));
    }

    @Test
    public void buildLogMessage_ifNoResponseThenDoesNotHaveStatusCode() {
        setUpCall();

        String text = callback.buildLogMessage(call, null, new RuntimeException());

        assertFalse(text.contains("HTTP"));
    }

    @Test
    public void buildLogMessage_containsExceptionMessage() {
        String exceptionMessage = "Exception message";
        setUpCall();

        String text = callback.buildLogMessage(call, null, new RuntimeException(exceptionMessage));

        assertTrue(text.contains(exceptionMessage));
    }

    @Test
    public void buildLogMessage_containsMethodAndUrlFromRequest() {
        doReturn(new Request.Builder().get().url("http://example.com").build()).when(call).request();

        String text = callback.buildLogMessage(call, null, new RuntimeException());

        assertTrue(text.contains("GET http://example.com"));
    }

    @Test
    public void buildLogMessage_ifResponseIsNullThenDoesNotHaveResponseIdentity() {
        Response<Object> response = null;
        setUpCall();

        String text = callback.buildLogMessage(call, response, new RuntimeException());

        assertFalse(text.contains("Response Identity"));
    }

    @Test
    public void buildLogMessage_ifResponseIsNotNullThenDoesNotHaveResponseIdentity() {
        Response<Object> response = Response.success(null);
        setUpCall();
        HeaderSet headerSet = mock(HeaderSet.class);
        doReturn(headerSet).when(next).headerSet();
        doReturn("Response Identity").when(headerSet).getResponseIdentity(response);

        String text = callback.buildLogMessage(call, response, new RuntimeException());

        assertTrue(text.contains("Response Identity"));
    }

    @Test
    public void getErrorBody_ifResponseIsNullThenNull() {
        Response<Object> response = null;

        String result = callback.getErrorBody(response);

        Assertions.assertNull(result);
    }

    @Test
    public void getErrorBody_ifErrorIsNullThenNull() {
        Response<Object> response = Response.success(null);

        String result = callback.getErrorBody(response);

        Assertions.assertNull(result);
    }

    @Test
    public void getErrorBody_ifErrorIsNotNullThenErrorBody() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Error body"));

        String result = callback.getErrorBody(response);

        assertEquals("Error body", result);
    }

    @Test
    public void getErrorBody_zeroLengthBodyIsEmpty() {
        Response<Object> response = Response.error(
            404,
            ResponseBody.create(MediaType.get("text/html"), 0L, new Buffer()));

        String result = callback.getErrorBody(response);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getErrorBody_canBeCalledMultipleTimesWithTheSameResult() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Error body"));

        String result1 = callback.getErrorBody(response);
        String result2 = callback.getErrorBody(response);

        assertEquals("Error body", result1);
        assertEquals("Error body", result2);
    }

    @Test
    public void getErrorBody_doesNotConsumeResponseBody() throws IOException {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Error body"));

        String result = callback.getErrorBody(response);

        assertEquals("Error body", result);
        assertEquals("Error body", response.errorBody().string());  // Consuming ResponseBody
    }

    private static class CheckingLogCallsLoggingUnsuccessfulCallback<T> extends LoggingUnsuccessfulCallback<T> {

        boolean isCalledLogResponse = false;
        boolean isCalledLog = false;

        CheckingLogCallsLoggingUnsuccessfulCallback(@Nonnull Callback<T> callback) {
            super(callback);
        }

        @Override
        void logUnsuccessfulResponse(Call<T> call, Response<T> response) {
            isCalledLogResponse = true;
            super.logUnsuccessfulResponse(call, response);
        }

        @Override
        void log(IllegalStateException exception, String text) {
            isCalledLog = true;
        }
    }
}
