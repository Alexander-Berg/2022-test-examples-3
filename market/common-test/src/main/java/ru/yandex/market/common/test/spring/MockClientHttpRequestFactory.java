package ru.yandex.market.common.test.spring;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

    private Map<String, String> responses = new HashMap<>();
    private PathMatcher pathMatcher = new AntPathMatcher();

    public MockClientHttpRequestFactory(Resource resource) throws IOException {
        new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS)
                .readTree(resource.getInputStream()).fields()
                .forEachRemaining(e -> responses.put(e.getKey(), e.getValue().toString()));
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {

        MockClientHttpRequest request = new MockClientHttpRequest(
                httpMethod,
                uri
        );

        MockClientHttpResponse response = new MockClientHttpResponse(
                getResponseBody(uri).getBytes(StandardCharsets.UTF_8),
                HttpStatus.OK
        );
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        request.setResponse(response);
        return request;
    }

    private String getResponseBody(URI uri) {
        return responses.entrySet().stream()
                .filter(e -> uriMatches(e.getKey(), uri))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(uri.toString()));
    }

    private boolean uriMatches(String target, URI uri) {
        UriComponents targetUri = UriComponentsBuilder.fromUriString(target).build();
        UriComponents input = UriComponentsBuilder.fromUri(uri).build();
        return pathMatcher.match(targetUri.getPath(), input.getPath())
                && paramsMatches(targetUri.getQueryParams(), input.getQueryParams());
    }

    private boolean paramsMatches(MultiValueMap<String, String> target, MultiValueMap<String, String> params) {
        for (Map.Entry<String, List<String>> entry : target.entrySet()) {
            if (!params.containsKey(entry.getKey()) || !params.get(entry.getKey()).containsAll(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

}
