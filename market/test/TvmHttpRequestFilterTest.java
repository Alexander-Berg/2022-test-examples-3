package ru.yandex.market.jmf.tvm.support.test;

import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.RequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpRequestChain;
import ru.yandex.market.jmf.tvm.support.TvmService;
import ru.yandex.market.jmf.tvm.support.impl.TvmHttpRequestFilter;

public class TvmHttpRequestFilterTest {

    /**
     * Проверяем установку заголовка с TVM-тикетом
     */
    @Test
    public void doFilter() {
        String applicationId = Randoms.string();
        String ticket = Randoms.string();

        TvmService tvmService = Mockito.mock(TvmService.class);
        Mockito.when(tvmService.getTicket(Mockito.eq(applicationId))).thenReturn(ticket);

        HttpRequestChain chain = Mockito.mock(HttpRequestChain.class);
        Mockito.when(chain.doFilter(Mockito.any(), Mockito.any())).thenReturn(new CompletableFuture<>());

        RequestBuilder builder = new RequestBuilder();
        Http request = Http.get();

        // вызов системы
        TvmHttpRequestFilter filter = new TvmHttpRequestFilter(tvmService, applicationId);
        filter.doFilter(builder, request, chain);

        // проверка утверждений
        String actualHeader = builder.build().getHeaders().get("X-Ya-Service-Ticket");
        Assertions.assertEquals(ticket, actualHeader);
    }
}
