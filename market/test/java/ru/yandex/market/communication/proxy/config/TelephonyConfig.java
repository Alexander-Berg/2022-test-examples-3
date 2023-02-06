package ru.yandex.market.communication.proxy.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.inside.passport.tvm2.Tvm2;

@Configuration
public class TelephonyConfig {

    @Bean
    public Tvm2 tvm2() {
        return Mockito.mock(Tvm2.class);
    }

    @Bean
    public TelephonyClient telephonyClient() {
        return Mockito.mock(TelephonyClient.class);
    }
}
