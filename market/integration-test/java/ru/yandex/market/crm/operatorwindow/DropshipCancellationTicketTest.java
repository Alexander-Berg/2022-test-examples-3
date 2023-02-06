package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.dao.OwPersistedPropertyId;
import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.utils.MockCouponScriptServiceApi;
import ru.yandex.market.crm.operatorwindow.utils.MockOrderRules;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.OrderAction;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Disabled("FIXME")
public class DropshipCancellationTicketTest extends AbstractModuleOwTest {

    public static final String DROPSHIP_CANCELLATION_REFUND = "DROPSHIP_CANCELLATION_REFUND";
    private static final int MAXIMUM_CROSS_DOC_DEFER_COUNT = 3;

    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private TriggerServiceImpl triggerService;

    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private CommentTestUtils commentTestUtils;

    @Inject
    private MockCheckouterAPI mockCheckouterAPI;
    @Inject
    private MockOrderRules mockOrderRules;
    @Inject
    private MockCouponScriptServiceApi mockCouponScriptServiceApi;


    @BeforeEach
    public void setUp() {
        mockCheckouterAPI.clear();
        mockOrderRules.clear();
        mockCouponScriptServiceApi.clear();

        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_CROSSDOC_CANCELLATION);
    }

    @Test
    @Transactional
    public void dropshipCancellationTicketTest() {
        enableAutomaticCouponCreation(true);

        Order order = createOrder(null);
        Assertions.assertEquals(0, countCrossdocCancellationTicketFirstLines(order), "Пока тикетов быть не должно");
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SHOP, HistoryEventType.ORDER_CANCELLATION_REQUESTED);

        List<TicketFirstLine> tickets = getOrderCancellationTicketFirstLines(order);
        Assertions.assertEquals(1, tickets.size(), "Должен быть создан один тикет");
        assertDropshipCancellationTicketFirstLine(tickets.get(0));

        mockCheckouter(order.getOrderId(), OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);
        mockCouponScriptServiceApi.setupCouponCreation(DROPSHIP_CANCELLATION_REFUND);

        Assertions.assertEquals(0, commentTestUtils.getComments(order).size());

        confirmCancellation(tickets.get(0));

        mockCouponScriptServiceApi.verifyCouponCreation(DROPSHIP_CANCELLATION_REFUND);
        assertOrderWasCancelled(order, OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);

        final List<Comment> comments = commentTestUtils.getComments(order);
        Assertions.assertEquals(1, comments.size());
        Assertions.assertTrue(comments.get(0).getBody().contains("Проинформирован"));
    }

    @Test
    @Transactional
    public void couponCreationIsDisabled__confimDropshipCancellation__expectCouponIsNotCreated() {
        enableAutomaticCouponCreation(false);

        Order order = createOrder(null);
        Assertions.assertEquals(0, countCrossdocCancellationTicketFirstLines(order), "Пока тикетов быть не должно");
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SHOP, HistoryEventType.ORDER_CANCELLATION_REQUESTED);

        List<TicketFirstLine> tickets = getOrderCancellationTicketFirstLines(order);
        Assertions.assertEquals(1, tickets.size(), "Должен быть создан один тикет");
        assertDropshipCancellationTicketFirstLine(tickets.get(0));

        mockCheckouter(order.getOrderId(), OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);

        confirmCancellation(tickets.get(0));

        mockCouponScriptServiceApi.verifyNoCouponCreationRequest();
        assertOrderWasCancelled(order, OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);
    }

    @Test
    @Transactional
    public void maximumDeferCountExceeded__expectOrderWasCancelled() {
        enableAutomaticCouponCreation(true);

        Order order = createOrder(null);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SHOP, HistoryEventType.ORDER_CANCELLATION_REQUESTED);

        TicketFirstLine ticket = getOrderCancellationTicket(order);

        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        mockCheckouter(order.getOrderId(), OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);
        mockCouponScriptServiceApi.setupCouponCreation(DROPSHIP_CANCELLATION_REFUND);

        deferTicket(ticket, MAXIMUM_CROSS_DOC_DEFER_COUNT);

        mockCouponScriptServiceApi.verifyCouponCreation(DROPSHIP_CANCELLATION_REFUND);
        assertOrderWasCancelled(order, OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);

        Assertions.assertFalse(commentTestUtils.getComments(order)
                .stream()
                .anyMatch(x -> x.getBody().contains("Проинформирован")));
    }

    @Test
    @Transactional
    public void maximumDeferCountWasAchieved__expectDeferActionIsNotAvailable() {
        enableAutomaticCouponCreation(true);

        Order order = createOrder(null);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SHOP, HistoryEventType.ORDER_CANCELLATION_REQUESTED);

        TicketFirstLine ticket = getOrderCancellationTicket(order);

        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        mockCheckouter(order.getOrderId(), OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP);

        authRunnerService.setCurrentUserSuperUser(false);
        securityDataService.setCurrentUserProfiles("operator", "responsibleEmployee");
        deferTicket(ticket, MAXIMUM_CROSS_DOC_DEFER_COUNT - 1);

        // superuser здесь нужен, чтобы эмулировать возврат в processing
        // а ответственный сотрудник не может переоткрывать тикет
        // тк есть скрипт ограничения
        authRunnerService.setCurrentUserSuperUser(true);
        changeTicketStatus(ticket, Ticket.STATUS_REOPENED);
        authRunnerService.setCurrentUserSuperUser(false);

        changeTicketStatus(ticket, Ticket.STATUS_PROCESSING);

        try {
            deferTicket(ticket, MAXIMUM_CROSS_DOC_DEFER_COUNT);
            Assertions.fail("последующий перенос в отложенные невозможен");
        } catch (ValidationException e) {
            Assertions.assertTrue(e.getMessage()
                    .contains("Отсутствует право изменять атрибут status 'Статус'"), "доступ на перенос в отложенные");
        }
    }

    private void assertDropshipCancellationTicketFirstLine(TicketFirstLine ticket) {
        Assertions.assertEquals(
                "+79998887766", ticket.getClientPhone().getMain(), "в тикете должен быть указан телефон");
        Assertions.assertEquals(
                "Фердинанд Порше", ticket.getClientName(), "в тикете должно быть указано имя клиента");
        Assertions.assertEquals(
                Team.FIRST_LINE_PHONE, ticket.getResponsibleTeam().getCode(), "тикет должен быть на линии телефонии");
        Assertions.assertEquals(
                Constants.Service.BERU_CROSSDOC_CANCELLATION,
                ticket.getService().getCode(), "тикет должен быть в очереди "
                        + Constants.Service.BERU_CROSSDOC_CANCELLATION);
    }

    private void assertOrderWasCancelled(Order order,
                                         OrderSubstatus orderSubstatus) {
        mockCheckouterAPI.mockUpdateOrderStatus(
                order.getOrderId(),
                OrderStatus.CANCELLED,
                orderSubstatus);
    }

    private void mockCheckouter(long orderId, OrderSubstatus orderSubstatus) {
        var cancellationRequest = new CancellationRequest(orderSubstatus, null);
        var order = new ru.yandex.market.checkout.checkouter.order.Order();

        order.setId(orderId);
        order.setShopId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setCancellationRequest(cancellationRequest);

        mockCheckouterAPI.mockGetOrderNewApi(orderId, order);
        mockCheckouterAPI.mockUpdateOrderStatusAndReturnOrder(order);

        mockOrderRules.canBeCancelConfirmed();
        mockOrderRules.shouldSendCancelConfirmToCheckouter();
    }

    private Order createOrder(Map<String, Object> attributesOverrides) {
        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put(Order.BUYER_PHONE, Phone.fromRaw("+79998887766"));
        attributes.put(Order.BUYER_FIRST_NAME, "Фердинанд");
        attributes.put(Order.BUYER_MIDDLE_NAME, "");
        attributes.put(Order.BUYER_LAST_NAME, "Порше");
        attributes.put(Order.DELIVERY_REGION_ID, 54);
        attributes.put(Order.STATUS, OrderStatus.PENDING.name());
        attributes.put(Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION);

        if (null != attributesOverrides) {
            attributes.putAll(attributesOverrides);
        }

        return orderTestUtils.createOrder(attributes);
    }

    private void confirmCancellation(TicketFirstLine ticket) {
        bcpService.edit(ticket, Map.of(TicketFirstLine.STATUS, TicketFirstLine.STATUS_PROCESSING));
        bcpService.edit(ticket, Map.of(
                TicketFirstLine.STATUS, TicketFirstLine.STATUS_RESOLVED,
                TicketFirstLine.RESOLUTION, Resolution.SOLVED,
                TicketFirstLine.ORDER_ACTION, OrderAction.CONFIRM_CROSSDOC_ORDER_CANCELLATION
        ));
    }

    private long countCrossdocCancellationTicketFirstLines(Order order) {
        Query query = getOrderCancellationTicketFirstLineQuery(order);
        return dbService.count(query);
    }

    private List<TicketFirstLine> getOrderCancellationTicketFirstLines(Order order) {
        Query query = getOrderCancellationTicketFirstLineQuery(order);
        return dbService.list(query);
    }

    private TicketFirstLine getOrderCancellationTicket(Order order) {
        final List<TicketFirstLine> tickets =
                getOrderCancellationTicketFirstLines(order);
        Assertions.assertEquals(1, tickets.size());
        return tickets.get(0);
    }

    private Query getOrderCancellationTicketFirstLineQuery(Order order) {
        return Query.of(TicketFirstLine.FQN)
                .withFilters(Filters.and(
                        Filters.eq(TicketFirstLine.ORDER, order),
                        Filters.eq(TicketFirstLine.SERVICE, Constants.Service.BERU_CROSSDOC_CANCELLATION)
                ));
    }

    private void deferTicket(Ticket ticket, int deferCount) {
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_DEFERRED,
                // эмулируем несколько переносов сразу, фактически проверяя сразу граничный сценарий
                // иначе приходится возвращать тикет в нужный статус
                // а для этого приходится принести кучу конфигов
                // т.к. появляются доп зависимости на телефонию (статусы disconnected) итд
                Ticket.DEFER_COUNT, deferCount,
                Ticket.DEFER_TIME, Duration.ofDays(1)
        ));
    }

    private void changeTicketStatus(Ticket ticket, String status) {
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, status
        ));
    }

    private void enableAutomaticCouponCreation(boolean enable) {
        configurationService.setValue(OwPersistedPropertyId.CREATE_COUPON_AUTOMATICALLTY_FOR_ORDER_CANCELLATION.key(),
                enable);
    }

}
