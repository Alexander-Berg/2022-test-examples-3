package ru.yandex.market.common.test.matcher;

import java.util.Objects;

import org.apache.http.client.methods.HttpGet;
import org.mockito.ArgumentMatcher;

/**
 * Matcher для {@link HttpGet} параметров.
 *
 * @author fbokovikov
 */
public class HttpGetMatcher implements ArgumentMatcher<HttpGet> {
    private final String expectedUrl;
    private final String expectedMethod;

    public HttpGetMatcher(String expectedUrl, String expectedMethod) {
        this.expectedUrl = expectedUrl;
        this.expectedMethod = expectedMethod;
    }

    @Override
    public boolean matches(HttpGet actualGet) {
        return actualGet != null && Objects.equals(actualGet.getURI().toString(), expectedUrl)
                && Objects.equals(actualGet.getMethod(), expectedMethod);
    }

}
