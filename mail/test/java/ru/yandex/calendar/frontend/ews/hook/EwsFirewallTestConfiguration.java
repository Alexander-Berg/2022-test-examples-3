package ru.yandex.calendar.frontend.ews.hook;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EwsFirewallTestConfiguration {
    @Bean
    public EwsFirewall ewsFirewall() {
        return Mockito.mock(EwsFirewall.class);
    }
}
