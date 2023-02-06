package ru.yandex.market.checkout.checkouter.balance.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;

public class TrustGatewayTvmTest extends AbstractWebTestBase {

    @Autowired
    private TrustService trustService;

    @BeforeEach
    void setUp() {
        trustMockConfigurer.trustGatewayMock().resetAll();
    }

    @Test
    public void provideTvmTicketHeader() {
        try {
            trustService.getYandexCashbackBalance(359953025L, Color.BLUE);
        } catch (Exception e) {
            //NOT FOUND uid (здесь не важно)
        }
        trustMockConfigurer.trustGatewayMock().getAllServeEvents().forEach(e ->
                Assertions.assertEquals(
                        "service_ticket",
                        e.getRequest().getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER))
        );
    }
}
