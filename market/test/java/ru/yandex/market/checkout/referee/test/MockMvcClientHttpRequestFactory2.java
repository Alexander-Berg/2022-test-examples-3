package ru.yandex.market.checkout.referee.test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Копия {@link org.springframework.test.web.client.MockMvcClientHttpRequestFactory}.
 * Изменён вызов request(httpMethod, uri) таким образом, что внутри не происходит двойной encoding
 * По умолчанию, url <code>?test=Text%20With%20Spaces</code> превращается в <code>?test=Text%2520With%2520Spaces</code>
 *
 * @author kukabara
 */
public class MockMvcClientHttpRequestFactory2 implements ClientHttpRequestFactory {

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final MockMvc mockMvc;


    public MockMvcClientHttpRequestFactory2(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }


    @Override
    public ClientHttpRequest createRequest(final URI uri, final HttpMethod httpMethod) throws IOException {
        return new MockClientHttpRequest(httpMethod, uri) {
            @Override
            public ClientHttpResponse executeInternal() throws IOException {
                try {
                    MockHttpServletRequestBuilder requestBuilder = request(httpMethod, uri);
                    requestBuilder.content(getBodyAsBytes());
                    requestBuilder.headers(getHeaders());

                    MvcResult mvcResult = MockMvcClientHttpRequestFactory2.this.mockMvc.perform(requestBuilder).andReturn();

                    MockHttpServletResponse servletResponse = mvcResult.getResponse();
                    HttpStatus status = HttpStatus.valueOf(servletResponse.getStatus());
                    byte[] body = servletResponse.getContentAsByteArray();
                    HttpHeaders headers = getResponseHeaders(servletResponse);

                    MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
                    clientResponse.getHeaders().putAll(headers);

                    return clientResponse;
                } catch (Exception ex) {
                    byte[] body = ex.toString().getBytes(UTF8_CHARSET);
                    return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    private HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : response.getHeaderNames()) {
            List<String> values = response.getHeaders(name);
            for (String value : values) {
                headers.add(name, value);
            }
        }
        return headers;
    }

}
