package ru.yandex.market.ocrm.module.checkouter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

import static org.mockito.Mockito.mock;

@Import({
        ModuleCheckouterConfiguration.class
})
public class ModuleCheckouterTestConfiguration {

    @Bean
    CheckouterAPI checkouterAPI() {
        return mock(CheckouterAPI.class);
    }

    @Bean
    CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi() {
        return mock(CheckouterOrderHistoryEventsApi.class);
    }

    @Bean
    TvmTicketProvider tvmTicketProvider() {
        return new TestTvmTicketProvider();
    }
}
