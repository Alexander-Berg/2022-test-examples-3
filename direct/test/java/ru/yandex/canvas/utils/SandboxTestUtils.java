package ru.yandex.canvas.utils;

import java.util.Objects;
import java.util.function.Function;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpStatus;

public class SandboxTestUtils {
    public static Dispatcher makeSandboxDispatcher(Function<RecordedRequest, MockResponse> requestSupplier) {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (!"OAuth sandboxTestToken".equals(request.getHeader("Authorization"))) {
                    return new MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value());
                }

                MockResponse requestApplyResult = requestSupplier.apply(request);

                return Objects.requireNonNullElseGet(requestApplyResult,
                        () -> new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
            }
        };
    }
}
