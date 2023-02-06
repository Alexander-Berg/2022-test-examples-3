package ru.yandex.market.logistics.cs.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Configuration
@Import(TvmMockConfig.class)
@ImportResource({"classpath:WEB-INF/checkouter-serialization.xml"})
public class CheckouterClientMockConfig {
    @Primary
    @Bean
    public CheckouterAPI checkouterClient(CheckouterOrderHistoryEventsApi orderHistoryEventsApi) {
        CheckouterAPI checkouterApi = Mockito.mock(CheckouterAPI.class);
        when(checkouterApi.orderHistoryEvents()).thenReturn(orderHistoryEventsApi);
        doNothing().when(checkouterApi).setTvmTicketProvider(any());
        return checkouterApi;
    }

    @Bean
    public CheckouterOrderHistoryEventsApi orderHistoryEventsApi() {
        return Mockito.mock(CheckouterOrderHistoryEventsApi.class);
    }
}
