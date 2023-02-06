package ru.yandex.market.takeout.service;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;
import ru.yandex.market.request.trace.RequestTraceUtil;
import ru.yandex.market.takeout.common.DangerousResponseProcessingFunction;
import ru.yandex.market.takeout.common.TakeoutAsyncHttpClient;
import ru.yandex.market.takeout.config.ModuleDescription;
import ru.yandex.market.takeout.config.ModulesHolder;

public class NewMarketUidProviderImplTest {
    @Test
    public void deserializationTest() throws ExecutionException, InterruptedException {
        NewMarketUidProviderImpl ch = new NewMarketUidProviderImpl(getTvmClient(), getHttpClient(),
                new ModulesHolder(Collections.singletonMap(Module.CHECKOUTER, new ModuleDescription("http://localhost"
                        , "ch"))));
        Long aLong = ch.getNewMarketUid(new RequestContext(RequestTraceUtil.generateRequestId())).get();
        Assert.assertNotEquals(0L, aLong.longValue());
    }

    @NotNull
    private TakeoutAsyncHttpClient getHttpClient() {
        return new TakeoutAsyncHttpClient() {
            @Override
            public <T> CompletableFuture<T> execute(RequestContext requestContext,
                                                    Consumer<RequestBuilder> requestCustomizer,
                                                    Consumer<RequestLogRecordBuilder> requestLogCustomizer,
                                                    DangerousResponseProcessingFunction<T> responseProcessingFunction
                    , Set<Integer> acceptCodes) {
                CompletableFuture<T> tCompletableFuture = new CompletableFuture<>();

                try {
                    T apply = responseProcessingFunction.apply(new Response() {
                        @Override
                        public int getStatusCode() {
                            return 0;
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
                            return null;
                        }

                        @Override
                        public String getResponseBody() {
                            return "{\"muid\":1234567890}";
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
                    });
                    tCompletableFuture.complete(apply);

                } catch (Exception exception) {
                    tCompletableFuture.completeExceptionally(exception);
                }
                return tCompletableFuture;
            }
        };
    }

    @NotNull
    private AsyncTvmClient getTvmClient() {
        return new AsyncTvmClient() {
            @Override
            public CompletableFuture<Long> getUid(String userTicket, RequestContext context) {
                return null;
            }

            @Override
            public CompletableFuture<Map<String, String>> getServiceTickets(Set<String> destinations,
                                                                            RequestContext context) {
                Map<String, String> collect = destinations.stream().collect(Collectors.toMap(o -> o, o -> o));
                return CompletableFuture.completedFuture(collect);
            }

            @Override
            public CompletableFuture<Void> checkServiceTicketForTvmIds(String serviceTicket, Set<Long> tvmIds,
                                                                       RequestContext context) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
