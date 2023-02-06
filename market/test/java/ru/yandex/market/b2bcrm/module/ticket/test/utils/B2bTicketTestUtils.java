package ru.yandex.market.b2bcrm.module.ticket.test.utils;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicketRoutingRule;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.TelephonyService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class B2bTicketTestUtils {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    private BcpService bcpService;

    public B2bTicket createB2bTicket(Map<String, Object> properties) {
        return ticketTestUtils.createTicket(B2bTicket.FQN, properties);
    }

    public TelephonyService createTelephonyService(String code, Team team) {
        Brand brand = ticketTestUtils.createBrand();
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        return (TelephonyService) ticketTestUtils.createService(
                team,
                serviceTime24x7,
                brand,
                Optional.of(code),
                TelephonyService.FQN,
                Map.of()
        );
    }

    public TelephonyService createTelephonyService(String code) {
        return createTelephonyService(code, ticketTestUtils.createTeam());
    }

    public B2bLeadTicket createB2bLead(Map<String, Object> propeties) {
        return createB2bLead(createTelephonyService("defaultTelephonyService"), propeties);
    }

    public B2bLeadTicket createB2bLead(TelephonyService service, Map<String, Object> propeties) {
        Team team = service.getResponsibleTeam() != null ? service.getResponsibleTeam() : ticketTestUtils.createTeam();
        return ticketTestUtils.createTicket(B2bLeadTicket.FQN, team, service, propeties);
    }

    public B2bTicketRoutingRule createRoutingRule(Service service, String replyTo) {
        return createRoutingRule(service, replyTo, null);
    }

    public B2bTicketRoutingRule createRoutingRule(Service service, String replyTo, String title) {
        return bcpService.create(B2bTicketRoutingRule.FQN, Maps.of(
                B2bTicketRoutingRule.SERVICE, service,
                B2bTicketRoutingRule.REPLY_TO, replyTo,
                B2bTicketRoutingRule.TITLE, title
        ));
    }

    public B2bTicketRoutingRule createRoutingRule(String title, Service service) {
        return createRoutingRule(service, null, title);
    }


}
