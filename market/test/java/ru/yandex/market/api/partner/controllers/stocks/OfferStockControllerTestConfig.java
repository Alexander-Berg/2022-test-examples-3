package ru.yandex.market.api.partner.controllers.stocks;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.api.partner.controllers.stocks.checkers.PartnerOfferStockUpdateEnabledChecker;
import ru.yandex.market.logbroker.LogbrokerService;

import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
@Configuration
public class OfferStockControllerTestConfig {

    @Bean
    @Qualifier("marketQuickStocksLogbrokerService")
    public LogbrokerService marketQuickStocksLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public PartnerOfferStockUpdateEnabledChecker offerStockUpdateEnabledChecker() {
        return mock(PartnerOfferStockUpdateEnabledChecker.class);
    }
}
