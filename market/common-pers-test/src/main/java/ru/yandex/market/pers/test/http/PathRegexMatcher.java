package ru.yandex.market.pers.test.http;

import java.net.URLDecoder;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.07.2019
 */
public class PathRegexMatcher implements ArgumentMatcher<HttpUriRequest> {
    private final String pathRegexp;

    public PathRegexMatcher(String queryRegex) {
        this.pathRegexp = queryRegex;
    }

    @Override
    public boolean matches(HttpUriRequest argument) {
        if (argument == null) {
            return false;
        }
        try {
            final String path = URLDecoder.decode(argument.getURI().getPath(), "UTF-8");
            return Pattern.matches(pathRegexp, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
