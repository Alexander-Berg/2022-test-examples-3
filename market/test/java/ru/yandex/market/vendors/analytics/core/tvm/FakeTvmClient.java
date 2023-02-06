package ru.yandex.market.vendors.analytics.core.tvm;

import ru.yandex.market.vendors.analytics.core.utils.parser.NumberParserUtils;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.roles.Roles;

public class FakeTvmClient implements TvmClient {

    @Override
    public ClientStatus getStatus() {
        return new ClientStatus(ClientStatus.Code.OK, "OK");
    }

    @Override
    public String getServiceTicketFor(String alias) {
        return "test_service_ticket";
    }

    @Override
    public String getServiceTicketFor(int clientId) {
        return "test_service_ticket";
    }

    @Override
    public CheckedServiceTicket checkServiceTicket(String ticketBody) {
        var uid = NumberParserUtils.tryParseLong(ticketBody);
        if (uid.isPresent()) {
            return new CheckedServiceTicket(
                    TicketStatus.OK, null, uid.get().intValue(), 0L);
        } else {
            return new CheckedServiceTicket(TicketStatus.EXPIRED, null, 0, 0L);
        }
    }

    @Override
    public CheckedUserTicket checkUserTicket(String ticketBody) {
        var uid = NumberParserUtils.tryParseLong(ticketBody);
        if (uid.isPresent()) {
            return new CheckedUserTicket(
                    TicketStatus.OK, null, new String[0], uid.get(), new long[]{uid.get()});
        } else {
            return new CheckedUserTicket(TicketStatus.EXPIRED, null, new String[0], 0L, null);
        }
    }

    @Override
    public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv env) {
        return null;
    }

    @Override
    public Roles getRoles() {
        return null;
    }

    @Override
    public void close() { }
}
