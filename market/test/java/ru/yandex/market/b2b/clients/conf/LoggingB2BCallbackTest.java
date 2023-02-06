package ru.yandex.market.b2b.clients.conf;

import javax.annotation.Nonnull;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.yandex.market.b2b.clients.conf.ClientsLoggingAdapterConfiguration.LoggingB2BCallback;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class LoggingB2BCallbackTest {

    @Mock
    private Call<Object> call;

    private CheckingLogCallLoggingB2BCallback<Object> callback;

    @BeforeEach
    public void beforeEach() {
        callback = new CheckingLogCallLoggingB2BCallback<>(new CheckingMethodCallsCallback<>());
    }

    @Test
    public void ifResponseIsSuccessThenDoesNotCallLog() {
        Response<Object> response = Response.success(null);

        callback.onResponse(call, response);

        assertFalse(callback.logCalled);
    }

    @Test
    public void ifResponseIsUnsuccessfulThenCallsLog() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Error"));
        setUpCall();

        callback.onResponse(call, response);

        assertTrue(callback.logCalled);
    }

    private void setUpCall() {
        Mockito.doReturn(new Request.Builder().url("http://example.com").build()).when(call).request();
    }

    @Test
    public void ifResponseIsUnsuccessfulThenBuildsCorrectMessage() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Error message"));
        setUpCall();

        callback.onResponse(call, response);

        String message = callback.message;;
        assertTrue(message.contains("HTTP "));
        assertTrue(message.contains("404"));
        assertTrue(message.contains("GET"));
        assertTrue(message.contains("http://example.com"));
    }

    @Test
    public void ifHasResponseBodyThenLogsBody() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), "Response body message"));
        setUpCall();

        callback.onResponse(call, response);

        assertTrue(callback.message.contains("Response body message"));
    }

    @Test
    public void ifDoesNotHaveResponseBodyThenLogsDefault() {
        Response<Object> response = Response.error(404, ResponseBody.create(MediaType.get("text/html"), ""));
        setUpCall();

        callback.onResponse(call, response);

        assertTrue(callback.message.contains("Error"));
    }

    private static class CheckingLogCallLoggingB2BCallback<T> extends LoggingB2BCallback<T> {

        boolean logCalled = false;
        String message = "";

        public CheckingLogCallLoggingB2BCallback(@Nonnull Callback<T> callback) {
            super(callback);
        }

        @Override
        void log(String message) {
            logCalled = true;
            this.message = message;
        }
    }

    private static class CheckingMethodCallsCallback<T> implements Callback<T> {

        boolean onResponseCalled = false;

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            onResponseCalled = true;
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            throw new UnsupportedOperationException();
        }
    }
}
