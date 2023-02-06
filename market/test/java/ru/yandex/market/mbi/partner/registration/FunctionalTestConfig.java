package ru.yandex.market.mbi.partner.registration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.api.MbibpmnApi;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.partner.registration.balance.BalanceService;
import ru.yandex.market.mbi.partner.registration.model.PartnerRegistrationInfo;
import ru.yandex.market.mbi.partner.registration.services.MbiBpmnRetrofitService;
import ru.yandex.market.mbi.partner.registration.util.IsRegistrationByAgencyAllowed;
import ru.yandex.mj.generated.client.integration_npd.api.ApplicationApiClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class FunctionalTestConfig {

    @Bean
    public MbiOpenApiClient mbiOpenApiClient() {
        return Mockito.mock(MbiOpenApiClient.class);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerRegistrationInfo> partnerRegistrationInfoPublisher() {
        return Mockito.mock(LogbrokerEventPublisher.class);
    }

    @Bean
    @Primary
    public ApplicationApiClient applicationApi() {
        return mock(ApplicationApiClient.class);
    }

    @Bean
    public MbibpmnApi mbibpmnApi() {
        return mock(MbibpmnApi.class);
    }

    @Bean
    public MbiBpmnRetrofitService mbiBpmnRetrofitService(MbibpmnApi mbibpmnApi) {
        MbiBpmnRetrofitService mbiBpmnRetrofitService = Mockito.mock(MbiBpmnRetrofitService.class);
        when(mbiBpmnRetrofitService.create(eq(MbibpmnApi.class))).thenReturn(mbibpmnApi);
        return mbiBpmnRetrofitService;
    }

    @Bean
    public BalanceService balanceService() {
        return mock(BalanceService.class);
    }

    @Bean
    public IsRegistrationByAgencyAllowed isRegistrationByAgencyAllowed() {
        return mock(IsRegistrationByAgencyAllowed.class);
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }
}
