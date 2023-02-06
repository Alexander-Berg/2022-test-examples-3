package ru.yandex.market.pers.notify.ems;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         28.06.17
 */
public class ReceiptMailProcessorTest extends MarketMailerMockedDbTest {
    @Autowired
    @Qualifier("receiptMailProcessor")
    private NotificationProcessor receiptMailProcessor;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private CheckouterService checkouterService;

    private static final long ORDER_ID = 4536456L;
    private static final long RECEIPT_ID = 985479435L;

    @Test
    public void process() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valter@yandex-team.ru", NotificationSubtype.ORDER_PAID_RECEIPT_PRINTED)
            .setSourceId(ORDER_ID)
            .addDataParam(NotificationEventDataName.RECEIPT_ID, String.valueOf(RECEIPT_ID))
            .build()
        );

        when(checkouterService.getOrderReceipts(anyLong(), any(ClientRole.class), any(Long.class), any(Long.class)))
            .thenReturn(new Receipts(Collections.emptyList()));
        receiptMailProcessor.process();

        NotificationEvent notSentEvent = mailerNotificationEventService.getEvent(event.getId());
        assertEquals(NotificationEventStatus.REJECTED_NO_ATTACHMENT_DATA, notSentEvent.getStatus());
    }


    @Test
    public void process2() {

        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valter@yandex-team.ru", NotificationSubtype.ORDER_PAID_RECEIPT_PRINTED)
            .setSourceId(ORDER_ID)
            .addDataParam(NotificationEventDataName.RECEIPT_ID, String.valueOf(RECEIPT_ID))
            .build()
        );

        Receipt receipt = new Receipt();
        receipt.setId(RECEIPT_ID);
        receipt.setType(ReceiptType.INCOME);
        when(checkouterService.getOrderReceipts(anyLong(), any(ClientRole.class), any(Long.class), any(Long.class)))
            .thenReturn(new Receipts(
                Collections.singletonList(receipt)
            ));
        receiptMailProcessor.process();

        NotificationEvent sentEvent = mailerNotificationEventService.getEvent(event.getId());
        assertEquals(NotificationEventStatus.SENT, sentEvent.getStatus());
    }
}
