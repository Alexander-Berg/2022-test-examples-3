package ru.yandex.market.api.listener.expectations;

import ru.yandex.market.api.listener.domain.HttpHeaders;
import ru.yandex.market.api.listener.domain.HttpMethod;

import java.util.*;

public class HttpExpectationCombiners {
    public static class AndCombiner {
        private static final String SCHEME_FAIL_REASON = "inconsistent scheme";
        private static final String INCONSISTENT_HTTP_METHOD = "inconsistent method configuration";
        private static final String INCONSISTENT_PORT = "inconsistent port";
        private static final String INCONSISTENT_HOST = "inconsistent host";
        private static final String INCONSISTENT_SERVER_METHOD = "inconsistent server method";

        private static void addParameter(Map<String, List<String>> result, String key, List<String> value) {
            List<String> strings = result.get(key);
            if (strings == null) {
                result.put(key, new ArrayList<>());
            }
            strings = result.get(key);
            strings.addAll(value);
        }

        private static HttpRequestDescription and(HttpRequestDescription a,
                                                  HttpRequestDescription b) {

            return new HttpRequestDescriptionBuilder()
                .setHttpMethod(and(a.getMethod(), b.getMethod(), a, b))
                .setScheme(andScheme(a.getScheme(), b.getScheme(), a, b))
                .setHost(andHost(a.getHost(), b.getHost(), a, b))
                .setPort(and(a.getPort(), b.getPort(), a, b))
                .setServerMethod(andServerMethod(a.getServerMethod(), b.getServerMethod(), a, b))
                .withBodyConditions(andBodyConditions(a.getBodyConditions(), b.getBodyConditions()))
                .withParameters(andParameters(a.getParameters(), b.getParameters()))
                .withoutParameters(andWithoutParameters(a.getWithoutParameters(), b.getWithoutParameters()))
                .withHeaders(andHeaders(a.getHeaders(), b.getHeaders()))
                .get();
        }

        private static Set<String> andWithoutParameters(Set<String> a, Set<String> b) {
            Set<String> result = new HashSet<>();
            result.addAll(a);
            result.addAll(b);
            return result;
        }

        private static Integer and(Integer a, Integer b, HttpRequestDescription da, HttpRequestDescription db) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            if (a == b) {
                return a;
            }
            if (!a.equals(b)) {
                fail(INCONSISTENT_PORT, da, db);
            }
            return a;
        }

        private static HttpMethod and(HttpMethod a, HttpMethod b, HttpRequestDescription da, HttpRequestDescription db) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            if (a != b) {
                fail(INCONSISTENT_HTTP_METHOD, da, db);
            }
            return a;
        }

        private static List<String> andBodyConditions(List<String> bodyConditions, List<String> bodyConditions1) {
            List<String> result = new ArrayList<>(bodyConditions.size() + bodyConditions1.size());
            result.addAll(bodyConditions);
            result.addAll(bodyConditions1);
            return result;
        }

        private static String andHost(String a, String b, HttpRequestDescription da, HttpRequestDescription db) {
            return andString(a, b, da, db, INCONSISTENT_HOST);
        }

        private static Map<String, List<String>> andParameters(Map<String, List<String>> a,
                                                               Map<String, List<String>> b) {
            return mergeParameterValues(a, b);
        }

        private static String andScheme(String a, String b, HttpRequestDescription da, HttpRequestDescription db) {
            return andString(a, b, da, db, SCHEME_FAIL_REASON);
        }

        private static String andServerMethod(String a, String b, HttpRequestDescription da, HttpRequestDescription db) {
            return andString(a, b, da, db, INCONSISTENT_SERVER_METHOD);
        }

        private static String andString(String a, String b, HttpRequestDescription da, HttpRequestDescription db, String reason) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            if (a.equals(b)) {
                return a;
            }
            if (!a.equalsIgnoreCase(b)) {
                fail(reason, da, db);
            }
            return a;
        }

        private static void fail(String message, HttpRequestDescription a, HttpRequestDescription b) {
            throw new IllegalStateException(String.format("%s %nDescription 1: %s%nExpectation 2:%s", message, a, b));
        }

        private static Map<String, List<String>> mergeParameterValues(Map<String, List<String>> a,
                                                                      Map<String, List<String>> b) {
            Map<String, List<String>> result = new HashMap<>();
            a.entrySet()
                .forEach(x -> addParameter(result, x.getKey(), x.getValue()));
            b.entrySet()
                .forEach(x -> addParameter(result, x.getKey(), x.getValue()));
            return result;
        }

        private static HttpHeaders andHeaders(HttpHeaders a, HttpHeaders b) {
            return HttpHeaders.combine(a, b);
        }
    }

    public static HttpRequestExpectation and(HttpRequestExpectation a, HttpRequestExpectation b) {
        return new HttpRequestExpectation(request -> a.getMatchFunction().match(request) && b.getMatchFunction().match(request),
            AndCombiner.and(a.getDescription(), b.getDescription()));
    }
}
