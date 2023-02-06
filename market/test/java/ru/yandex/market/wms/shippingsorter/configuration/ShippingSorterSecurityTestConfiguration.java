package ru.yandex.market.wms.shippingsorter.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.wms.common.spring.tvm.TvmTicketProviderStub;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@TestConfiguration
public class ShippingSorterSecurityTestConfiguration {

    @Bean(name = "coreTvmTicketProvider")
    @Profile({Profiles.TEST, Profiles.DEVELOPMENT})
    public TvmTicketProvider coreTvmTicketProvider() {
        return new TvmTicketProviderStub();
    }
}
