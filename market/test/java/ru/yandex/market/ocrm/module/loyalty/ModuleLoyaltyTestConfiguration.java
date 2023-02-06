package ru.yandex.market.ocrm.module.loyalty;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;

@Configuration
@Import({
        ModuleLoyaltyConfiguration.class,
        ModuleOrderTestConfiguration.class,
        ModuleOuTestConfiguration.class,
        ModuleTicketTestConfiguration.class
})
public class ModuleLoyaltyTestConfiguration {

    @Bean
    @Primary
    public MarketLoyaltyClient marketLoyaltyClient() {
        return Mockito.mock(MarketLoyaltyClient.class);
    }

//    @Bean
//    public TvmService tvmService() {
//        return new TvmServiceMockImpl();
//    }
}
