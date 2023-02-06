package ru.yandex.market.notifier.test.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.notifier.util.providers.ReceiptProvider;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.RECEIPT_PRINTED;
import static ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder.anOrderHistoryEvent;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_ORDER_CREATION_DATE;

/**
 * @author musachev
 */
public class ReceiptPrintedTest extends AbstractServicesTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private InboxService inboxService;
    @Autowired
    private InboxDao inboxDao;
    @Autowired
    private PersNotifyClient persNotifyClient;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(new Object[][]{
                        {
                                Color.BLUE,
                                RECEIPT_PRINTED,
                                ReceiptType.INCOME,
                                true
                        },
                        {
                                Color.BLUE,
                                RECEIPT_PRINTED,
                                ReceiptType.INCOME_RETURN,
                                true
                        },
                        {
                                Color.BLUE,
                                RECEIPT_PRINTED,
                                ReceiptType.OFFSET_ADVANCE_ON_DELIVERED,
                                true
                        }
                }
        ).map(Arguments::of);
    }

    /**
     * Проверяем отправку емейлов пользователю для двух типов чеков:
     * • INCOME
     * • INCOME_RETURN.
     */
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void test(Color rgb, HistoryEventType historyEventType, ReceiptType receiptType,
                     boolean shouldEmailToBeSend) throws PersNotifyClientException {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.setRgb(rgb);
        order.getDelivery().setValidFeatures(null);

        OrderHistoryEvent event = anOrderHistoryEvent()
                .withEventType(RECEIPT_PRINTED)
                .withOrder(order)
                .withClientInfo(new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID))
                .build();
        event.setReceipt(ReceiptProvider.getReceipt(receiptType));
        eventTestUtils.mockEvents(Collections.singletonList(event));

        eventTestUtils.runImport();

        List<Notification> found = inboxDao.getAllNotifications();

        /*
         * Для красных создаем нотификацию на каждый евент.
         */
        if (rgb != Color.RED) {
            Assertions.assertEquals(shouldEmailToBeSend ? 1 : 0, inboxService.getDeliveryStatisticsFull().size());
            Assertions.assertEquals(shouldEmailToBeSend ? 1 : 0, found.size());
        }


        if (shouldEmailToBeSend) {
            Notification notification = found.get(0);
            assertThat(notification.getDeliveryChannels(), contains(
                    hasProperty("type", is(ChannelType.EMAIL))
            ));
            deliverNotifications();

            Mockito.verify(persNotifyClient).createEvent(MockitoHamcrest.argThat(allOf(
                    instanceOf(NotificationEventSource.class),
                    hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail()))
            )));
        }

    }

    private void deliverNotifications() {
        eventTestUtils.deliverNotifications();
    }
}
