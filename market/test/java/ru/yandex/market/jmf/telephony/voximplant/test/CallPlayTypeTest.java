package ru.yandex.market.jmf.telephony.voximplant.test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.telephony.voximplant.CallPlayType;
import ru.yandex.market.jmf.telephony.voximplant.OutboundTelephonyService;
import ru.yandex.market.jmf.telephony.voximplant.TelephonyTicket;
import ru.yandex.market.jmf.timings.attributes.TimerStatus;
import ru.yandex.market.jmf.tx.TxService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = VoximplantTestConfiguration.class)
@TestPropertySource("classpath:vox_test.properties")
public class CallPlayTypeTest {
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private RetryTaskProcessor retryTaskProcessor;
    @Inject
    private FastRetryTasksQueue queue;
    @Inject
    private TxService txService;

    @Test
    @Transactional
    public void testPreviewProgressiveTimer_notStarted() {
        final var ctx = ticketTestUtils.create();

        final var service = ticketTestUtils.createService(
                ctx.team0,
                ctx.serviceTime24x7,
                ctx.brand,
                Optional.empty(),
                OutboundTelephonyService.FQN,
                Map.of(
                        OutboundTelephonyService.CALL_PLAY_TYPE, CallPlayType.PREVIEW_PROGRESSIVE,
                        OutboundTelephonyService.WAIT_FOR_CALL_TIME, "PT1S"
                )
        );

        final TelephonyTicket ticket = bcpService.create(Fqn.of("ticket$outgoingCall"), Map.of(
                Ticket.SERVICE, service,
                Ticket.RESPONSIBLE_TEAM, ctx.team0,
                Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                Ticket.TITLE, "Test"
        ));

        Assertions.assertEquals(TimerStatus.NOT_STARTED, ticket.getWaitForCallBackTimer().getStatus());
    }

    @Test
    @Transactional
    public void testPreviewProgressiveTimer_started() {
        final var ctx = ticketTestUtils.create();

        final var service = ticketTestUtils.createService(
                ctx.team0,
                ctx.serviceTime24x7,
                ctx.brand,
                Optional.empty(),
                OutboundTelephonyService.FQN,
                Map.of(
                        OutboundTelephonyService.CALL_PLAY_TYPE, CallPlayType.PREVIEW_PROGRESSIVE,
                        OutboundTelephonyService.WAIT_FOR_CALL_TIME, "PT1S"
                )
        );

        final TelephonyTicket ticket = bcpService.create(Fqn.of("ticket$outgoingCall"), Map.of(
                Ticket.SERVICE, service,
                Ticket.RESPONSIBLE_TEAM, ctx.team0,
                Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                Ticket.TITLE, "Test"
        ));

        bcpService.edit(ticket, Map.of(
                Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));

        Assertions.assertEquals(TimerStatus.ACTIVE, ticket.getWaitForCallBackTimer().getStatus());
    }

    @Test
    @Transactional
    public void testPreviewProgressiveTimer_reset() {
        final var ctx = ticketTestUtils.create();

        final var service = ticketTestUtils.createService(
                ctx.team0,
                ctx.serviceTime24x7,
                ctx.brand,
                Optional.empty(),
                OutboundTelephonyService.FQN,
                Map.of(
                        OutboundTelephonyService.CALL_PLAY_TYPE, CallPlayType.PREVIEW_PROGRESSIVE,
                        OutboundTelephonyService.WAIT_FOR_CALL_TIME, "PT1S"
                )
        );

        final TelephonyTicket ticket = bcpService.create(Fqn.of("ticket$outgoingCall"), Map.of(
                Ticket.SERVICE, service,
                Ticket.RESPONSIBLE_TEAM, ctx.team0,
                Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                Ticket.TITLE, "Test"
        ));

        bcpService.edit(ticket, Map.of(
                Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));

        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, Ticket.STATUS_REOPENED
        ));

        Assertions.assertEquals(TimerStatus.NOT_STARTED, ticket.getWaitForCallBackTimer().getStatus());
    }

    @Test
    public void testPreviewProgressiveTimer_resetAfterTimer() throws InterruptedException {
        final var ctx = txService.doInNewTx(ticketTestUtils::create);

        var ticket = txService.doInNewTx(() -> {
            final var service = ticketTestUtils.createService(
                    ctx.team0,
                    ctx.serviceTime24x7,
                    ctx.brand,
                    Optional.empty(),
                    OutboundTelephonyService.FQN,
                    Map.of(
                            OutboundTelephonyService.CALL_PLAY_TYPE, CallPlayType.PREVIEW_PROGRESSIVE,
                            OutboundTelephonyService.WAIT_FOR_CALL_TIME, "PT1S"
                    )
            );

            return bcpService.<TelephonyTicket>create(Fqn.of("ticket$outgoingCall"), Map.of(
                    Ticket.SERVICE, service,
                    Ticket.RESPONSIBLE_TEAM, ctx.team0,
                    Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                    Ticket.TITLE, "Test"
            ));
        });

        txService.runInNewTx(() -> bcpService.edit(ticket, Map.of(
                Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        )));

        Thread.sleep(7000);
        retryTaskProcessor.processPendingTasksWithReset(queue);

        Assertions.assertEquals(TimerStatus.NOT_STARTED, ticket.getWaitForCallBackTimer().getStatus());
        Assertions.assertNull(ticket.getResponsibleEmployee());
    }

    @Test
    public void testPreviewProgressiveTimer_stopWhenCallIsActive() throws InterruptedException {
        final var ctx = txService.doInNewTx(ticketTestUtils::create);

        var ticket = new AtomicReference<>(txService.doInNewTx(() -> {
            final var service = ticketTestUtils.createService(
                    ctx.team0,
                    ctx.serviceTime24x7,
                    ctx.brand,
                    Optional.empty(),
                    OutboundTelephonyService.FQN,
                    Map.of(
                            OutboundTelephonyService.CALL_PLAY_TYPE, CallPlayType.PREVIEW_PROGRESSIVE,
                            OutboundTelephonyService.WAIT_FOR_CALL_TIME, "PT1S"
                    )
            );

            return bcpService.<TelephonyTicket>create(Fqn.of("ticket$outgoingCall"), Map.of(
                    Ticket.SERVICE, service,
                    Ticket.RESPONSIBLE_TEAM, ctx.team0,
                    Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                    Ticket.TITLE, "Test"
            ));
        }));

        ticket.set(txService.doInNewTx(() -> bcpService.edit(ticket.get(), Map.of(
                Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ))));

        Assertions.assertEquals(TimerStatus.ACTIVE, ticket.get().getWaitForCallBackTimer().getStatus());

        Thread.sleep(1000);

        ticket.set(txService.doInNewTx(() -> bcpService.edit(ticket.get(), Map.of(
                TelephonyTicket.CALL_ACTIVE, true
        ))));

        Assertions.assertEquals(TimerStatus.STOPPED, ticket.get().getWaitForCallBackTimer().getStatus());
    }
}
