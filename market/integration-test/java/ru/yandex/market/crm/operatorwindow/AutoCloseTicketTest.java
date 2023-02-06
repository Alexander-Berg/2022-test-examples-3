package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.jmf.entity.FraudConfirmationTicket;
import ru.yandex.market.crm.operatorwindow.utils.MockSmsCampaignService;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.checkouter.OrderStatus;
import ru.yandex.market.ocrm.module.order.OrderAction;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_CROSSDOC_CANCELLATION;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_FRAUD_CONFIRMATION;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_ORDER_POSTPONE;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_OTHER_ORDER_TASKS;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_PREORDER_CONFIRMATION;

@Transactional
public class AutoCloseTicketTest extends AbstractModuleOwTest {

    private static final Fqn BERU_OUTGOING_CALL = Fqn.of("ticket$beruOutgoingCall");
    private static final Logger LOG = LoggerFactory.getLogger(AutoCloseTicketTest.class);
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private EntityService entityService;
    @Inject
    private MockSecurityDataService mockSecurityDataService;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private EmployeeTestUtils employeeTestUtils;
    @Inject
    private MockSmsCampaignService mockSmsCampaignService;
    @Inject
    private Geobase geobaseClient;

    /**
     * В тестах проверяется автоматика закрытия обращений при действиях с заказом.
     * Закрытие происходит в триггерах fqn="order" event="imported", по большей части, триггер autoCloseTicket
     */
    private static Stream<Arguments> parameters() {
        return Stream.of(
                // preorder
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_PREORDER_CONFIRMATION,       // serviceCode
                        OrderStatus.CANCELLED,            // orderStatus
                        List.of(),                        // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CANCELLED)
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_PREORDER_CONFIRMATION,        // serviceCode
                        OrderStatus.PROCESSING,            // orderStatus
                        List.of(Ticket.STATUS_PROCESSING), // ticketStatuses
                        doNothing()                        // action
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_PREORDER_CONFIRMATION,        // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_DEFERRED), // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CANCELLED)   // orderAction
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_PREORDER_CONFIRMATION,        // serviceCode
                        OrderStatus.PICKUP,                // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_REOPENED), // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED)   // orderAction
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_PREORDER_CONFIRMATION,        // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_RESOLVED), // ticketStatuses
                        doNothing()                       // orderAction
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_PREORDER_CONFIRMATION,        // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_RESOLVED, Ticket.STATUS_CLOSED),  //
                        // ticketStatuses
                        doNothing()                       // orderAction
                ),
                // fraud
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(),
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CANCELLED)
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.PROCESSING,            // orderStatus
                        List.of(Ticket.STATUS_PROCESSING),
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED)
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.DELIVERY,              // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_DEFERRED),
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED)
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.PICKUP,                // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_REOPENED),
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED)
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_RESOLVED),
                        doNothing()
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, Ticket.STATUS_RESOLVED, Ticket.STATUS_CLOSED),
                        doNothing()
                ),
                Arguments.of(
                        FraudConfirmationTicket.FQN,       // fqn
                        BERU_FRAUD_CONFIRMATION,           // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(Ticket.STATUS_PROCESSING, FraudConfirmationTicket.STATUS_FAILED),
                        doNothing()
                ),
                // cross-doc
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_CROSSDOC_CANCELLATION,        // serviceCode
                        OrderStatus.CANCELLED,             // orderStatus
                        List.of(),
                        doNothing()
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,                // fqn
                        BERU_CROSSDOC_CANCELLATION,        // serviceCode
                        OrderStatus.PROCESSING,            // orderStatus
                        List.of(),
                        doNothing()
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_CROSSDOC_CANCELLATION,       // serviceCode
                        OrderStatus.DELIVERY,             // orderStatus
                        List.of(),                        // ticketStatuses
                        doNothing()
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_CROSSDOC_CANCELLATION,       // serviceCode
                        OrderStatus.PICKUP,               // orderStatus
                        List.of(),                        // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED) // orderAction
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_CROSSDOC_CANCELLATION,       // serviceCode
                        OrderStatus.DELIVERED,            // orderStatus
                        List.of(),                        // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CONFIRMED)  // orderAction
                ),
                // manual tasks
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_ORDER_POSTPONE,              // serviceCode
                        OrderStatus.CANCELLED,            // orderStatus
                        List.of(),                        // ticketStatuses
                        closeTicketAutomatically(OrderAction.ORDER_WAS_CANCELLED) // orderAction
                ),
                Arguments.of(
                        BERU_OUTGOING_CALL,               // fqn
                        BERU_OTHER_ORDER_TASKS,           // serviceCode
                        OrderStatus.CANCELLED,            // orderStatus
                        List.of(),                        // ticketStatuses
                        doNothing()                       // orderAction
                )
        );
    }

    private static AutocloseAction closeTicketAutomatically(String action) {
        return new CloseTicketAutomatically(action);
    }

    private static AutocloseAction doNothing() {
        return new DoNothing();
    }

    @BeforeEach
    public void setup() {
        mockSecurityDataService.reset();
        ticketTestUtils.setServiceTime24x7(BERU_PREORDER_CONFIRMATION);
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_FRAUD_CONFIRMATION);
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_CROSSDOC_CANCELLATION);
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_ORDER_CANCELLATION);
        ticketTestUtils.setServiceTime24x7(BERU_ORDER_POSTPONE);
        ticketTestUtils.setServiceTime24x7(BERU_OTHER_ORDER_TASKS);

        //см. скрипт sendSmsOnDeferTicket
        final Ou ou = ouTestUtils.createOu();
        var employee = employeeTestUtils.createEmployee("employeeTitle", ou, 123L);
        mockSecurityDataService.setCurrentEmployee(employee);
        mockSmsCampaignService.mock();
        Mockito.when(geobaseClient.getTimeZoneByRegionId(213)).thenReturn("Asia/Yekaterinburg");
    }

    @AfterEach
    void tearDown() {
        mockSecurityDataService.reset();
        mockSmsCampaignService.clear();
        Mockito.reset(geobaseClient);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testAutoClose(Fqn fqn,
                              String serviceCode,
                              String orderStatus,
                              List<String> ticketStatuses,
                              AutocloseAction autocloseAction) {
        Order order = orderTestUtils.createOrder();
        TicketFirstLine ticket = createTicket(fqn, order, serviceCode);
        ticket = changeTicketStatus(ticket, ticketStatuses);

        final String initialTicketStatus = ticket.getStatus();
        LOG.info("Change status to {}", orderStatus);

        var oldOrder = entityService.clone(order);
        var editedOrder = bcpService.<Order>edit(order, Map.of(Order.STATUS, orderStatus));

        // Посылаем событие imported, эмулируя работу импорта заказов
        orderTestUtils.fireOrderImportedEvent(oldOrder, editedOrder);

        checkTicket(ticket, order, initialTicketStatus, autocloseAction);
    }

    private TicketFirstLine changeTicketStatus(TicketFirstLine ticket, List<String> ticketStatuses) {
        for (String ticketStatus : ticketStatuses) {
            Map<String, Object> properties;
            Map<String, Object> attributes;
            if (TicketFirstLine.STATUS_DEFERRED.equals(ticketStatus)) {
                properties = Map.of(
                        TicketFirstLine.STATUS, ticketStatus,
                        TicketFirstLine.DEFER_TIME, Duration.ZERO
                );
            } else {
                properties = Map.of(TicketFirstLine.STATUS, ticketStatus);
            }

            if (TicketFirstLine.STATUS_FAILED.equals(ticketStatus)) {
                attributes = Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true);
            } else {
                attributes = Map.of();
            }

            LOG.info("Change status to {}", ticketStatus);
            ticket = bcpService.edit(ticket, properties, attributes);
        }
        return ticket;
    }

    private void checkTicket(TicketFirstLine ticket,
                             Order order,
                             String initialTicketStatus,
                             AutocloseAction autocloseAction) {
        var actualTicket = dbService.<TicketFirstLine>get(ticket.getGid());
        if (autocloseAction instanceof DoNothing) {
            Assertions.assertEquals(initialTicketStatus, actualTicket.getStatus());
        } else {
            final CloseTicketAutomatically closeTicketAction = (CloseTicketAutomatically) autocloseAction;
            Assertions.assertEquals(TicketFirstLine.STATUS_RESOLVED, actualTicket.getStatus());
            Assertions.assertNotNull(actualTicket.getOrderAction());
            Assertions.assertEquals(closeTicketAction.getExpectedOrderAction(),
                    actualTicket.getOrderAction().getCode());
            // см. скрипт resetResolutionOnTransitionFromDeferred
            if (!Ticket.STATUS_DEFERRED.equals(initialTicketStatus)) {
                Assertions.assertNotNull(actualTicket.getResolution());
                Assertions.assertEquals(Resolution.SOLVED, actualTicket.getResolution().getCode());
            }
            checkComment(actualTicket, order);
        }
    }

    private void checkComment(TicketFirstLine ticket, Order order) {
        Query q = Query.of(InternalComment.FQN)
                .withFilters(
                        Filters.eq(Comment.ENTITY, ticket)
                )
                .withSortingOrder(SortingOrder.asc(Comment.CREATION_TIME));
        List<Comment> comments = dbService.list(q);
        // Удалим из списка первый комментарий т.к. он является описанием
        if (ticket.getService().isCreateInternalCommentFromDescription()) {
            comments.remove(0);
        }
        Assertions.assertEquals(1, comments.size());
        Assertions.assertEquals(
                String.format(
                        "Обращение решено автоматически, т.к. связанный заказ № %d перешел в статус %s",
                        order.getOrderId(), order.getStatus().getCode()
                ),
                comments.get(0).getBody()
        );
    }

    public TicketFirstLine createTicket(Fqn fqn, Order order, String serviceCode) {
        Map<String, Object> attributes = Maps.of(
                TicketFirstLine.SERVICE, serviceCode,
                TicketFirstLine.ORDER, order
                // TicketFirstLine.PRIORITY, priorityCode
                // TicketFirstLine.RESPONSIBLE_TEAM, team
        );

        return ticketTestUtils.createTicket(fqn, attributes);
    }


    private interface AutocloseAction {

    }

    private static class CloseTicketAutomatically implements AutocloseAction {
        private final String expectedOrderAction;

        public CloseTicketAutomatically(String expectedOrderAction) {
            this.expectedOrderAction = expectedOrderAction;
        }

        public String getExpectedOrderAction() {
            return expectedOrderAction;
        }

        @Override
        public String toString() {
            return "CloseTicketAutomatically{" +
                    "expectedOrderAction='" + expectedOrderAction + '\'' +
                    '}';
        }
    }

    private static class DoNothing implements AutocloseAction {
        @Override
        public String toString() {
            return "DoNothing{}";
        }
    }
}
