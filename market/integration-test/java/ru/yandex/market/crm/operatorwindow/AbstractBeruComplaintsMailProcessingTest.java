package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.crm.operatorwindow.domain.Email;
import ru.yandex.market.crm.operatorwindow.utils.VipTestUtil;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;


public class AbstractBeruComplaintsMailProcessingTest extends AbstractMailProcessingTest {

    protected static final Long DEFAULT_ORDER_ID = 11989056L;
    protected static final String BERU_COMPLAINTS_MAIL_CONNECTION = "beruComplaints";
    protected static final String MARKET_CREDIT_MAIL_CONNECTION = "marketCredit";
    protected static final String MARKET_COMPLAINTS_MAIL_CONNECTION = "marketComplaint";
    protected static final String FIRST_LINE_SENDER_EMAIL = "devnull@yandex-team.ru";
    protected static final String WAREHOUSE_SENDER_EMAIL = "promises@beru.ru";
    protected static final String BERU_COMPLAINTS_EMAIL = "promises@beru.ru";
    protected static final String VIP_EMAIL = Randoms.email();

    @Inject
    protected VipTestUtil vipTestUtil;

    @BeforeEach
    public void prepareData() {
        orderTestUtils.clearCheckouterAPI();

        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "08_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "08:00", "21:00");

        mailTestUtils.createMailConnection(BERU_COMPLAINTS_MAIL_CONNECTION);
        orderTestUtils.createOrder(Map.of(Order.NUMBER, DEFAULT_ORDER_ID));

        vipTestUtil.registerEmailAsVip(new Email(VIP_EMAIL));
    }

    protected BeruComplaintsTicket getSingleOpenedBeruComplaintsTicket() {
        List<BeruComplaintsTicket> tickets = dbService.list(Query.of(BeruComplaintsTicket.FQN).withFilters(
                Filters.ne(Ticket.STATUS, Ticket.STATUS_CLOSED)
        ));
        Assertions.assertEquals(1, tickets.size());
        return tickets.get(0);
    }
}
