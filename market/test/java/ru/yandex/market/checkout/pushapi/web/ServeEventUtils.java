package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class ServeEventUtils {
    private ServeEventUtils() {
        throw new UnsupportedOperationException();
    }

    public static String extractTokenParameter(ServeEvent event) {
        LoggedRequest request = event.getRequest();
        UriComponents components = UriComponentsBuilder.fromUriString(request.getUrl()).build();
        MultiValueMap<String, String> queryParams = components.getQueryParams();
        return queryParams.getFirst("auth-token");
    }
}
