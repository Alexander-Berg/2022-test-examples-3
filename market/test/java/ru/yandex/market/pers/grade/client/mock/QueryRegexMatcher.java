package ru.yandex.market.pers.grade.client.mock;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.07.2020
 */
public class QueryRegexMatcher implements ArgumentMatcher<HttpUriRequest> {
    private final String pathRegexp;
    private final String queryRegexp;

    public QueryRegexMatcher(String pathRegexp, String queryRegexp) {
        this.pathRegexp = pathRegexp;
        this.queryRegexp = queryRegexp;
    }

    @Override
    public boolean matches(HttpUriRequest argument) {
        if (argument == null) {
            return false;
        }
        try {
            if (pathRegexp != null && !Pattern.matches(pathRegexp, decode(argument.getURI().getPath()))) {
                return false;
            }
            if (queryRegexp != null && !Pattern.matches(queryRegexp, decode(argument.getURI().getQuery()))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decode(String data){
        return URLDecoder.decode(data, StandardCharsets.UTF_8);
    }

}
