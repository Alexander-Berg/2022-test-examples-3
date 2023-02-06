package ru.yandex.market.mbi.tariffs.client;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.request.trace.RequestTraceUtil;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class TariffTraceInterceptorTest {

    @Test
    void intercept() throws IOException {
        TariffTraceInterceptor tariffTraceInterceptor = new TariffTraceInterceptor();
        ClientHttpRequest request = new RestTemplate().getRequestFactory()
                .createRequest(URI.create("http://localhost/drafts"), HttpMethod.POST);
        ClientHttpRequestExecution execution = Mockito.mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = Mockito.mock(ClientHttpResponse.class);
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(response);

        tariffTraceInterceptor.intercept(request, new byte[]{}, execution);
        assertEquals(request.getHeaders().size(), 1);
        assertTrue(request.getHeaders().containsKey(RequestTraceUtil.REQUEST_ID_HEADER));
    }
}
