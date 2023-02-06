package ru.yandex.market.pers.test.http;

import java.net.URLDecoder;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.07.2019
 */
public class QueryRegexMatcher implements ArgumentMatcher<HttpUriRequest> {
    private final String queryRegex;

    public QueryRegexMatcher(String queryRegex) {
        this.queryRegex = queryRegex;
    }

    @Override
    public boolean matches(HttpUriRequest argument) {
        if (argument == null) {
            return false;
        }
        try {
            final String query = URLDecoder.decode(argument.getURI().getQuery(), "UTF-8");
            return Pattern.matches(queryRegex, query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
