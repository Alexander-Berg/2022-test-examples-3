package ru.yandex.market.checkout.checkouter.balance.service;


import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.balance.xmlrpc.Balance2XmlRPCServiceFactory;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

public class BalanceTvmTest extends AbstractServicesTestBase {

    @Autowired
    private ExternalBalance2Service externalBalanceService;

    @Autowired
    private Balance2XmlRPCServiceFactory balance2XmlRPCServiceFactory;

    @BeforeEach
    public void init() {
        trustMockConfigurer.balanceHelper().mockWholeBalance();
        balance2XmlRPCServiceFactory.setTvmTicketProvider(() -> Optional.of("balance_tvm_ticket"));
    }

    @Test
    public void provideTvmTicketHeader() {
        externalBalanceService.createClient(BuyerProvider.getBuyer());
        trustMockConfigurer.balanceMock().getAllServeEvents().forEach(e ->
                Assertions.assertNotNull(e.getRequest().getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER))
        );
    }

}
