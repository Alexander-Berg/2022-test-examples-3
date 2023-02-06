package ru.yandex.market.api.listener.match;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import ru.yandex.market.api.listener.domain.HttpHeaders;
import ru.yandex.market.api.listener.domain.HttpMethod;
import ru.yandex.market.api.listener.domain.HttpStatus;
import ru.yandex.market.api.listener.expectations.*;
import ru.yandex.market.api.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.yandex.market.api.util.Exceptions.wrapRuntimeException;

/**
 * @author dimkarp93
 */
public class MatchServlet extends HttpServlet {
    private final HttpExpectations httpExpectations;

    public MatchServlet(HttpExpectations httpExpectations) {
        this.httpExpectations = httpExpectations;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        super.service(req, resp);

        Result<PredefinedHttpResponse, String> found = httpExpectations.tryResolve(convert(req));

        if (found.hasError()) {
            resp.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
            return;
        }
        HttpOptions options = found.getValue().getOptions();
        if (options.getError() != null) {
            if (options.getTimeout() > 0) {
                waitUntilTimestamp(start + options.getTimeout());
                switch (options.getError()) {
                    case CONNECT_TIMEOUT:
                        /*TODO Подумать как это можно обработать,
                         пока просто рву сеть
                         */
                        return;
                    case PROCESS_TIMEOUT:
                        process(found.getValue().getResponse(), resp);
                        return;
                }
                return;
            } else {
                process(found.getValue().getResponse(), resp);
                return;
            }
        }
        if (options.hasTimeout()) {
            waitUntilTimestamp(start + options.getTimeout());
            process(found.getValue().getResponse(), resp);
            return;
        }
        process(found.getValue().getResponse(), resp);
    }

    private HttpRequest convert(HttpServletRequest request) {
        return new HttpRequest(
            HttpMethod.valueOf(request.getMethod()),
            uri(request),
            headers(request),
            wrapRuntimeException(
                () -> IOUtils.toByteArray(
                    wrapRuntimeException(request::getInputStream)
                )
            )
        );
    }

    private void process(HttpResponse response, HttpServletResponse r) {
        r.setStatus(response.getHttpStatusCode());

        if (null != response.getHeaders()) {
            for (Map.Entry<String, List<String>> headers : response.getHeaders().entrySet()) {
                String headerName = headers.getKey();
                for (String headerValue : headers.getValue()) {
                    r.addHeader(headerName, headerValue);
                }
            }
        }

        if (null != response.getBody()) {
            wrapRuntimeException(() -> r.getOutputStream().write(response.getBody()));
        }


    }

    private URI uri(HttpServletRequest request) {
        return URI.create(request.getScheme() + "://" +
            request.getServerName() + ":" +
            request.getServerPort() +
            request.getRequestURI() + "?" +
            request.getQueryString());
    }

    private HttpHeaders headers(HttpServletRequest request) {
        Multimap<String, String> result = ArrayListMultimap.create();
        List<String> headers = Collections.list(request.getHeaderNames());
        for (String header: headers) {
            result.putAll(header, Collections.list(request.getHeaders(header)));
        }

        return new HttpHeaders(result);

    }

    private void waitUntilTimestamp(long timestamp) {
        long delay = Math.max(timestamp - System.currentTimeMillis(), 0L);
        while (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                //ignore
            }
            delay = Math.max(timestamp - System.currentTimeMillis(), 0L);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }
}
