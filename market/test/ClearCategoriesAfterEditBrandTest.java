package ru.yandex.market.jmf.module.ticket.test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;


@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class ClearCategoriesAfterEditBrandTest {

    @Inject
    protected BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    private Team team;
    private Entity brand1;
    private Entity brand2;
    private Entity brand3;
    private Entity brand4;
    private Service service1;
    private Service service2;
    private Service service3;
    private Service serviceWithNull;
    private TicketCategory categoryb1;
    private TicketCategory categoryb2;
    private TicketCategory categoryb3S2;
    private TicketCategory categoryb4s3;

    @BeforeEach
    public void setUp() {
        team = ticketTestUtils.createTeam();
        brand1 = ticketTestUtils.createBrand();
        brand2 = ticketTestUtils.createBrand();
        brand3 = ticketTestUtils.createBrand();
        brand4 = ticketTestUtils.createBrand();
        service1 = createService(serviceTimeTestUtils.createServiceTime24x7(), brand1);
        service2 = createService(serviceTimeTestUtils.createServiceTime24x7(), brand2);
        service3 = createService(serviceTimeTestUtils.createServiceTime24x7(), brand3);
        serviceWithNull = createService(serviceTimeTestUtils.createServiceTime24x7(), brand3);

        bcpService.edit(serviceWithNull, "usedToFilterCategories", null);

        categoryb1 = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand1.getGid(),
                "code", "test category1",
                "title", "test category1"));

        categoryb2 = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand2.getGid(),
                "code", "test category2",
                "title", "test category2"
        ));

        categoryb3S2 = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand3.getGid(),
                "services", Collections.singleton(service2.getGid()),
                "code", "test category3",
                "title", "test category3"
        ));

        categoryb4s3 = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand4.getGid(),
                "services", Collections.singleton(service3.getGid()),
                "code", "test category4",
                "title", "test category4"
        ));
    }

    @Test
    @Description("Должны почистится все категории в тикете")
    public void shouldClearCategories() {
        Set<TicketCategory> categories = new HashSet<>();
        categories.add(categoryb1);
        categories.add(categoryb4s3);

        Ticket ticket = ticketTestUtils.createTicket(Fqn.of("ticket$test"), team, service1,
                Map.of("categories", categories));

        bcpService.edit(ticket, "service", service2);
        Assertions.assertEquals(service2.getBrand(), ticket.getBrand());
        Assertions.assertTrue(ticket.getCategories().isEmpty());
    }


    @Test
    @Description("не должно падать NPE когда service.isUsedToFilterCategories = null")
    public void shouldNotNpe() {
        Set<TicketCategory> categories = new HashSet<>();
        categories.add(categoryb1);
        categories.add(categoryb4s3);

        Ticket ticket = ticketTestUtils.createTicket(Fqn.of("ticket$test"), team, service1,
                Map.of("categories", categories));

        bcpService.edit(ticket, "service", serviceWithNull);
        Assertions.assertEquals(serviceWithNull.getBrand(), ticket.getBrand());
        Assertions.assertTrue(ticket.getCategories().isEmpty());
    }

    @Test
    @Description("Должна остаться категория в тикете, так как она соответсвуюет бренду из тикета")
    public void shouldNotClearOneCategoryByService() {
        Set<TicketCategory> categories = new HashSet<>();
        categories.add(categoryb1);
        categories.add(categoryb2);
        categories.add(categoryb3S2);

        Ticket ticket = ticketTestUtils.createTicket(Fqn.of("ticket$test"), team, service1,
                Map.of("categories", categories));

        bcpService.edit(ticket, "service", service3);
        Assertions.assertEquals(service3.getBrand(), ticket.getBrand());
        Assertions.assertEquals(1, ticket.getCategories().size());
        Assertions.assertTrue(ticket.getCategories().contains(categoryb3S2));
    }

    private Service createService(ServiceTime serviceTime, Entity brand) {
        return ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTime,
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand
                ));
    }
}
