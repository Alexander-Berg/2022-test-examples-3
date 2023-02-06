package ru.yandex.market.mboc.integration.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboCategoryServiceStub;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.mdm.http.SupplierDocumentServiceStub;

@Configuration
public class HttpIntegrationTestConfig {

    private static final int CONN_TIMEOUT_SEC = 30;

    @Value("${mboc.integration-test.root-uri}")
    private String intTestHandlesHost;

    @Value("${mboc.api.url}")
    private String host;

    @Value("${mboc.integration-test.supplierId}")
    private int supplierId;

    @Value("${mboc.integration-test.skuId}")
    private long marketSkuId;

    @Bean
    public SupplierDocumentService supplierDocumentService() {
        SupplierDocumentServiceStub service = new SupplierDocumentServiceStub();
        service.setUserAgent("mboc-integration-test");
        service.setHost(host + "/mdm/supplierDocumentService/");
        service.setConnectionTimeoutSeconds(CONN_TIMEOUT_SEC);
        service.setTriesBeforeFail(1);
        return service;
    }

    @Bean
    public MboMappingsService mboMappingsService() {
        MboMappingsServiceStub service = new MboMappingsServiceStub();
        service.setUserAgent("mboc-integration-test");
        service.setHost(host + "/mboMappingsService/");
        service.setConnectionTimeoutSeconds(CONN_TIMEOUT_SEC);
        service.setTriesBeforeFail(1);
        return service;
    }

    @Bean
    public MboCategoryService mboCategoryService() {
        MboCategoryServiceStub service = new MboCategoryServiceStub();
        service.setConnectionTimeoutSeconds(CONN_TIMEOUT_SEC);
        service.setHost(host + "/mboCategoryService/");
        service.setUserAgent("mboc-integration-test");
        service.setTriesBeforeFail(1);
        return service;
    }

    @Bean
    public CommonTestParameters commonTestParameters() {
        return new CommonTestParameters(intTestHandlesHost, host, supplierId, marketSkuId);
    }

    public static class CommonTestParameters {
        private final String intTestHandlesHost;
        private final String host;
        private final int supplierId;
        private final long marketSkuId;

        CommonTestParameters(String intTestHandlesHost, String host, int supplierId, long marketSkuId) {
            this.intTestHandlesHost = intTestHandlesHost;
            this.host = host;
            this.supplierId = supplierId;
            this.marketSkuId = marketSkuId;
        }

        public String getIntTestHandlesHost() {
            return intTestHandlesHost;
        }

        public String getHost() {
            return host;
        }

        public int getSupplierId() {
            return supplierId;
        }

        public long getMarketSkuId() {
            return marketSkuId;
        }
    }
}
