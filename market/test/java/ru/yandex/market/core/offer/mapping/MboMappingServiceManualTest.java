package ru.yandex.market.core.offer.mapping;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.util.retry.InterruptedIOHttpRequestRetryHandler;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestRetryHandler;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Disabled
class MboMappingServiceManualTest {

    @Test
    void doTest() {
        final MboMappingsServiceStub service = new MboMappingsServiceStub();
        service.setHost("https://cm-testing.market.yandex-team.ru/proto/mboMappingsService/");
        service.setUserAgent("gaklimov");
        service.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(Module.MBO_MAPPINGS_SERVICE));
        service.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        service.setRetryHandler(new TraceHttpRequestRetryHandler(new InterruptedIOHttpRequestRetryHandler()));

        var response = service.searchMappingsByShopId(
                MboMappings.SearchMappingsBySupplierIdRequest.newBuilder()
                        .setSupplierId(11075352)
                        .setLimit(1)
                        .build());
        System.out.println(response);
    }
}
