package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.utils.MockSmsCampaignService;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

public class DeferTicketTest extends AbstractModuleOwTest {

    @Inject
    public OrderTestUtils orderTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private MockSmsCampaignService mockSmsCampaignService;
    @Inject
    private CommentTestUtils commentTestUtils;
    @Inject
    private MockSecurityDataService mockSecurityDataService;
    @Inject
    private MockCheckouterAPI mockCheckouterAPI;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private EmployeeTestUtils employeeTestUtils;
    @Inject
    private TriggerServiceImpl triggerService;
    @Inject
    private Geobase geobaseClient;


    @BeforeEach
    @Transactional
    public void setup() {
        mockSecurityDataService.reset();
        mockCheckouterAPI.clear();
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_PREORDER_CONFIRMATION);
        Mockito.when(geobaseClient.getTimeZoneByRegionId(213)).thenReturn("Asia/Yekaterinburg");
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(geobaseClient);
    }

    @Test
    @Transactional
    public void deferTicket__expectSmsWasSendAndCommentWasAddedAndOrderCommentWasAdded() {
        Order order = createOrder(Map.of(Order.PREORDER, true));
        final Long orderId = order.getOrderId();
        Assertions.assertEquals(1, countPreorderConfirmationTickets(order));
        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);

        mockSmsCampaignService.mockSendUnreachableSmsClientMessage(orderId);

        mockSecurityDataService.setCurrentEmployee(
                getTestEmployee(123L)
        );

        bcpService.edit(createdPreorderTicket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        Assertions.assertEquals(0, commentTestUtils.getComments(createdPreorderTicket).size());

        bcpService.edit(createdPreorderTicket, Map.of(
                Ticket.STATUS, Ticket.STATUS_DEFERRED,
                Ticket.DEFER_TIME, Duration.ofDays(1))
        );

        final Ticket updateTicket = getCreatedPreorderTicket(order);
        Assertions.assertEquals(1L, (long) updateTicket.getDeferCount());

        final List<Comment> ticketComments = commentTestUtils.getComments(createdPreorderTicket);
        Assertions.assertEquals(0, ticketComments.size());

        final List<Comment> orderComments = commentTestUtils.getComments(order);
        Assertions.assertEquals(1, orderComments.size());
        Assertions.assertTrue(orderComments.get(0).getBody().contains("Отложено"));

        mockSmsCampaignService.verifySendUnreachableSmsClientMessage(orderId);
    }

    private ru.yandex.market.jmf.module.ou.Employee getTestEmployee(long uid) {
        final Ou ou = ouTestUtils.createOu();
        return employeeTestUtils.createEmployee("employeeTitle", ou, uid);
    }

    private Order createOrder(Map<String, Object> attributesOverrides) {
        Map<String, Object> attributes = Maps.of(
                Order.STATUS, OrderStatus.PENDING.name(),
                Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION.name()
        );

        if (null != attributesOverrides) {
            attributes.putAll(attributesOverrides);
        }

        var order = orderTestUtils.createOrder(attributes);
        orderTestUtils.fireOrderImportedEvent(order);
        return order;
    }

    private long countPreorderConfirmationTickets(Order order) {
        return getPreorderConfigurationTickets(order).size();
    }

    private Optional<Ticket> getPreorderConfirmationTicket(Order order) {
        List<Entity> list = getPreorderConfigurationTickets(order);
        return list.isEmpty() ? Optional.empty() : Optional.of((TicketFirstLine) list.get(0));
    }

    private Ticket getCreatedPreorderTicket(Order order) {
        return getPreorderConfirmationTicket(order)
                .orElseThrow(() -> new RuntimeException("no ticket was created"));
    }

    private List<Entity> getPreorderConfigurationTickets(Order order) {
        Query query = Query.of(Fqn.parse("ticket$beruTelephony"))
                .withFilters(Filters.eq("order", order));
        return dbService.list(query);
    }
}
