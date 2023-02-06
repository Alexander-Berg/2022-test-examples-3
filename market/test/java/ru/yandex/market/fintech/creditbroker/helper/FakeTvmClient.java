package ru.yandex.market.fintech.creditbroker.helper;

import org.mockito.Mockito;

import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.roles.Roles;

public class FakeTvmClient implements TvmClient {
    public static final String VALID_TOKEN = "abc";

    @Override
    public ClientStatus getStatus() {
        return null;
    }

    @Override
    public String getServiceTicketFor(String alias) {
        return null;
    }

    @Override
    public String getServiceTicketFor(int clientId) {
        return null;
    }

    @Override
    public CheckedServiceTicket checkServiceTicket(String ticketBody) {
        CheckedServiceTicket serviceTicket = Mockito.mock(CheckedServiceTicket.class);
        if (ticketBody.equals(VALID_TOKEN)) {
            Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.OK);
            Mockito.when(serviceTicket.getSrc()).thenReturn(424242);
        } else {
            Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.MALFORMED);
            Mockito.when(serviceTicket.getSrc()).thenReturn(-1);
        }
        return serviceTicket;
    }

    @Override
    public CheckedUserTicket checkUserTicket(String ticketBody) {
        return null;
    }

    @Override
    public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv overridedBbEnv) {
        return null;
    }

    @Override
    public Roles getRoles() {
        return null;
    }

    @Override
    public void close() {

    }
}
