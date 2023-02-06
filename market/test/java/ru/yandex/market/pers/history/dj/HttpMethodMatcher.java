package ru.yandex.market.pers.history.dj;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpMethod;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.07.2019
 */
public class HttpMethodMatcher implements ArgumentMatcher<HttpUriRequest> {
    private final HttpMethod httpMethod;

    public HttpMethodMatcher(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean matches(HttpUriRequest argument) {
        if (argument == null) {
            return false;
        }
        try {
            final String method = argument.getMethod();
            return httpMethod.matches(method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
