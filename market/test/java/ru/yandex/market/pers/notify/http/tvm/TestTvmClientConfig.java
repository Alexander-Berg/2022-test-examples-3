package ru.yandex.market.pers.notify.http.tvm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.market.pers.notify.config.TvmClientConfig;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.passport.tvmauth.TvmClient;

// Пришлось задублировать конфигурацию в тесте, чтобы объявить бин Lazy
@Configuration
@Import({TvmClientConfig.class})
class TestTvmClientConfig {

    @Value("${pers.notify.tvm.id}")
    private int tvmClientId;

    @Value("${pers.notify.tvm.secret}")
    private String tvmClientSecret;

    @Value("${pers.notify.sberlogAppId}")
    private int sberlogAppId;

    @Value("${pers.notify.passportAppId}")
    private int passportAppId;

    @Lazy
    @Bean
    public TvmClient tvmClient() {
        final TvmApiSettings tvmApiSettings = new TvmApiSettings();
        tvmApiSettings.setSelfTvmId(tvmClientId);
        tvmApiSettings.enableServiceTicketChecking();
        tvmApiSettings.enableServiceTicketsFetchOptions(tvmClientSecret, new int[]{sberlogAppId, passportAppId});

        return new NativeTvmClient(tvmApiSettings);
    }
}
