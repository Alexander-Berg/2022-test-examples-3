package ru.yandex.market.takeout.service;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.google.common.base.Throwables;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import junit.framework.TestCase;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;
import ru.yandex.market.takeout.common.DangerousResponseProcessingFunction;
import ru.yandex.market.takeout.common.TakeoutAsyncHttpClient;
import ru.yandex.market.takeout.config.ModuleDescription;

public abstract class RequestProcessorTestCase extends TestCase {
    protected final static TakeoutAsyncHttpClient EXCEPTIONAL_HTTP_CLIENT =
            new TakeoutAsyncHttpClient() {
                @Override
                public <T> CompletableFuture<T> execute(RequestContext requestContext,
                                                        Consumer<RequestBuilder> requestCustomizer,
                                                        Consumer<RequestLogRecordBuilder> requestLogCustomizer,
                                                        DangerousResponseProcessingFunction<T> responseProcessingFunction, Set<Integer> acceptCodes) {
                    CompletableFuture<T> response = new CompletableFuture<>();
                    response.completeExceptionally(new TakeoutRequestProcessorTestCase.HttpException());
                    return response;
                }
            };

    protected final static Map<Module, ModuleDescription> MODULE_MAP;

    static {
        MODULE_MAP = new HashMap<>();
        MODULE_MAP.put(null, new ModuleDescription("http://localhost", null));
    }

    protected static <T> void unwrapExecutionException(CompletableFuture<T> status) throws Throwable {
        try {
            status.get();
        } catch (ExecutionException e) {
            throw Throwables.getRootCause(e);
        }
    }

    @NotNull
    protected TakeoutAsyncHttpClient getSuccessfulHttpClient(String responseBody) {
        return new TakeoutAsyncHttpClient() {
            @Override
            public <T> CompletableFuture<T> execute(RequestContext requestContext,
                                                    Consumer<RequestBuilder> requestCustomizer,
                                                    Consumer<RequestLogRecordBuilder> requestLogCustomizer,
                                                    DangerousResponseProcessingFunction<T> responseProcessingFunction
                    , Set<Integer> acceptCodes) {
                return CompletableFuture.completedFuture(getHttpResponseMock(responseBody))
                        .thenCompose(response -> {
                            CompletableFuture<T> result = new CompletableFuture<>();
                            try{
                                T processed = responseProcessingFunction.apply(response);
                                result.complete(processed);
                            }
                            catch (Exception e){
                                result.completeExceptionally(e);
                            }
                            return result;
                        });
            }
        };
    }

    Response getHttpResponseMock(String responseBody) {
        return new Response() {
            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public String getStatusText() {
                return null;
            }

            @Override
            public byte[] getResponseBodyAsBytes() {
                return new byte[0];
            }

            @Override
            public ByteBuffer getResponseBodyAsByteBuffer() {
                return null;
            }

            @Override
            public InputStream getResponseBodyAsStream() {
                return null;
            }

            @Override
            public String getResponseBody(Charset charset) {
                return responseBody;
            }

            @Override
            public String getResponseBody() {
                return responseBody;
            }

            @Override
            public Uri getUri() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public String getHeader(CharSequence name) {
                return null;
            }

            @Override
            public List<String> getHeaders(CharSequence name) {
                return null;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public boolean isRedirected() {
                return false;
            }

            @Override
            public String toString() {
                return null;
            }

            @Override
            public List<Cookie> getCookies() {
                return null;
            }

            @Override
            public boolean hasResponseStatus() {
                return false;
            }

            @Override
            public boolean hasResponseHeaders() {
                return false;
            }

            @Override
            public boolean hasResponseBody() {
                return false;
            }

            @Override
            public SocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public SocketAddress getLocalAddress() {
                return null;
            }
        };
    }
}
