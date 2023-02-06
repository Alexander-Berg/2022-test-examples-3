package ru.yandex.market.wms.servicebus.scenario.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class FulfillmentUrl {

    private final HttpMethod httpMethod;
    private final Collection<String> urlParts = new ArrayList<>();
    private final MultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();

    private FulfillmentUrl(
        Collection<String> urlParts,
        HttpMethod httpMethod,
        MultiValueMap<String, String> urlArguments
    ) {
        this.urlParts.addAll(urlParts);
        this.httpMethod = httpMethod;
        this.urlArguments.putAll(urlArguments);
    }

    public static FulfillmentUrl fulfillmentUrl(Collection<String> urlParts, HttpMethod httpMethod) {
        return fulfillmentUrl(urlParts, httpMethod, new LinkedMultiValueMap<>());
    }

    public static FulfillmentUrl fulfillmentUrl(Collection<String> urlParts,
                                                HttpMethod httpMethod,
                                                Map<String, List<String>> urlArguments) {
        return fulfillmentUrl(urlParts, httpMethod, new LinkedMultiValueMap<>(urlArguments));
    }

    public static FulfillmentUrl fulfillmentUrl(Collection<String> urlParts,
                                                HttpMethod httpMethod,
                                                MultiValueMap<String, String> urlArguments) {
        return new FulfillmentUrl(urlParts, httpMethod, urlArguments);
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Collection<String> getUrlParts() {
        return urlParts;
    }

    public MultiValueMap<String, String> getUrlArguments() {
        return urlArguments;
    }
}
