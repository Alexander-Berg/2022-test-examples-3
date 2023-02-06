package ru.yandex.market.gutgin.tms.manual.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.gutgin.tms.config.interceptor.CompositeHttpRequestInterceptor;
import ru.yandex.market.gutgin.tms.config.interceptor.TraceStartHttpRequestInterceptor;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

@Configuration
public class ManualExternalProductionMappingServices {

    @Value("${user.agent}")
    String defaultUserAgent;
    int defaultConnectionTimeoutMillis = 300;
    @Bean
    MboMappingsServiceHelper mboMappingsServiceHelper(MboMappingsService mboMappingsService) {
        return new MboMappingsServiceHelper(mboMappingsService);
    }

    @Bean
    MboMappingsService mboMappingsService(
            @Value("${mboc.mappings.service.uri}") String mboMappingServiceUrl
    ) {
        MboMappingsServiceStub result = new MboMappingsServiceStub();
        result.setHost(mboMappingServiceUrl);
        initServiceClient(result, Module.MBOC_UI);
        result.setConnectionTimeoutMillis(defaultConnectionTimeoutMillis);
        result.setSocketTimeoutMillis(defaultConnectionTimeoutMillis);
        result.setTriesBeforeFail(1);
        return result;
    }

    private void initServiceClient(ServiceClient serviceClient, Module traceModule) {
        serviceClient.setUserAgent(defaultUserAgent);
        if (traceModule != null) {
            serviceClient.setHttpRequestInterceptor(CompositeHttpRequestInterceptor.of(
                    new TraceStartHttpRequestInterceptor(),
                    new TraceHttpRequestInterceptor(traceModule))
            );
            serviceClient.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        }
    }
}
