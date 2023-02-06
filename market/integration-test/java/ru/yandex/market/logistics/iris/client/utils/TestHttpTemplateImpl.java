package ru.yandex.market.logistics.iris.client.utils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.util.client.HttpTemplate;

import static java.util.Objects.requireNonNull;

public class TestHttpTemplateImpl extends HttpTemplate {

    private final RestTemplate restTemplate;

    public TestHttpTemplateImpl(String baseUri, RestTemplate restTemplate) {
        super(baseUri);
        this.restTemplate = requireNonNull(restTemplate);
    }

    @Nonnull
    @Override
    public <Request, Response> Response executePost(@Nonnull Request request, @Nonnull Class<Response> responseClass,
                                                    @Nonnull String... uriFragments) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(baseUri);
        uriComponentsBuilder.pathSegment(uriFragments);
        return Optional.ofNullable(
            restTemplate.postForObject(
                uriComponentsBuilder.build().toUriString(), request, responseClass))
            .orElseThrow(() -> new RuntimeException("Null returned"));
    }

    @Override
    public <Request> void executePost(@Nonnull Request request, @Nonnull String... uriFragments) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(baseUri);
        uriComponentsBuilder.pathSegment(uriFragments);
        restTemplate.postForLocation(uriComponentsBuilder.build().toUriString(), request);
    }

    @Nonnull
    @Override
    public <Response> Response executeGet(@Nonnull Class<Response> responseClass,
                                          @Nonnull Map<String, Set<String>> paramMap, @Nonnull String... uriFragments) {
        throw new UnsupportedOperationException();
    }
}
