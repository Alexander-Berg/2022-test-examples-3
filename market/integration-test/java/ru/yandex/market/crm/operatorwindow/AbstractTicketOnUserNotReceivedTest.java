package ru.yandex.market.crm.operatorwindow;

import java.util.List;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketTicket;
import ru.yandex.market.crm.operatorwindow.utils.TestOrderUtils;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
public abstract class AbstractTicketOnUserNotReceivedTest extends AbstractModuleOwTest {

    @Inject
    protected DbService dbService;
    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    protected TicketTestUtils ticketTestUtils;
    @Inject
    protected TestOrderUtils testOrderUtils;
    @Inject
    protected OrderTestUtils orderTestUtils;
    @Inject
    protected TriggerServiceImpl triggerService;

    @BeforeEach
    public void setUpSuper() {
        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");
    }

    protected MarketTicket getSingleOpenedMarketTicket() {
        return ticketTestUtils.getSingleOpenedTicket(MarketTicket.FQN);
    }

    @NotNull
    protected List<MarketTicket> getActiveTickets() {
        return ticketTestUtils.getAllActiveTickets(MarketTicket.FQN);
    }
}
