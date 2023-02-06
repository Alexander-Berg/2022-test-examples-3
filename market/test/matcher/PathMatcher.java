package ru.yandex.market.jmf.http.test.matcher;

import org.mockito.ArgumentMatcher;

import ru.yandex.market.jmf.http.Http;

public class PathMatcher implements ArgumentMatcher<Http> {
    private final String expectedPath;

    public PathMatcher(String expectedPath) {
        this.expectedPath = expectedPath;
    }

    @Override
    public boolean matches(Http argument) {
        if (null == argument) {
            return false;
        }
        return argument.getPath().equals(expectedPath);
    }
}
