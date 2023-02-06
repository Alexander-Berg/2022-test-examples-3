package ru.yandex.market.wms.transportation.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings;
import ru.yandex.market.wms.common.spring.tvm.TvmTicketProviderStub;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLog;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;
import ru.yandex.market.wms.trace.request.RequestIdHolder;

import static ru.yandex.market.wms.trace.request.RequestUtils.REQUEST_ID_HEADER;


public class TransportationIntegrationTestConfig {

    @Bean(name = "applicationModuleName")
    public Module applicationModuleName() {
        return Module.TRANSPORTATION;
    }

    @Bean
    public RequestTraceLog requestTraceLog(Module applicationModuleName) {
        return new RequestTraceLogBase(applicationModuleName.toString() + "-trace");
    }

    @Bean
    public HttpClientSettings httpClientSettings() {
        return new HttpClientSettings();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public HttpClient httpClient(HttpClientSettings settings) {
        return HttpClient.create();
    }

    @Bean
    @Qualifier("request-id-filter")
    public ExchangeFilterFunction requestIdFilterFunction() {
        return ExchangeFilterFunction.ofRequestProcessor(
                request -> Mono.just(ClientRequest.from(request)
                        .headers(headers -> {
                            headers.set(REQUEST_ID_HEADER, RequestIdHolder.get().child().next());
                        })
                        .build())
        );
    }

    @Bean
    public TvmTicketProvider transportationTvmTicketProvider() {
        return new TvmTicketProviderStub();
    }
}
