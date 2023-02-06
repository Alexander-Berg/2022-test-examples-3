package ru.yandex.market.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.api.server.RequestAttribute;
import ru.yandex.market.api.server.domain.LimitType;
import ru.yandex.market.api.server.domain.Resource;
import ru.yandex.market.api.server.sec.client.CoreClient;
import ru.yandex.market.api.util.CommonCollections;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MockRequestBuilder {

    public static MockRequestBuilder start() {
        return new MockRequestBuilder();
    }

    private Multimap<String, String> params = ArrayListMultimap.create();
    private Map<String, String> firstParam = Maps.newHashMap();

    private Map<String, String> headers = Maps.newHashMap();
    private List<Cookie> cookies = Lists.newArrayList();

    private String host;
    private String remoteAddr;

    private String pathInfo;

    private String clientId;

    private Map<String, Object> attributes = Maps.newHashMap();

    public MockRequestBuilder param(String param, String value) {
        params.put(param, value);
        firstParam.putIfAbsent(param, value);
        return this;
    }

    public MockRequestBuilder param(String param, int value) {
        String val = String.valueOf(value);
        params.put(param, val);
        firstParam.putIfAbsent(param, val);
        return this;
    }

    public MockRequestBuilder param(String param, @NotNull Object value) {
        String val = String.valueOf(value);
        params.put(param, val);
        firstParam.put(param, val);
        return this;
    }

    public MockRequestBuilder header(String headerName, String headerValue) {
        if (null != headerValue) {
            headers.put(headerName, String.valueOf(headerValue));
        }
        return this;
    }

    public MockRequestBuilder remoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
        return this;
    }

    public MockRequestBuilder host(String host) {
        this.host = host;
        return this;
    }

    public MockRequestBuilder clientId(String clientId){
        this.clientId = clientId;
        return this;
    }

    public MockRequestBuilder pathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
        return this;
    }

    public MockRequestBuilder cookie(Cookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public <T> MockRequestBuilder attribute(RequestAttribute<T> attribute, T value) {
        attributes.put(attribute.getAttributeName(), value);
        return this;
    }

    public MockRequestBuilder methodName(String value) {
        return attribute(RequestAttribute.RESOURCE, new Resource() {
            @Override
            public String getName() {
                return value;
            }

            @Override
            public LimitType getLimitType() {
                return LimitType.METHOD;
            }
        });
    }

    public HttpServletRequest build() {
        HttpServletRequest mock = mock(HttpServletRequest.class);
        when(mock.getServerName()).thenReturn(host);

        for (Map.Entry<String, String> entry : firstParam.entrySet()) {
            when(mock.getParameter(eq(entry.getKey())))
                .thenReturn(entry.getValue());
        }
        for (Map.Entry<String, Collection<String>> entry : params.asMap().entrySet()) {
            when(mock.getParameterValues(eq(entry.getKey())))
                .thenReturn(entry.getValue().toArray(new String[0]));
        }

        when(mock.getParameterMap())
            .thenReturn(fromMultimap(params));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            when(mock.getHeader(eq(entry.getKey())))
                .thenReturn(entry.getValue());

            when(mock.getHeaders(eq(entry.getKey())))
                .thenAnswer(i -> Collections.enumeration(Collections.singleton(entry.getValue())));
        }
        if (!Strings.isNullOrEmpty(pathInfo)) {
            when(mock.getPathInfo())
                .thenReturn(pathInfo);
        }
        if (CommonCollections.notEmpty(cookies)) {
            when(mock.getCookies())
                .thenReturn(cookies.toArray(new Cookie[0]));
        }
        when(mock.getRemoteUser())
            .thenReturn(remoteAddr);

        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            when(mock.getAttribute(eq(entry.getKey())))
                    .thenReturn(entry.getValue());
        }
        if(clientId!= null){
            CoreClient cl = new CoreClient() {
                @Override
                public String getId() {
                    return clientId;
                }

                @Override
                public String getSecret() {
                    return null;
                }

                @Override
                public Status getStatus() {
                    return null;
                }
            };
            when(mock.getAttribute(eq("ru.yandex.market.api.market.client"))).thenReturn(cl);
        }
        return mock;
    }

    private static Map<String, String[]> fromMultimap(Multimap<String, String> multi) {
        Map<String, String[]> result = Maps.newHashMap();

        for (Map.Entry<String, Collection<String>> e: multi.asMap().entrySet()) {
            result.put(e.getKey(), e.getValue().toArray(new String[0]));
        }

        return result;
    }
}
