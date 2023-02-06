package ru.yandex.market.api.util.httpclient.spi;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestProcessor;

/**
 * Позволяет подменять ответ сервиса.
 */
public class MockRequestProcessor implements RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MockRequestProcessor.class);

    private static final String TEST_NAME = "";

    private final HttpExpectations httpExpectations;
    private final MockExternal external;
    private final ApplicationContext appContext;

    private AtomicInteger testCounter = new AtomicInteger(0);

    public MockRequestProcessor(
        HttpExpectations httpExpectations,
        MockExternal external,
        ApplicationContext appContext
    ) {
        this.httpExpectations = httpExpectations;
        this.external = external;
        this.appContext = appContext;
    }

    @Override
    public Future<HttpResponse> doCall(EventLoop executor,
                                       URI uri,
                                       HttpRequest request,
                                       String requestId,
                                       Object chunks) {

        List<String> keys = Lists.newArrayListWithCapacity(2);
        for (Integer invocation : Lists.newArrayList(testCounter.getAndIncrement(), null)) {
            String key = MockHelper.key(request.method().name(),
                                        request.headers().get(HttpHeaderNames.CONTENT_MD5),
                                        TEST_NAME,
                                        uri.toASCIIString(),
                                        external.getIgnoredParams(),
                                        invocation);
            keys.add(key);
            LOG.debug("#moc_request_processor, check, {}", key);
            MockResource resource = external.getResource(key);

            if (Objects.nonNull(resource)) {
                return process(resource);
            }
        }


        Result<Future<HttpResponse>, String> handledResponse = tryHandlePredefinedRequest(uri, request);
        if (handledResponse.isOk())
            return handledResponse.getValue();

        return Futures.newFailedFuture(new UnmatchedHttpRequestException(keys.toString()));
    }

    private ru.yandex.market.http.HttpRequest convertRequest(URI uri, HttpRequest request) {
        return new ru.yandex.market.http.HttpRequest(request.method(), uri, request.headers(), getBody(request));
    }

    private byte[] getBody(HttpRequest request) {
        if (request instanceof FullHttpRequest) {
            ByteBuf content = ((FullHttpRequest) request).content();
            if (content != null) {
                return content.array();
            }
        }
        return null;
    }

    private Throwable getError(HttpErrorType error) {
        switch (error) {
            case CONNECT_TIMEOUT:
                return new ConnectTimeoutException();
            case PROCESS_TIMEOUT:
                return new CancellationException();
            default:
                throw new IllegalArgumentException(String.format("invalid errorType = %s", error.toString()));
        }
    }

    @NotNull
    private Throwable getError(MockResource resource) {
        switch (resource.getError()) {
            case "CONNECT_TIMEOUT":
                return new ConnectTimeoutException();

            case "PROCESS_TIMEOUT":
                return new CancellationException();

            default:
                return new RuntimeException();
        }
    }

    private Future<HttpResponse> process(MockResource resource) {
        try {
            if (!Strings.isNullOrEmpty(resource.getError())) {
                Throwable e = getError(resource);
                if (resource.getTimeout() > 0) {
                    Promise<HttpResponse> result = Futures.newPromise();
                    Futures.eventLoop().schedule(() -> result.tryFailure(e), resource.getTimeout(), TimeUnit.MILLISECONDS);
                    return result;
                } else {
                    return Futures.newFailedFuture(e);
                }

            } else {
                Resource source = appContext.getResource(resource.getValue());

                //ClassPathResource source = new ClassPathResource(resource.getValue());
                byte[] data = IOUtils.readFully(source.getInputStream(), (int) source.contentLength());
                HttpResponse response = HttpResponse.of(resource.getStatus(), new ArrayList<>(), data);

                if (resource.getTimeout() > 0) {
                    Promise<HttpResponse> result = Futures.newPromise();
                    Futures.eventLoop().schedule(() -> result.trySuccess(response), resource.getTimeout(), TimeUnit.MILLISECONDS);
                    return result;
                } else {
                    return Futures.newSucceededFuture(response);
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Result<Future<HttpResponse>, String> tryHandlePredefinedRequest(URI uri, HttpRequest request) {
        ru.yandex.market.http.HttpRequest r = convertRequest(uri, request);
        Result<PredefinedHttpResponse, String> found = httpExpectations.tryResolve(r);
        if (found.hasError()) {
            return Result.newError(String.format("cant find match for request [%s %s]", request.method().toString(), uri.toString()));
        }
        ru.yandex.market.api.util.httpclient.spi.HttpOptions options = found.getValue().getOptions();
        if (options.getError() != null) {
            Throwable e = getError(options.getError());
            if (options.getTimeout() > 0) {
                Promise<HttpResponse> result = Futures.newPromise();
                Futures.eventLoop().schedule(() -> result.tryFailure(e), options.getTimeout(), TimeUnit.MILLISECONDS);
                return Result.newResult(result);
            } else {
                return Result.newResult(Futures.newFailedFuture(e));
            }
        }
        if (options.hasTimeout()) {
            Promise<HttpResponse> result = Futures.newPromise();
            Futures.eventLoop().schedule(() -> result.trySuccess(found.getValue().getResponse()),
                                         options.getTimeout(),
                                         TimeUnit.MILLISECONDS);
            return Result.newResult(result);
        }
        return Result.newResult(Futures.newSucceededFuture(found.getValue().getResponse()));
    }
}
