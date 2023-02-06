package ru.yandex.market.partner.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

@ParametersAreNonnullByDefault
public class MbiArgumentMatchers {

    public static HttpUriRequest httpGets(String expectedUrl) throws URISyntaxException {
        return Mockito.argThat(new HttpGetMatcher(new URI(expectedUrl)));
    }

    public static HttpUriRequest httpGets(URI expectedUrl) {
        return Mockito.argThat(new HttpGetMatcher(expectedUrl));
    }

    private static class HttpGetMatcher implements ArgumentMatcher<HttpUriRequest> {
        private final URI expectedURI;

        public HttpGetMatcher(URI expectedURI) {
            this.expectedURI = expectedURI;
        }

        @Override
        public boolean matches(@Nullable HttpUriRequest argument) {
            return argument != null
                    && Objects.equals(argument.getURI(), expectedURI)
                    && argument.getMethod().equals("GET");
        }
    }
}
