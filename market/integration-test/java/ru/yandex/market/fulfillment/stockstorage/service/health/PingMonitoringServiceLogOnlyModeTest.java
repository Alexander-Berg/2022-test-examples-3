package ru.yandex.market.fulfillment.stockstorage.service.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.PingMonitoringService;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;


@TestPropertySource(properties = {
        "stockstorage.tvm.log-only-mode=true"
})
public class PingMonitoringServiceLogOnlyModeTest extends AbstractContextualTest {


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
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.ERROR, ""));
        String ping = service.ping();
        softly.assertThat(ping).as("Check status").isEqualTo("0;OK");

    }

}
