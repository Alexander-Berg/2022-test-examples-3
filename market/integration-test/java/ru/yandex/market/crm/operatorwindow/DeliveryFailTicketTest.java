package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.http.controller.orders.view.DeliveryCheckpointStatus;
import ru.yandex.market.crm.operatorwindow.jmf.entity.DeliveryFailedTicket;
import ru.yandex.market.crm.operatorwindow.jmf.script.OrderScriptServiceApi;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.timings.impl.TimingScriptServiceApi;
import ru.yandex.market.jmf.timings.test.TimingScriptServiceApiTest;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class DeliveryFailTicketTest extends AbstractModuleOwTest {
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private OrderScriptServiceApi orderScriptServiceApi;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private Geobase geobase;

    @BeforeEach
    @Transactional
    public void setupTestEnv() {
        orderTestUtils.clearCheckouterAPI();

        bcpService.edit(ConfigurationService.GID, Map.of(
                "deliveryFailedOrdersHandlingEnabled", true,
                "employeeActivityLessDuration", 60
        ));

        ticketTestUtils.setServiceTime24x7("beruDeliveryServiceFailedToDeliver");

        Mockito.reset(geobase);
        Mockito.when(geobase.getTimeZoneByRegionId(54))
                .thenReturn("Asia/Yekaterinburg");
    }

    @Test
    @Transactional
    public void createDeliveryFailedTicketOnNewOrder() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        assertDeliveryFailedTicketData(order);
    }

    @Test
    @Transactional
    public void createDeliveryFailedTicketOnOrderThatBecameFailed() {
        OffsetDateTime checkpointDate = OffsetDateTime.now();
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ARRIVED, checkpointDate);

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "тикетов на звонок пользователю пока не должно " +
                "быть");

        order = addTrackCheckpoint(order, DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED,
                checkpointDate.plusHours(1));

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        assertDeliveryFailedTicketData(order);
    }

    @Test
    @Transactional
    public void deliveryFailedTicketIsClosedOnOrderBecameNotFailed() {
        OffsetDateTime checkpointDate = OffsetDateTime.now();
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED, checkpointDate);

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        order = addTrackCheckpoint(order, DeliveryCheckpointStatus.DELIVERY_DELIVERED, checkpointDate.plusHours(1));

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "тикет на звонок клиенту должен был закрыться");
    }

    @Test
    @Transactional
    public void deliveryFailedTicketIsClosedOnOrderBecameCancelled() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        order = editStatus(order, "CANCELLED");

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "тикет на звонок клиенту должен был закрыться");
    }

    @Test
    @Transactional
    public void deliveryFailedTicketIsClosedOnOrderBecameDelivered() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        order = editStatus(order, "DELIVERED");

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "тикет на звонок клиенту должен был закрыться");
    }

    @Test
    @Transactional
    public void deliveryFailedTicketIsNotCreateTwiceForSameCheckpoint() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        Assertions.assertEquals(1, countDeliveryFailedTickets(order), "должен быть создан тикет на звонок клиенту");

        final var ticket = getDeliveryFailedTicket(order).orElseThrow();
        bcpService.edit(ticket, Map.of(
                DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_RESOLVED
        ));

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "тикет на звонок клиенту должен быть решен");

        orderTestUtils.fireOrderImportedEvent(order);

        Assertions.assertEquals(0, countDeliveryFailedTickets(order), "дублирующийся тикет на звонок клиенту не " +
                "должен быть создан");
    }

    @Nonnull
    private Order editStatus(Order order, String delivered) {
        Order result = bcpService.edit(order, Map.of(Order.STATUS, delivered));
        orderTestUtils.fireOrderImportedEvent(result);
        return result;
    }

    @Test
    @Disabled("FIXME")
    @Transactional
    public void checkDeferTimes() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        var ticket = getDeliveryFailedTicket(order).orElse(null);
        Assertions.assertNotNull(ticket, "Должны были создать тикет т.к. выполнился триггер " +
                "createTicketOnOrderDeliveryFail");

        checkFirstDeferring(ticket);
        checkSecondDeferring(ticket);
        checkThirdDeferring(ticket);
    }

    @Test
    @Disabled("FIXME")
    @Transactional
    public void checkLastDeferringCancelsOrderAndResolvesTicket() {
        Order order = createOrderWithTrackCheckpoint(DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED);

        final var ticket = getDeliveryFailedTicket(order).orElseThrow();

        checkFirstDeferring(ticket);
        checkSecondDeferring(ticket);
        checkThirdDeferring(ticket);

        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_REOPENED));
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_PROCESSING));
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_DEFERRED));

        verify(orderScriptServiceApi, times(1)).cancel(
                argThat(o -> Objects.equals(o.getGid(), order.getGid())),
                eq(OrderSubstatus.USER_UNREACHABLE),
                anyString()
        );
        Assertions.assertEquals(DeliveryFailedTicket.STATUS_RESOLVED, ticket.getStatus());
    }

    private void checkThirdDeferring(DeliveryFailedTicket ticket) {
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_REOPENED));
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_PROCESSING));
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_DEFERRED));

        Assertions.assertEquals(Duration.ofHours(3), ticket.getDeferTime());
    }

    /**
     * Проверяем, что правильно работает второе откладывание
     * <p>
     * Не проверяем работу метода {@link TimingScriptServiceApi#durationToStartTime} т.к. он проверен в своем тесте
     * {@link TimingScriptServiceApiTest#durationToStartTimeSimple()} ()}, а лишь проверяем, что в него пришли
     * правильные параметры
     */
    private void checkSecondDeferring(DeliveryFailedTicket ticket) {
        Duration expectedDuration = Duration.ofHours(3);

        final var actualDuration = ticket.getDeferTime();
        final var message = String.format("Второе откладывание должно отложить обращение до ближайшего времени, " +
                "когда пользователю можно позвонить, входящее во время обслуживания очереди: " +
                "expected %s, but was %s", expectedDuration, actualDuration);
        Assertions.assertTrue(expectedDuration.minus(actualDuration).abs().getSeconds() < 10, message);
    }

    private void checkFirstDeferring(DeliveryFailedTicket ticket) {
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_PROCESSING));
        bcpService.edit(ticket, Map.of(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_DEFERRED));

        Assertions.assertEquals(Duration.ofHours(2), ticket.getDeferTime());
    }

    private void assertDeliveryFailedTicketData(Order order) {
        getDeliveryFailedTicket(order).ifPresentOrElse(
                ticket -> {
                    Assertions.assertEquals("+79998887766", ticket.getClientPhone().getMain(), "в тикете должен быть " +
                            "указан телефон");
                    Assertions.assertEquals("Порше Фердинанд", ticket.getClientName(), "в тикете должно быть указано " +
                            "имя клиента");
                    Assertions.assertEquals(Team.FIRST_LINE_PHONE, ticket.getResponsibleTeam().getCode(), "тикет " +
                            "должен быть на линии роботов");
                    Assertions.assertEquals(Constants.Service.BERU_DELIVERY_SERVICE_FAILED_TO_DELIVER,
                            ticket.getService().getCode(), "тикет должен быть в очереди "
                                    + Constants.Service.BERU_DELIVERY_SERVICE_FAILED_TO_DELIVER);
                },
                () -> {
                    throw new IllegalStateException("There is no ticket");
                }
        );
    }

    private Order createOrderWithTrackCheckpoint(DeliveryCheckpointStatus checkpointStatus,
                                                 OffsetDateTime checkpointDate) {
        Map<String, Object> attributes = Maps.of(
                Order.BUYER_PHONE, Phone.fromRaw("+79998887766"),
                Order.RECIPIENT_FIRST_NAME, "Фердинанд",
                Order.RECIPIENT_MIDDLE_NAME, "",
                Order.RECIPIENT_LAST_NAME, "Порше",
                Order.DELIVERY_REGION_ID, 54,
                Order.STATUS, OrderStatus.DELIVERY.name(),
                Order.SUB_STATUS, checkpointStatus == DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED
                        ? OrderSubstatus.DELIVERY_PROBLEMS
                        : OrderSubstatus.DELIVERY_SERVICE_RECEIVED
        );

        var order = orderTestUtils.createOrder(attributes);

        orderTestUtils.createOrderTrackCheckpoint(order, checkpointStatus.getCode(), checkpointDate);
        orderTestUtils.fireOrderImportedEvent(order);

        return order;
    }

    private Order createOrderWithTrackCheckpoint(DeliveryCheckpointStatus checkpointStatus) {
        return createOrderWithTrackCheckpoint(checkpointStatus, OffsetDateTime.now());
    }

    private Order addTrackCheckpoint(Order order,
                                     DeliveryCheckpointStatus checkpointStatus,
                                     OffsetDateTime checkpointDate) {
        var attributes = new HashMap<String, Object>();
        attributes.put(Order.SUB_STATUS, checkpointStatus == DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED
                ? OrderSubstatus.DELIVERY_PROBLEMS
                : OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        bcpService.edit(order, attributes);

        orderTestUtils.createOrderTrackCheckpoint(order, checkpointStatus.getCode(), checkpointDate);
        orderTestUtils.fireOrderImportedEvent(order);

        return order;
    }


    private long countDeliveryFailedTickets(Order order) {
        Query query = getDeliveryFailedTicketQuery(order);
        return dbService.count(query);
    }

    private Optional<DeliveryFailedTicket> getDeliveryFailedTicket(Order order) {
        Query query = getDeliveryFailedTicketQuery(order)
                .withLimit(1);
        return dbService.<DeliveryFailedTicket>list(query)
                .stream()
                .findFirst();
    }

    private Query getDeliveryFailedTicketQuery(Order order) {
        return Query.of(DeliveryFailedTicket.FQN).withFilters(
                Filters.eq(DeliveryFailedTicket.ORDER, order),
                Filters.ne(DeliveryFailedTicket.STATUS, DeliveryFailedTicket.STATUS_RESOLVED)
        );
    }

}
