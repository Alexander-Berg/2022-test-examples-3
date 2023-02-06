package ru.yandex.market.tpl.carrier.core.config;

import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.carrier.core.external.kiosk.KioskApiClient;
import ru.yandex.market.tpl.carrier.core.service.region.CarrierRegionService;
import ru.yandex.market.tpl.carrier.core.service.region.DummyCarrierRegionService;
import ru.yandex.market.tpl.common.dsm.client.api.DriverApi;
import ru.yandex.market.tpl.common.dsm.client.api.DriverManualApi;
import ru.yandex.market.tpl.common.dsm.client.api.EmployerApi;
import ru.yandex.market.tpl.common.web.tvm.DummyTvmClient;
import ru.yandex.market.tpl.mock.DriverApiEmulator;
import ru.yandex.market.tpl.mock.EmployerApiEmulator;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
public class MockIntegrationTestsConfig {
    @Configuration
    public static class TvmTestConfiguration {

        @Bean
        public TvmClient tvmClient() {
            return new DummyTvmClient();
        }

    }

    @Configuration
    public static class CarrierRegionServiceTestConfiguration {

        @Bean
        public CarrierRegionService carrierRegionService() {
            return new DummyCarrierRegionService();
        }
    }


    @MockBean(DriverManualApi.class)
    @Configuration
    public static class DeliveryStaffManagerTestConfiguration {

    }

    public static class DeliveryStaffManagerEmulator {

        @Bean
        public EmployerApi employerApi() {
            return new EmployerApiEmulator();
        }

        @Bean
        public DriverApi driverApi() {
            return new DriverApiEmulator();
        }
    }

    @Configuration
    public static class KioskTestConfiguration {
        @Bean(name = "testKioskApiClient")
        public KioskApiClient testKioskApiClient() {
            return Mockito.mock(KioskApiClient.class);
        }
    }

}
