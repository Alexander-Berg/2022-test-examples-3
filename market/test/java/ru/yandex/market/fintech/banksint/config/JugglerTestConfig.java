package ru.yandex.market.fintech.banksint.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.fintech.banksint.service.juggler.JugglerService;

@Configuration
public class JugglerTestConfig {

    @Bean
    public JugglerService jugglerService() {
        var mockJuggler = Mockito.mock(JugglerService.class);
        Mockito.doNothing().when(mockJuggler).sendJugglerEvent(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(), Mockito.any());
        return mockJuggler;
    }
}
