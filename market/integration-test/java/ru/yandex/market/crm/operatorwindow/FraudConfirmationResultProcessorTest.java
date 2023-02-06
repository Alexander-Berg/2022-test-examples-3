package ru.yandex.market.crm.operatorwindow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.AttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.CallRecordResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudAttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudCallData;
import ru.yandex.market.crm.operatorwindow.jmf.entity.FraudConfirmationTicket;
import ru.yandex.market.crm.operatorwindow.services.fraud.FraudConfirmationResultProcessor;
import ru.yandex.market.crm.operatorwindow.services.task.calltime.NearestCallTime;
import ru.yandex.market.crm.operatorwindow.utils.MockCustomerCallTimeService;
import ru.yandex.market.crm.operatorwindow.utils.MockSmartcalls;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
public class FraudConfirmationResultProcessorTest extends AbstractModuleOwTest {

    @Inject
    private FraudConfirmationResultProcessor processor;

    @Inject
    private MockCheckouterAPI mockCheckouterAPI;

    @Inject
    private MockSmartcalls mockSmartcalls;

    @Inject
    private MockCustomerCallTimeService mockCustomerCallTimeService;

    @Inject
    private BcpService bcpService;

    @Inject
    private DbService dbService;

    @Inject
    private TxService txService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private OrderTestUtils orderTestUtils;

    @Inject
    private TriggerServiceImpl triggerService;

    @Inject
    private Geobase geobaseClient;

    @BeforeEach
    public void setUp() {
        mockCheckouterAPI.clear();
        mockCustomerCallTimeService.clear();
        mockSmartcalls.clear();
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_FRAUD_CONFIRMATION);

        //см. скрипт sendFraudConfirmationTicketToSmartcalls
        var options = new OrderEditOptions();
        options.setCurrentDeliveryOptionActual(true);
        mockCheckouterAPI.mockGetOrderEditOptions(options);
        mockSmartcalls.mockAppendToCampaign();
        Mockito.when(geobaseClient.getTimeZoneByRegionId(213)).thenReturn("Asia/Yekaterinburg");
    }

    @AfterEach
    void tearDown() {
        mockCheckouterAPI.clear();
        mockCustomerCallTimeService.clear();
        mockSmartcalls.clear();
        Mockito.reset(geobaseClient);
    }

    @Test
    public void whenBuyerConfirmsOrderAndCheckouterDoesConfirmation() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);
        withSmartcallsAnswer(orderId, true, true);
        whenCheckouterCanConfirmOrder(orderId, true);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket -> {
                    Assertions.assertEquals(FraudConfirmationTicket.STATUS_RESOLVED, ticket.getStatus());
                    assertHasOneInternalComment(ticket, "Заказ подтверждён");
                }
        );

        mockCheckouterAPI.verifyBeenCalled(orderId);
    }

    @Test
    public void whenBuyerConfirmsOrderAndCheckouterFailsConfirmation() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);

        withSmartcallsAnswer(orderId, true, true);
        whenCheckouterCanConfirmOrder(orderId, false);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket -> {
            Assertions.assertEquals(
                    Constants.Service.BERU_FRAUD_CONFIRMATION, ticket.getService().getCode(),
                    "тикет остался в очереди подтверждения фрод-заказов");
            Assertions.assertEquals(
                    Team.FIRST_LINE_PHONE, ticket.getResponsibleTeam().getCode(), "тикет перенесён на линию телефонии");
            Assertions.assertEquals(
                    FraudConfirmationTicket.STATUS_REOPENED, ticket.getStatus(), "тикет переоткрыт");
            Assertions.assertEquals(0L, (long) ticket.getDeferCount(), "сброшен счётчик откладываний");
        });

        mockCheckouterAPI.verifyBeenCalled(orderId);
    }

    @Test
    public void whenBuyerNotConfirmOrderAndCheckouterDoesCancelling() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);

        withSmartcallsAnswer(orderId, true, false);
        whenCheckouterCanCancelOrder(orderId, true);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket -> {
                    Assertions.assertEquals(FraudConfirmationTicket.STATUS_RESOLVED, ticket.getStatus());
                    assertHasOneInternalComment(ticket, "Заказ отменён");
                }
        );

        mockCheckouterAPI.verifyCreateCancellationRequest(orderId);
    }

    @Test
    public void whenBuyerNotConfirmOrderAndCheckouterFailsCancelling() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);

        withSmartcallsAnswer(orderId, true, false);
        whenCheckouterCanCancelOrder(orderId, false);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket ->
                Assertions.assertEquals(FraudConfirmationTicket.STATUS_FAILED, ticket.getStatus())
        );

        mockCheckouterAPI.verifyCreateCancellationRequest(orderId);
    }

    @Test
    public void whenBuyerDoesNotAnswer() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);

        withSmartcallsAnswer(orderId, false, false);
        withNextAllowedCallTime(OffsetDateTime.now().plusMinutes(5));

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket -> {
                    Assertions.assertEquals(FraudConfirmationTicket.STATUS_DEFERRED, ticket.getStatus());
                    assertHasOneInternalComment(ticket, "Абонент недоступен");
                }
        );
    }

    @Test
    public void whenBuyerDoesNotAnswerAndDefersLimitExceededAndCheckouterDoesCancelling() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        FraudConfirmationTicket ticket = createJmfConfirmationTicket(order);
        updateJmfTicket(ticket, Map.of(FraudConfirmationTicket.DEFER_COUNT, 5));

        withSmartcallsAnswer(orderId, false, false);
        withNextAllowedCallTime(OffsetDateTime.now().plusMinutes(5));
        whenCheckouterCanCancelOrder(orderId, true);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, reloadedTicket -> {
                    Assertions.assertEquals(FraudConfirmationTicket.STATUS_RESOLVED, reloadedTicket.getStatus());
                    assertHasOneInternalComment(ticket, "Заказ отменён");
                }
        );

        mockCheckouterAPI.verifyCreateCancellationRequest(orderId);
    }

    @Test
    public void whenBuyerDoesNotAnswerAndDefersLimitExceededAndCheckouterFailsCancelling() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        FraudConfirmationTicket ticket = createJmfConfirmationTicket(order);
        updateJmfTicket(ticket, Map.of(FraudConfirmationTicket.DEFER_COUNT, 5));

        withSmartcallsAnswer(orderId, false, false);
        withNextAllowedCallTime(OffsetDateTime.now().plusMinutes(5));
        whenCheckouterCanCancelOrder(orderId, false);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, reloadedTicket ->
                Assertions.assertEquals(FraudConfirmationTicket.STATUS_FAILED, reloadedTicket.getStatus())
        );

        mockCheckouterAPI.verifyCreateCancellationRequest(orderId);
    }

    @Test
    public void whenBuyerDoesNotAnswerAndNoNextAllowedCallTime() {
        Order order = createJmfOrder();
        long orderId = order.getOrderId();
        createJmfConfirmationTicket(order);

        withSmartcallsAnswer(orderId, false, false);
        withNextAllowedCallTime(null);

        processor.processConfirmationResults();

        withReloadedJmfTicketForOrder(orderId, ticket ->
                Assertions.assertEquals(FraudConfirmationTicket.STATUS_FAILED, ticket.getStatus())
        );
    }

    // === assertions ===

    public void assertHasOneInternalComment(Entity entity, String comment) {
        assertHasOneComment(entity, InternalComment.FQN, comment);
    }

    public void assertHasOneComment(Entity entity, Fqn commentTypeFqn, String comment) {
        List<Comment> comments = dbService.list(Query.of(commentTypeFqn)
                .withFilters(Filters.eq(Comment.ENTITY, entity)));
        List<String> foundComments = comments.stream()
                .map(Comment::getBody)
                .filter(comment::equals)
                .collect(Collectors.toList());
        Assertions.assertFalse(
                foundComments.isEmpty(), String.format("С объектом %s должен быть связан комментарий '%s'",
                        entity.getGid(), comment));
        Assertions.assertEquals(
                1, foundComments.size(),
                String.format("С объектом %s должен быть связан ровно один комментарий '%s'", entity.getGid(), comment)
        );
    }

    // === utils ====

    private Order createJmfOrder() {
        long orderId = Randoms.unsignedLongValue();
        return txService.doInNewTx(() ->
                orderTestUtils.createOrder(orderId, OffsetDateTime.now(), Map.of(
                        Order.STATUS, OrderStatus.PENDING,
                        Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION
                ))
        );
    }

    private FraudConfirmationTicket createJmfConfirmationTicket(Order order) {
        return txService.doInNewTx(() -> {
            FraudConfirmationTicket ticket = bcpService.create(FraudConfirmationTicket.FQN, Map.of(
                    FraudConfirmationTicket.TITLE, "Тикет для заказа " + order.getOrderId(),
                    FraudConfirmationTicket.CHANNEL, "phone",
                    FraudConfirmationTicket.SERVICE, Constants.Service.BERU_FRAUD_CONFIRMATION,
                    FraudConfirmationTicket.CLIENT_PHONE, Phone.fromNormalized(Randoms.phoneNumber()),
                    FraudConfirmationTicket.ORDER, order
            ));

            bcpService.edit(ticket, Map.of(
                    FraudConfirmationTicket.STATUS, FraudConfirmationTicket.STATUS_PROCESSING
            ));

            return ticket;
        });
    }

    private void updateJmfTicket(FraudConfirmationTicket ticket, Map<String, Object> attrs) {
        txService.runInNewTx(() ->
                bcpService.edit(ticket, attrs)
        );
    }

    private void withReloadedJmfTicketForOrder(long orderId, Consumer<FraudConfirmationTicket> action) {
        txService.runInNewTx(() -> {
            List<FraudConfirmationTicket> tickets = dbService.list(
                    Query.of(FraudConfirmationTicket.FQN)
                            .withFilters(Filters.eq(FraudConfirmationTicket.ORDER, orderId))
                            .withAttributes("service")
            );

            if (tickets.isEmpty()) {
                throw new RuntimeException("Ticket not found");
            }

            action.accept(tickets.get(0));
        });
    }

    private void withSmartcallsAnswer(long orderId, boolean isAnswerReceived, boolean isOrderConfirmed) {
        ConfirmFraudAttemptResult smartcallsAnswer = new ConfirmFraudAttemptResult(
                new AttemptResult(
                        "one",
                        1,
                        null,
                        isAnswerReceived,
                        "",
                        "0",
                        0,
                        false,
                        3,
                        null,
                        null,
                        null,
                        null
                ),
                new ConfirmFraudCallData(
                        null,
                        String.valueOf(orderId),
                        null,
                        isOrderConfirmed ? "1" : "0",
                        null
                ),
                List.of(new CallRecordResult(null))
        );

        mockSmartcalls.mockGet(List.of(smartcallsAnswer));
    }

    private void whenCheckouterCanConfirmOrder(long orderId, boolean success) {
        if (success) {
            mockCheckouterAPI.mockBeenCalled(orderId);
        } else {
            mockCheckouterAPI.mockBeenCalledWithErrorThrow(orderId);
        }
    }

    private void whenCheckouterCanCancelOrder(long orderId, boolean success) {
        if (success) {
            mockCheckouterAPI.mockCreateCancellationRequest(orderId, null);
        } else {
            mockCheckouterAPI.mockCreateCancellationRequestWithErrorThrow(orderId);
        }
    }

    private void withNextAllowedCallTime(OffsetDateTime dateTime) {
        Optional<NearestCallTime> nextCallTime = Optional.ofNullable(dateTime)
                .map(dt -> new NearestCallTime(dt, dt, dt));
        mockCustomerCallTimeService.mockGetNearestCallTime(nextCallTime);
    }
}
