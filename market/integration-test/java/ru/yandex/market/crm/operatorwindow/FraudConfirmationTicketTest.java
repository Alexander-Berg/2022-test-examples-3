package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsClient;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsConfiguration;
import ru.yandex.market.crm.operatorwindow.jmf.entity.FraudConfirmationTicket;
import ru.yandex.market.crm.operatorwindow.jmf.script.OrderScriptServiceApi;
import ru.yandex.market.crm.operatorwindow.services.fraud.FraudConfirmationTicketScriptServiceApi;
import ru.yandex.market.crm.operatorwindow.services.geo.TimeZoneResolver;
import ru.yandex.market.crm.operatorwindow.utils.EntityValueMatcher;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.impl.TimingScriptServiceApi;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.dao.OwPersistedPropertyId.CREATE_ANTIFRAUD_TICKETS_BY_PENDING_ANTIFRAUD_ORDER_SUBSTATUS;

@Disabled("FIXME")
public class FraudConfirmationTicketTest extends AbstractModuleOwTest {

    @Inject
    ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    SmartcallsConfiguration smartcallsConfiguration;
    @Inject
    OrderTestUtils orderTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private SmartcallsClient smartcallsMock;
    @Inject
    private TimeZoneResolver timeZoneResolverMock;
    @Inject
    private TimingScriptServiceApi timingScriptServiceApi;

    @Inject
    private OrderScriptServiceApi orderScriptServiceApi;

    @Inject
    private FraudConfirmationTicketScriptServiceApi fraudConfirmationTicketScriptServiceApi;

    @BeforeEach
    public void setup() {
        resetMocks();
        setupMocks();

        configurationService.setValue(CREATE_ANTIFRAUD_TICKETS_BY_PENDING_ANTIFRAUD_ORDER_SUBSTATUS.key(), true);
    }


    @Test
    @Transactional
    public void createFraudConfirmationTicketOnNewOrder() {
        setupDeliveryOptionIsActual(true);
        Order order = createOrder(Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true));

        Assertions.assertEquals(
                1, countFraudConfirmationTickets(order), "должен быть создан тикет на подтверждение фрод-заказа");

        assertFraudTicketData(order);
        verifySmartcallsClientInvocation(order, 1);
    }

    @Test
    @Transactional
    public void createFraudConfirmationTiketOnAntifraudSubStatus() {
        setupDeliveryOptionIsActual(true);
        Order order = createOrder(
                Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true,
                        Order.IS_DROPSHIP, true,
                        Order.SUB_STATUS, OrderSubstatus.ANTIFRAUD));

        Assertions.assertEquals(
                1, countFraudConfirmationTickets(order), "должен быть создан тикет на подтверждение фрод-заказа");

        assertFraudTicketData(order);
        verifySmartcallsClientInvocation(order, 1);
    }

    @Test
    @Transactional
    public void deliveryOptionIsExpired__waitTicketWasMovedToOperator() {
        setupDeliveryOptionIsActual(false);
        Order order = createOrder(Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true));

        Ticket ticket = getFraudConfirmationTicket(order).orElseThrow();

        verifySmartcallsClientInvocation(order, 0);
        verifyForwardToOperator(ticket, 1);
    }

    @Test
    @Transactional
    public void createFraudConfirmationTicketOnOrderThatBecameFraud() {
        setupDeliveryOptionIsActual(true);
        Order order = createOrder(Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, false));

        Assertions.assertEquals(
                0, countFraudConfirmationTickets(order), "тикетов на подтверждение фрод-заказов пока не должно быть");

        order = updateOrder(order, Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true));

        Assertions.assertEquals(
                1, countFraudConfirmationTickets(order), "должен быть создан тикет на подтверждение фрод-заказа");

        assertFraudTicketData(order);
        verifySmartcallsClientInvocation(order, 1);
    }

    @Test
    @Transactional
    public void sendFraudConfirmationTicketToSmartcallsWhenItReopenedAfterDeferring() {
        setupDeliveryOptionIsActual(true);
        Order order = createOrder(Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true));

        Ticket ticket = getFraudConfirmationTicket(order).orElseThrow();

        // мы отправили тикет в smartcalls при создании
        verifySmartcallsClientInvocation(order, 1);

        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_DEFERRED,
                Ticket.DEFER_TIME, Duration.ofHours(1)
        ));

        // мы не отправили второй раз тикет в smartcalls при откладывании
        verifySmartcallsClientInvocation(order, 1);

        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_REOPENED
        ));

        // мы отправили второй раз тикет в smartcalls при переоткрытии
        verifySmartcallsClientInvocation(order, 2);
    }

    @Test
    @Transactional
    public void deferFraudConfirmationTicketWhenCallIsNotAllowedNow() {
        when(timingScriptServiceApi.isNowServiceTime(ArgumentMatchers.<ServiceTime>any(), ArgumentMatchers.any())).thenReturn(false);
        setupDeliveryOptionIsActual(true);

        Order order = createOrder(Map.of(Order.IS_SUSPECTED_BY_ANTIFRAUD, true));
        Ticket ticket = getFraudConfirmationTicket(order).orElseThrow();

        Assertions.assertEquals(Ticket.STATUS_DEFERRED, ticket.getStatus(), "тикет должен быть отложен");
        Assertions.assertEquals(0L, (long) ticket.getDeferCount(), "не должно увеличиться число откладываний");

        // мы не отправили тикет в smartcalls
        verifySmartcallsClientInvocation(order, 0);
    }

    private void setupMocks() {
        Mockito.when(smartcallsMock.appendToCampaign(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyMap()
        )).thenReturn(true);

        Mockito.when(timeZoneResolverMock.get(ArgumentMatchers.anyLong())).thenReturn("GMT");
        when(timingScriptServiceApi.isNowServiceTime(ArgumentMatchers.<ServiceTime>any(), ArgumentMatchers.any())).thenReturn(true);
    }

    public void resetMocks() {
        Mockito.reset(smartcallsMock);
    }

    private void assertFraudTicketData(Order order) {
        getFraudConfirmationTicket(order).ifPresent(ticket -> {
            Assertions.assertEquals(
                    "+79998887766", ticket.getClientPhone().getMain(), "в тикете должен быть указан телефон");
            Assertions.assertEquals(
                    "Фердинанд Порше", ticket.getClientName(), "в тикете должно быть указано имя клиента");
            Assertions.assertEquals(
                    Team.ROBOTS, ticket.getResponsibleTeam().getCode(), "тикет должен быть на линии роботов");
            Assertions.assertEquals(
                    Constants.Service.BERU_FRAUD_CONFIRMATION,
                    ticket.getService().getCode(), "тикет должен быть в очереди "
                            + Constants.Service.BERU_FRAUD_CONFIRMATION);
        });
    }

    private void verifySmartcallsClientInvocation(Order order, int wantedNumberOfInvocations) {
        Mockito.verify(smartcallsMock, Mockito.times(wantedNumberOfInvocations))
                .appendToCampaign(
                        ArgumentMatchers.eq(smartcallsConfiguration.getFraudCampaign()),
                        ArgumentMatchers.eq(order.getBuyerPhone()),
                        ArgumentMatchers.anyMap()
                );
    }

    private void verifyForwardToOperator(Ticket expectedTicket,
                                         int wantedNumberOfInvocations) {
        Mockito.verify(fraudConfirmationTicketScriptServiceApi, Mockito.times(wantedNumberOfInvocations))
                .forwardToOperator(
                        ArgumentMatchers.argThat(new EntityValueMatcher<FraudConfirmationTicket>(
                                expectedTicket::equals)),
                        ArgumentMatchers.eq("Опции доставки не актуальны. Для обработки обращение было переведено на " +
                                "операторов.")
                );
    }

    private void setupDeliveryOptionIsActual(boolean isDeliveryOptionActual) {
        Mockito.when(orderScriptServiceApi.isDeliveryOptionActual(
                        ArgumentMatchers.anyLong()
                ))
                .thenReturn(isDeliveryOptionActual);
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

        final var order = orderTestUtils.createOrder(attributes);
        orderTestUtils.fireOrderImportedEvent(order);
        return order;
    }

    private Order updateOrder(Order order, Map<String, Object> updatedAttributes) {
        final Order edited = bcpService.edit(order, updatedAttributes);
        orderTestUtils.fireOrderImportedEvent(edited);
        return edited;
    }


    private long countFraudConfirmationTickets(Order order) {
        Query query = getFraudConfirmationTicketQuery(order);
        return dbService.count(query);
    }

    private Optional<FraudConfirmationTicket> getFraudConfirmationTicket(Order order) {
        Query query = getFraudConfirmationTicketQuery(order);
        List<Entity> list = dbService.list(query);

        return list.isEmpty() ? Optional.empty() : Optional.of((FraudConfirmationTicket) list.get(0));
    }

    private Query getFraudConfirmationTicketQuery(Order order) {
        return Query.of(FraudConfirmationTicket.FQN).withFilters(Filters.eq("order", order));
    }
}
