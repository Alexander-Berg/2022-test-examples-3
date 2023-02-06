package ru.yandex.market.checkout.referee.test;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.impl.DefaultMemCachingService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.referee.external.dealer.DealerApiClient;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.shopinfo.SupplierInfoService;

/**
 * @author komarovns
 * @date 18.10.18
 */
@Configuration
public class MockExternalServicesConfig {
    @Autowired
    private MockFactory mockFactory;

    @Bean
    public MdsS3Client mdsS3Client() throws IOException {
        return mockFactory.getMdsS3Client();
    }

    @Bean
    public MemCachedAgent memCachedAgent() {
        return mockFactory.getMemCachedAgentMock();
    }

    @Bean
    public DefaultMemCachingService memCachingService() {
        return new DefaultMemCachingService();
    }

    @Bean({"checkouterClient", "refereeCheckouterClient"})
    public CheckouterAPI checkouterClient() {
        return MockFactory.getCheckouterClientMock();
    }

    @Bean
    public MbiApiClient mbiApiClient() {
        return MockFactory.getMbiApiClientMock();
    }

    @Bean
    public DealerApiClient dealerApiClient() {
        return MockFactory.getDealerApiClientMock();
    }

    @Bean
    public SupplierInfoService supplierInfoService() {
        return mockFactory.getSupplierInfoService();
    }

    @Bean
    public MockMvc mockMvc() {
        return mockFactory.getMockMvc();
    }
}
