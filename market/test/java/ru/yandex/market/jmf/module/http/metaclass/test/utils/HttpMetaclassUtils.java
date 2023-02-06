package ru.yandex.market.jmf.module.http.metaclass.test.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.jmf.module.http.support.ModuleHttpSupportConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
public class HttpMetaclassUtils {

    @Inject
    @Named(ModuleHttpSupportConfiguration.HTTP_SUPPORT_REST_TEMPLATE)
    RestTemplate restTemplate;

    @Nonnull
    public ClientHttpRequest prepareMocks(String url,
                                          HttpMethod method,
                                          ClientHttpResponse response) throws IOException {
        return prepareMocks(url, method, () -> response);
    }

    @Nonnull
    public ClientHttpRequest prepareMocks(String url,
                                          HttpMethod method,
                                          RuntimeException exception) throws IOException {
        return prepareMocks(url, method, () -> {
            throw exception;
        });
    }

    @Nonnull
    private ClientHttpRequest prepareMocks(String url,
                                           HttpMethod method,
                                           Supplier<ClientHttpResponse> responseSupplier) throws IOException {
        var request = mock(ClientHttpRequest.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(request.getBody()).thenReturn(outputStream);
        var headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(restTemplate.execute(
                eq(url),
                eq(method),
                any(),
                any()
        )).then(invocation -> {
            invocation.getArgument(2, RequestCallback.class).doWithRequest(request);
            ClientHttpResponse response = responseSupplier.get();
            return invocation.getArgument(3, ResponseExtractor.class).extractData(response);
        });
        return request;
    }
}
