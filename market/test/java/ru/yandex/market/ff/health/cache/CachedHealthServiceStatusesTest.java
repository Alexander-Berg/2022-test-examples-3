package ru.yandex.market.ff.health.cache;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.TvmIntegrationTest;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


public class CachedHealthServiceStatusesTest extends TvmIntegrationTest {

    @Autowired
    private CachedHealthServiceStatuses service;

    @Autowired
    private TvmClient tvmClient;

    @Autowired
    private TvmTicketChecker tvmTicketChecker;

    @BeforeEach
    public void init() {
        when(tvmTicketChecker.isLogOnlyMode()).thenReturn(false);
        service.invalidateCache();
    }


    @Test
    public void getHealthStatusesWhenTvmClientStatusOk() {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        assertTrue(service.getHealthStatuses()
                .entrySet()
                .stream()
                .allMatch(Map.Entry::getValue));
    }

    @Test
    public void getHealthStatusesWhenTvmClientStatusExpiringCache() {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.WARNING, ""));
        assertTrue(service.getHealthStatuses()
                .entrySet()
                .stream()
                .allMatch(Map.Entry::getValue));
    }

    @Test
    public void getHealthStatusesWhenTvmClientStatusInvalidCache() {

        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.ERROR, ""));
        assertFalse(service.getHealthStatuses()
                .entrySet()
                .stream()
                .allMatch(Map.Entry::getValue));
    }


}
