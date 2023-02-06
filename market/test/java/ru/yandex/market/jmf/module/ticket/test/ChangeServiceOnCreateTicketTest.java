package ru.yandex.market.jmf.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class ChangeServiceOnCreateTicketTest {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    private Team team;
    private Entity brand;
    private Service newService;

    @BeforeEach
    public void setUp() {
        team = ticketTestUtils.createTeam();
        brand = ticketTestUtils.createBrand();

        newService = createService(serviceTimeTestUtils.createServiceTime24x7());
    }

    @Test
    public void testNeedChangeIsOn() {
        Service service = createService(
                serviceTimeTestUtils.createServiceTime24x7(),
                true,
                newService,
                "ALWAYS_CHANGE"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(newService, ticket.getService());
    }

    @Test
    public void testNeedChangeIsOff() {
        Service service = createService(
                serviceTimeTestUtils.createServiceTime24x7(),
                false,
                newService,
                "ALWAYS_CHANGE"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(service, ticket.getService());
    }

    @Test
    public void testNewServiceIsNull() {
        Service service = createService(
                serviceTimeTestUtils.createServiceTime24x7(),
                true,
                null,
                "ALWAYS_CHANGE"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(service, ticket.getService());
    }

    @Test
    public void testConditionIsNull() {
        Service service = createService(
                serviceTimeTestUtils.createServiceTime24x7(),
                true,
                newService,
                null
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(service, ticket.getService());
    }

    @Test
    public void testServiceHasChangedWhenConditionOnlyNonWorkingHours() {
        Service service = createService(
                serviceTimeTestUtils.createNonWorkingNowServiceTime(),
                true,
                newService,
                "ONLY_NON_WORKING_HOURS"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(newService, ticket.getService());
        Assertions.assertEquals(newService.getServiceTime(), ticket.getServiceTime());
    }

    @Test
    public void testServiceHasNotChangedWhenConditionOnlyNonWorkingHours() {
        Service service = createService(
                serviceTimeTestUtils.createServiceTime24x7(),
                true,
                newService,
                "ONLY_NON_WORKING_HOURS"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(service, ticket.getService());
    }

    @Test
    public void testServiceHasChangedWhenNonWorkingHoursAndAlwaysChangeCondition() {
        Service service = createService(
                serviceTimeTestUtils.createNonWorkingNowServiceTime(),
                true,
                newService,
                "ALWAYS_CHANGE"
        );

        Ticket ticket = createTicket(service);

        Assertions.assertEquals(newService, ticket.getService());
    }

    private Service createService(ServiceTime serviceTime,
                                  boolean needChangeService,
                                  Service newService,
                                  String condition) {
        return ticketTestUtils.createService(
                Maps.of(
                        Service.SERVICE_TIME, serviceTime,
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand,
                        Service.NEED_CHANGE_SERVICE_ON_CREATE_TICKET, needChangeService,
                        Service.SERVICE_ON_CREATE_TICKET, newService,
                        Service.CONDITION_FOR_CHANGE_SERVICE_ON_CREATE_TICKET, condition
                ));
    }

    private Service createService(ServiceTime serviceTime) {
        return ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTime,
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand
                ));
    }

    private Ticket createTicket(Service service) {
        return ticketTestUtils.createTicket(Fqn.of("ticket$test"), team, service, Map.of());
    }
}
