package ru.yandex.market.fulfillment.stockstorage.service.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.PingMonitoringService;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;

public class PingMonitoringServiceTest extends AbstractContextualTest {

    @Autowired
    private PingMonitoringService service;

    @Autowired
    private TvmClient tvmClient;

    @Test
    void pingWhenTvmClientStatusIsOkTest() {

        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        String ping = service.ping();
        softly.assertThat(ping).as("Check status").isEqualTo("0;OK");

    }

    @Test
    void pingWhenTvmClientStatusIsErrorTest() {

        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.ERROR, "InvalidCache"));
        String ping = service.ping();
        softly.assertThat(ping).as("Check status")
                .isEqualTo("2;Tvm client has invalid status: InvalidCache");

    }


}
