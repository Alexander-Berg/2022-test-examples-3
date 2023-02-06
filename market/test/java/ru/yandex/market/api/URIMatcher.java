package ru.yandex.market.api;

import java.net.URI;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.http.NameValuePair;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.util.Urls;

/**
 * @author dimkarp93
 */
public class URIMatcher {
    public static Matcher<URI> uri(Matcher<URI> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<URI> hasQueryParams(String name, String ... values) {
        List<Matcher<? super String>> matchers = Lists.newArrayList();

        for (String value: values) {
            matchers.add(Matchers.containsString(name + '=' + value));
        }

        return ApiMatchers.map(
            URI::getQuery,
            "'query'",
            Matchers.allOf(matchers),
            URIMatcher::toStr
        );
    }

    public static Matcher<URI> hasSingleQueryParam(String name, String value) {
        String param = name + '=' + value;

        return ApiMatchers.map(
                URI::getQuery,
                "'query'",
                ApiMatchers.containsTimes(param, 1),
                URIMatcher::toStr
        );
    }

    public static Matcher<URI> hasNoQueryParams(String name, String ... values) {
        List<Matcher<? super String>> matchers = Lists.newArrayList();

        for (String value: values) {
            matchers.add(Matchers.not(Matchers.containsString(name + '=' + value)));
        }

        return ApiMatchers.map(
            URI::getQuery,
            "'query'",
            Matchers.anyOf(matchers),
            URIMatcher::toStr
        );
    }


    public static Matcher<URI> hasNoQueryParams(String name) {
        return ApiMatchers.map(
            URI::getQuery,
            "'query'",
            Matchers.not(Matchers.containsString(name + '=')),
            URIMatcher::toStr
        );
    }

    public static Matcher<URI> scheme(String scheme) {
        return ApiMatchers.map(
                URI::getScheme,
                "'host'",
                Matchers.is(scheme),
                URIMatcher::toStr
        );
    }

    public static Matcher<URI> host(String host) {
        return ApiMatchers.map(
            URI::getHost,
            "'host'",
            Matchers.is(host),
            URIMatcher::toStr
        );
    }

    public static Matcher<URI> port(int port) {
        return ApiMatchers.map(
            URI::getPort,
            "'port'",
            Matchers.is(port),
            URIMatcher::toStr
        );
    }

    public static Matcher<URI> path(String path) {
        return ApiMatchers.map(
            URI::getPath,
            "'path'",
            Matchers.is(path),
            URIMatcher::toStr
        );
    }

    public static String toStr(URI uri) {
        if (null == uri) {
            return "null";
        }
        return uri.toASCIIString();
    }
}
