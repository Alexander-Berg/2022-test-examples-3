package ru.yandex.market.fulfillment.stockstorage.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.TvmClientStatusChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TvmClientStatusCheckerLogOnlyModeTest {

    private TvmClient tvmClient;
    private TvmClientStatusChecker checker;

    @BeforeEach
    public void init() {
        this.tvmClient = mock(TvmClient.class);
        TvmTicketChecker tvmTicketChecker = mock(TvmTicketChecker.class);
        when(tvmTicketChecker.isLogOnlyMode()).thenReturn(true);
        checker = new TvmClientStatusChecker(this.tvmClient, tvmTicketChecker);
    }

    @Test
    public void whenTvmClientStatusOkThenOk() {
        when(this.tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        CheckResult check = this.checker.check();
        assertEquals(CheckResult.Level.OK, check.getLevel());
    }

    @Test
    public void whenTvmClientStatusWarningThenOk() {
        when(this.tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.WARNING, ""));
        CheckResult check = this.checker.check();
        assertEquals(CheckResult.Level.OK, check.getLevel());
    }
}
