package ru.yandex.market.tpl.integration.tests.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@RequiredArgsConstructor
public class TvmConfiguration {
    public static final String X_YA_SERVICE_TICKET = "X-Ya-Service-Ticket";

    @Bean
    public TvmClient tvmClientTpl(
            @Value("${tvm.clientId}") Integer clientId,
            @Value("${tvm.tplTestingId}") Integer tplTestingId,
            @Value("${tvm.tplSecret}") String tplSecret
    ) {
        var settings = TvmApiSettings.create().setSelfTvmId(clientId);
        settings.enableServiceTicketsFetchOptions(tplSecret, new int[] {tplTestingId});
        return new NativeTvmClient(settings.enableServiceTicketChecking());
    }

    @Bean
    public TvmClient tvmClientSc(
            @Value("${tvm.scClientId}") Integer clientId,
            @Value("${tvm.scTestingId}") Integer scTestingId,
            @Value("${tvm.scSecret}") String scSecret
    ) {
        var settings = TvmApiSettings.create().setSelfTvmId(clientId);
        settings.enableServiceTicketsFetchOptions(scSecret, new int[] {scTestingId});
        return new NativeTvmClient(settings.enableServiceTicketChecking());
    }

    @Bean
    public TvmTicketProvider tplTvmTicketProvider(@Qualifier("tvmClientTpl") TvmClient tvmClient,
                                                  @Value("${tvm.tplTestingId}") Integer tplTestingId) {
        return () -> tvmClient.getServiceTicketFor(tplTestingId);
    }

    @Bean
    public TvmTicketProvider scTvmTicketProvider(@Qualifier("tvmClientSc")TvmClient tvmClient,
                                                 @Value("${tvm.scTestingId}") Integer testingId) {
        return () -> tvmClient.getServiceTicketFor(testingId);
    }

    public interface TvmTicketProvider {
        String provideServiceTicket();
    }

}
