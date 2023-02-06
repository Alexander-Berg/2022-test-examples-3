package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.utils.MockOrderRules;
import ru.yandex.market.crm.operatorwindow.utils.MockScriptService;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.metric.test.impl.InMemoryMetricsService;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.OrderAction;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
public class CreatePreorderConfirmationTest extends AbstractModuleOwTest {

    private static final String TICKET_CREATION_ERROR = "TICKET_CREATION_ERROR";
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private MockCheckouterAPI mockCheckouterAPI;
    @Inject
    private MockOrderRules mockOrderRules;
    @Inject
    private MockScriptService mockScriptService;
    @Inject
    private InMemoryMetricsService testMetricsService;
    @Inject
    private TriggerServiceImpl triggerService;
    @Inject
    private Geobase geobaseClient;

    @BeforeEach
    public void setup() {
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_PREORDER_CONFIRMATION);

        mockCheckouterAPI.clear();
        mockOrderRules.clear();
        mockScriptService.clear();

        Mockito.when(geobaseClient.getTimeZoneByRegionId(213)).thenReturn("Asia/Yekaterinburg");
    }

    @AfterEach
    void tearDown() {
        mockCheckouterAPI.clear();
        mockOrderRules.clear();
        mockScriptService.clear();

        Mockito.reset(geobaseClient);
    }

    @Test
    @Transactional
    public void createPreorderConfirmationTicketOnNewOrder() {
        Order order = createOrder(Map.of(Order.PREORDER, true));

        Assertions.assertEquals(1, countPreorderConfirmationTickets(order));

        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);
        assertPreorderTicketData(createdPreorderTicket);
    }

    @Test
    @Transactional
    public void cannotCreatePreorderConfirmationTicket__waitMetricsWereWritten() {
        mockScriptService.registerError(
                (script, parameters) ->
                        "createPreorderConfirmationTicket".equals(script.getCode()),
                "cannot create ticket for preorder");


        var creationErrorsBefore = testMetricsService
                .getLoggedItemsByCode(TICKET_CREATION_ERROR)
                .size();

        try {
            createOrder(Map.of(Order.PREORDER, true));
            Assertions.fail();
        } catch (ValidationException e) {
            Assertions.assertEquals(MockScriptService.TestException.class, Throwables.getRootCause(e).getClass());
        }

        Assertions.assertEquals(
                creationErrorsBefore + 1,
                testMetricsService
                        .getLoggedItemsByCode(TICKET_CREATION_ERROR)
                        .size());
    }

    @Test
    @Transactional
    public void createPreorderConfirmationTicketOnPreorderAtFirstWasNotAwaitConfirmation() {
        Order order = createOrder(Map.of(
                Order.PREORDER, true,
                Order.SUB_STATUS, OrderSubstatus.PREORDER.name()
        ));

        Assertions.assertEquals(0, countPreorderConfirmationTickets(order));
        Assertions.assertTrue(getPreorderConfirmationTicket(order).isEmpty());

        bcpService.edit(order, Map.of(Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION.name()));
        orderTestUtils.fireOrderImportedEvent(order);

        Assertions.assertEquals(1, countPreorderConfirmationTickets(order));
        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);
        assertPreorderTicketData(createdPreorderTicket);
    }

    @Test
    @Transactional
    public void confirmOrderTest() {
        Order order = createOrder(Map.of(Order.PREORDER, true));

        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);

        bcpService.edit(createdPreorderTicket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        final Long orderId = order.getOrderId();
        mockCheckouterAPI.mockGetOrderNewApi(orderId, getCheckouterOrder(orderId));
        mockCheckouterAPI.mockBeenCalled(orderId);
        mockOrderRules.mockOrderCanBeConfirmed();

        bcpService.edit(createdPreorderTicket, Map.of(
                Ticket.STATUS, Ticket.STATUS_RESOLVED,
                Ticket.RESOLUTION, Resolution.SOLVED,
                TicketFirstLine.ORDER_ACTION, OrderAction.CONFIRM_ORDER
        ));

        mockCheckouterAPI.verifyBeenCalled(orderId);
    }

    @Test
    @Transactional
    public void cancelOrderTest() {
        Order order = createOrder(Map.of(Order.PREORDER, true));

        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);

        bcpService.edit(createdPreorderTicket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        final Long orderId = order.getOrderId();
        final ru.yandex.market.checkout.checkouter.order.Order checkouterOrder = getCheckouterOrder(orderId);

        mockCheckouterAPI.mockGetOrderNewApi(orderId, checkouterOrder);
        mockCheckouterAPI.mockCreateCancellationRequest(orderId, checkouterOrder);
        mockOrderRules.mockOrderCanBeCancelled();

        bcpService.edit(createdPreorderTicket, Map.of(
                Ticket.STATUS, Ticket.STATUS_RESOLVED,
                Ticket.RESOLUTION, Resolution.SOLVED,
                TicketFirstLine.ORDER_ACTION, OrderAction.CANCEL_ORDER
        ));

        mockCheckouterAPI.verifyCreateCancellationRequest(orderId);
    }

    @Test
    @Transactional
    public void reopenTicketWhenConfirmationFails() {
        Order order = createOrder(Map.of(Order.PREORDER, true));

        Ticket createdPreorderTicket = getCreatedPreorderTicket(order);

        bcpService.edit(createdPreorderTicket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        final Long orderId = order.getOrderId();
        mockCheckouterAPI.mockGetOrderNewApi(orderId, getCheckouterOrder(orderId));
        mockCheckouterAPI.mockBeenCalledWithErrorThrow(orderId);
        mockOrderRules.mockOrderCanBeConfirmed();

        boolean wasError = false;
        try {
            bcpService.edit(createdPreorderTicket, Map.of(
                    Ticket.STATUS, Ticket.STATUS_RESOLVED,
                    Ticket.RESOLUTION, Resolution.SOLVED,
                    TicketFirstLine.ORDER_ACTION, OrderAction.CONFIRM_ORDER
            ));
        } catch (RuntimeException ignored) {
            wasError = true;
        }

        Assertions.assertTrue(wasError);
        Assertions.assertEquals(Ticket.STATUS_REOPENED, getCreatedPreorderTicket(order).getStatus(), "тикет должен " +
                "переоткрыться, если при подтверждении заказа произошла ошибка");
    }

    @Test
    @Transactional
    public void reopenTicketWhenCancellationFails() {
        Order order = createOrder(Map.of(Order.PREORDER, true));

        final Ticket createdPreorderTicket = getCreatedPreorderTicket(order);

        bcpService.edit(createdPreorderTicket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        final Long orderId = order.getOrderId();
        final ru.yandex.market.checkout.checkouter.order.Order checkouterOrder = getCheckouterOrder(orderId);

        mockCheckouterAPI.mockGetOrderNewApi(orderId, getCheckouterOrder(orderId));
        mockCheckouterAPI.mockCreateCancellationRequestWithErrorThrow(orderId);
        mockOrderRules.mockOrderCanBeCancelled();

        boolean wasError = false;
        try {
            bcpService.edit(createdPreorderTicket, Map.of(
                    Ticket.STATUS, Ticket.STATUS_RESOLVED,
                    Ticket.RESOLUTION, Resolution.SOLVED,
                    TicketFirstLine.ORDER_ACTION, OrderAction.CONFIRM_ORDER
            ));
        } catch (RuntimeException ignored) {
            wasError = true;
        }

        Assertions.assertTrue(wasError);
        Assertions.assertEquals(Ticket.STATUS_REOPENED, getCreatedPreorderTicket(order).getStatus(), "тикет должен " +
                "переоткрыться, если при отмене заказа произошла ошибка");
    }

    @Test
    @Transactional
    public void dontCreateTicketWhenOrderDoesNotWaitConfirmation() {
        Order order = createOrder(Map.of(
                Order.PREORDER, true,
                Order.STATUS, OrderStatus.PROCESSING,
                Order.SUB_STATUS, OrderSubstatus.PROCESSING_EXPIRED
        ));

        Assertions.assertTrue(getPreorderConfirmationTicket(order).isEmpty(), "тикет не должен быть создан");
    }

    private ru.yandex.market.checkout.checkouter.order.Order getCheckouterOrder(Long orderId) {
        final ru.yandex.market.checkout.checkouter.order.Order order =
                new ru.yandex.market.checkout.checkouter.order.Order();
        order.setId(orderId);
        return order;
    }

    private void assertPreorderTicketData(Ticket ticket) {
        Assertions.assertEquals(Phone.fromRaw("+79998887766"), ticket.getClientPhone(), "в тикете должен быть указан " +
                "телефон");
        Assertions.assertEquals("Фердинанд Порше", ticket.getClientName(), "в тикете должно быть указано имя клиента");
        Assertions.assertEquals(Constants.Service.BERU_PREORDER_CONFIRMATION,
                ticket.getService().getCode(),
                "тикет должен быть в очереди " + Constants.Service.BERU_PREORDER_CONFIRMATION);
        Assertions.assertEquals("firstLinePhone", ticket.getResponsibleTeam().getCode(), "Тикет должен оказаться на " +
                "линии firstLinePhone");
    }

    private Order createOrder(Map<String, Object> attributesOverrides) {
        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put(Order.BUYER_PHONE, Phone.fromRaw("+79998887766"));
        attributes.put(Order.BUYER_FIRST_NAME, "Фердинанд");
        attributes.put(Order.BUYER_MIDDLE_NAME, "");
        attributes.put(Order.BUYER_LAST_NAME, "Порше");
        attributes.put(Order.STATUS, OrderStatus.PENDING.name());
        attributes.put(Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION.name());

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
        return list.isEmpty() ? Optional.empty() : Optional.of((Ticket) list.get(0));
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
