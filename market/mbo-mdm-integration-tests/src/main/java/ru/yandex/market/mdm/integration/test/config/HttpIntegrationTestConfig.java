package ru.yandex.market.mdm.integration.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mdm.http.MasterDataService;
import ru.yandex.market.mdm.http.MasterDataServiceStub;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.mdm.http.SupplierDocumentServiceStub;

@Configuration
public class HttpIntegrationTestConfig {

    private static final int CONN_TIMEOUT_SEC = 30;

    @Value("${mdm.integration-test.root-uri}")
    private String intTestHandlesHost;

    @Value("${mdm.api.url}")
    private String mdmHost;

    @Value("${mdm.integration-test.supplierId}")
    private int supplierId;

    @Value("${mdm.integration-test.mskuId}")
    private long marketSkuId;

    @Bean
    public MasterDataService masterDataService() {
        var service = new MasterDataServiceStub();
        service.setUserAgent("mbo-mdm-integration-test");
        service.setHost(mdmHost + "/mdmMasterDataService/");
        service.setConnectionTimeoutSeconds(CONN_TIMEOUT_SEC);
        service.setTriesBeforeFail(1);
        return service;
    }

    @Bean
    public SupplierDocumentService supplierDocumentService() {
        SupplierDocumentServiceStub service = new SupplierDocumentServiceStub();
        service.setUserAgent("mbo-mdm-integration-test");
        service.setHost(mdmHost + "/supplierDocumentService/");
        service.setConnectionTimeoutSeconds(CONN_TIMEOUT_SEC);
        service.setTriesBeforeFail(1);
        return service;
    }

    @Bean
    public CommonTestParameters commonTestParameters() {
        return new CommonTestParameters(intTestHandlesHost, mdmHost, supplierId, marketSkuId);
    }

    public static class CommonTestParameters {
        private final String intTestHandlesHost;
        private final String mdmHost;
        private final int supplierId;
        private final long marketSkuId;

        CommonTestParameters(String intTestHandlesHost, String mdmHost,
                             int supplierId, long marketSkuId) {
            this.intTestHandlesHost = intTestHandlesHost;
            this.supplierId = supplierId;
            this.marketSkuId = marketSkuId;
            this.mdmHost = mdmHost;
        }

        public String getIntTestHandlesHost() {
            return intTestHandlesHost;
        }

        public int getSupplierId() {
            return supplierId;
        }

        public long getMarketSkuId() {
            return marketSkuId;
        }

        public String getMdmHost() {
            return mdmHost;
        }
    }
}
