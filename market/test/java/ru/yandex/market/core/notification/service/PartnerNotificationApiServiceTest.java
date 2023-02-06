package ru.yandex.market.core.notification.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.core.framework.composer.JDOMComposer;
import ru.yandex.market.core.framework.composer.JDOMConverter;
import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.core.notification.context.NotificationContext;
import ru.yandex.market.core.notification.context.impl.DualNotificationContext;
import ru.yandex.market.core.notification.context.impl.EmptyNotificationContext;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.context.impl.UidableNotificationContext;
import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;
import ru.yandex.market.partner.notification.client.model.SendNotificationResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartnerNotificationApiServiceTest {
    private final MbiDestinationResolver mbiDestinationResolver = new MbiDestinationResolver();
    private final PartnerNotificationClient partnerNotificationClient = mock(PartnerNotificationClient.class);
    private final NotificationService service = new PartnerNotificationApiService(
            partnerNotificationClient,
            mbiDestinationResolver,
            new JDOMComposer() {{
                setElementConverter(new JDOMConverter());
            }}
    );
    private final ArgumentCaptor<SendNotificationRequest> reqCaptor =
            ArgumentCaptor.forClass(SendNotificationRequest.class);

    private static final Long NN_TYPE_1 = 1L;
    private static final Long NN_TYPE_2 = 2L;
    private static final String STR_DATA = "string data";
    private static final ArrayListNotificationData<?> DATA =
            new ArrayListNotificationData<>(Collections.singletonList(STR_DATA));

    private static final String MAIL_TO = "test@yandex.ru";
    private static final String MAIL_FROM = "from@yandex.ru";

    private static final String SUBJECT = "test subject";
    private static final String BODY = "test body";
    private static final Long SHOP_ID = 666L;
    private static final Long USER_ID = 777L;
    private static final Long RESULT_GROUP_ID = 555L;

    private static final String ATTACHMENT_FILE_NAME = "file.txt";
    private static final String[] EMPTY_VALUES = {null, "", " "};

    @BeforeEach
    void setUp() {
        setUpClient(partnerNotificationClient, () -> RESULT_GROUP_ID);
    }

    public static void setUpClient(PartnerNotificationClient partnerNotificationClient) {
        var groupCounter = new AtomicLong();
        setUpClient(partnerNotificationClient, groupCounter::incrementAndGet);
    }

    public static void setUpClient(PartnerNotificationClient partnerNotificationClient, Supplier<Long> nextGroupId) {
        when(partnerNotificationClient.sendNotification(any()))
                .thenAnswer(invocation -> new SendNotificationResponse().groupId(nextGroupId.get()));
    }

    public static ArgumentCaptor<SendNotificationRequest> verifySentNotificationType(
            PartnerNotificationClient partnerNotificationClient,
            int times,
            long... notificationType
    ) {
        var types = LongStream.of(notificationType).boxed().toArray(Long[]::new);
        var reqCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient, times(Math.max(times, types.length))).sendNotification(reqCaptor.capture());
        assertThat(reqCaptor.getAllValues().stream().map(SendNotificationRequest::getTypeId)).containsOnly(types);
        return reqCaptor;
    }

    /**
     * Тест метода {@link NotificationService#send(int, List)}.
     */
    @Test
    void testSendByTypeAndData() {
        // Проверить первый тип.
        checkSend(
                service.send(NN_TYPE_1.intValue(), DATA),
                NN_TYPE_1, NotificationPriority.LOW, null
        );

        // Проверить второй тип.
        checkSend(
                service.send(NN_TYPE_2.intValue(), DATA),
                NN_TYPE_2, NotificationPriority.NORMAL, null
        );
    }


    /**
     * Тест метода {@link NotificationService#send(int, long, List)}.
     */
    @Test
    void testSendByTypeShopIdAndData() {
        // Проверить первый тип.
        checkSend(
                service.send(NN_TYPE_1.intValue(), SHOP_ID, DATA),
                NN_TYPE_1, NotificationPriority.LOW, SHOP_ID
        );

        // Проверить второй тип.
        checkSend(
                service.send(NN_TYPE_2.intValue(), SHOP_ID, DATA),
                NN_TYPE_2, NotificationPriority.NORMAL, SHOP_ID
        );
    }

    /**
     * Тест метода {@link NotificationService#send(int, List, NotificationContext)}.
     */
    @Test
    void testSendByTypeDataAndContext() {
        // Проверить первый тип с shop контекстом.
        checkSend(
                service.send(NN_TYPE_1.intValue(), DATA, new ShopNotificationContext(SHOP_ID)),
                NN_TYPE_1, NotificationPriority.LOW, SHOP_ID
        );

        // Проверить первый тип с empty контекстом.
        checkSend(
                service.send(NN_TYPE_1.intValue(), DATA, EmptyNotificationContext.INSTANCE),
                NN_TYPE_1, NotificationPriority.LOW, null
        );

        // Проверить второй тип с uid контекстом.
        checkSend(
                service.send(NN_TYPE_2.intValue(), DATA, new UidableNotificationContext(USER_ID)),
                NN_TYPE_2, NotificationPriority.NORMAL, null
        );

        // Проверить второй тип с dual контекстом.
        checkSend(
                service.send(NN_TYPE_2.intValue(), DATA, new DualNotificationContext(USER_ID, SHOP_ID)),
                NN_TYPE_2, NotificationPriority.NORMAL, SHOP_ID
        );
    }

    /**
     * Тест метода {@link NotificationService#send(int, List, MessageRecipients, NotificationContext)}.
     */
    @Test
    void testSendByTypeDataRecipientsAndContext() {
        MessageRecipients recipients = new MessageRecipients();
        recipients.setMailFrom(MAIL_FROM);
        recipients.setToAddressList(Collections.singletonList(MAIL_TO));

        // Проверить первый тип с shop контекстом.
        checkSend(
                service.send(NN_TYPE_1.intValue(), DATA, recipients, new ShopNotificationContext(SHOP_ID)),
                NN_TYPE_1, NotificationPriority.LOW, SHOP_ID
        );

        // Проверить первый тип с uid контекстом.
        checkSend(
                service.send(NN_TYPE_1.intValue(), DATA, recipients, new UidableNotificationContext(USER_ID)),
                NN_TYPE_1, NotificationPriority.LOW, null
        );

        // Проверить второй тип с dual контекстом.
        checkSend(
                service.send(NN_TYPE_2.intValue(), DATA, recipients, new DualNotificationContext(USER_ID, SHOP_ID)),
                NN_TYPE_2, NotificationPriority.NORMAL, SHOP_ID
        );

        // Проверить второй тип с empty контекстом.
        checkSend(
                service.send(NN_TYPE_2.intValue(), DATA, recipients, EmptyNotificationContext.INSTANCE),
                NN_TYPE_2, NotificationPriority.NORMAL, null
        );

    }

    /**
     * Тест метода {@link NotificationService#send(long, long, String, String, NotificationPriority)}.
     */
    @Test
    void testSendExplicitBodyAndPriority() {
        // Проверить первый тип.
        checkSend(
                service.send(NN_TYPE_1, SHOP_ID, SUBJECT, BODY, NotificationPriority.HIGH),
                NN_TYPE_1, NotificationPriority.HIGH, SHOP_ID
        );

        // Проверить второй тип.
        checkSend(
                service.send(NN_TYPE_2, SHOP_ID, SUBJECT, BODY, NotificationPriority.LOW),
                NN_TYPE_2, NotificationPriority.LOW, SHOP_ID
        );
    }

    @Test
    void testSendExplicitEmptySubject() {
        for (String emptyValue : EMPTY_VALUES) {
            try {
                fail(String.valueOf(service.send(NN_TYPE_2, SHOP_ID, emptyValue, BODY, NotificationPriority.LOW)));
            } catch (NotificationException ignored) {
            }
        }
    }

    @Test
    void testSendExplicitEmptyBody() {
        for (String emptyValue : EMPTY_VALUES) {
            try {
                fail(String.valueOf(service.send(NN_TYPE_2, SHOP_ID, SUBJECT, emptyValue, NotificationPriority.LOW)));
            } catch (NotificationException ignored) {
            }
        }
    }

    /**
     * Тест метода {@link NotificationService#send(int, String, long, String, String)}.
     */
    @Test
    void testSendExplicitBodyAndEmailTo() {
        // Проверить первый тип.
        checkSend(
                service.send(NN_TYPE_1.intValue(), MAIL_TO, SHOP_ID, SUBJECT, BODY),
                NN_TYPE_1, NotificationPriority.LOW, SHOP_ID
        );

        // Проверить второй тип.
        checkSend(
                service.send(NN_TYPE_2.intValue(), MAIL_TO, SHOP_ID, SUBJECT, BODY),
                NN_TYPE_2, NotificationPriority.NORMAL, SHOP_ID
        );
    }

    /**
     * Тест метода {@link NotificationService#sendAsAttachment(int, String, List, String)}.
     */
    @Test
    void testSendAttachment() {
        // Проверить первый тип.
        service.sendAsAttachment(NN_TYPE_1.intValue(), MAIL_TO, DATA, ATTACHMENT_FILE_NAME);
        checkSend(NN_TYPE_1, NotificationPriority.LOW, null);

        // Проверить второй тип.
        service.sendAsAttachment(NN_TYPE_2.intValue(), MAIL_TO, DATA, ATTACHMENT_FILE_NAME);
        checkSend(NN_TYPE_2, NotificationPriority.NORMAL, null);
    }

    private void checkSend(
            Long groupId,
            Long nnType,
            NotificationPriority priority,
            Long shopId
    ) {
        assertThat(groupId).isEqualTo(RESULT_GROUP_ID);
        checkSend(nnType, priority, shopId);
    }

    private void checkSend(
            Long nnType,
            NotificationPriority priority,
            Long shopId
    ) {
        verify(partnerNotificationClient, atLeastOnce()).sendNotification(reqCaptor.capture());
        var req = reqCaptor.getValue();
        assertThat(req.getData()).isNotEmpty();
        assertThat(req.getRenderOnly()).isFalse();
        assertThat(req.getTypeId()).isEqualTo(nnType);
        assertThat(req.getDestination().getShopId()).isEqualTo(shopId);
    }
}
