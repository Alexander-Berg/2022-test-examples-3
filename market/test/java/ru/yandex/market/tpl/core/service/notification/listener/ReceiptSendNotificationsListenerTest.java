package ru.yandex.market.tpl.core.service.notification.listener;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.receipt.FiscalDataDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationType;
import ru.yandex.market.tpl.common.communication.crm.model.CommunicationEventType;
import ru.yandex.market.tpl.common.communication.crm.service.CommunicationSender;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDtoMapper;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptFiscalData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptNotification;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptService;
import ru.yandex.market.tpl.core.domain.receipt.events.ReceiptSendNotificationsEvent;
import ru.yandex.market.tpl.core.service.crm.communication.model.CourierPlatformCommunicationDto;
import ru.yandex.market.tpl.core.service.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author valter
 */
@ExtendWith(MockitoExtension.class)
class ReceiptSendNotificationsListenerTest {

    private static final String EMPTY_EMAIL = " ";
    private static final String LO_PREFIX = "LO-";

    @InjectMocks
    private ReceiptSendNotificationsListener listener;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private ReceiptDtoMapper receiptDtoMapper;

    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @Mock
    private CommunicationSender<CourierPlatformCommunicationDto> asyncCommunicationSender;

    @Mock
    private Clock clock;

    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now();
        lenient().doReturn(now).when(clock).instant();
    }

    @Test
    void sendSmsTheOldWay() {
        var phone = "+1234567890";
        var notificationId = 34758L;

        var fiscalData = mockFiscalData();
        var fiscalDataDto = mock(FiscalDataDto.class);
        doReturn(fiscalDataDto).when(receiptDtoMapper).mapToDto(fiscalData);

        var notification = mockNotification(notificationId, phone, null, ReceiptNotificationType.SMS,
                fiscalData.getReceiptData());

        var order = mockLoOrder("LO-55555");
        when(receiptService.findOrderByReceiptData(notification.getReceiptData())).thenReturn(Optional.of(order));

        listener.sendReceiptNotifications(new ReceiptSendNotificationsEvent(
                List.of(notification),
                fiscalData
        ));

        verify(receiptService).markNotificationInQueue(eq(notificationId));
        verify(notificationService).sendReceiptCommunicationAsync(
                eq(ReceiptDataType.INCOME),
                eq(fiscalDataDto),
                eq(notificationId)
        );
    }

    @Test
    void sendSmsTriggerPlatform() {
        var notificationId = 34758L;

        var fiscalData = mockFiscalData();
        var notification = mockNotification(notificationId, "+1234567890", null,
                ReceiptNotificationType.SMS, fiscalData.getReceiptData());

        var order = mockOrder();
        var receiptData = notification.getReceiptData();
        when(receiptService.findOrderByReceiptData(receiptData)).thenReturn(Optional.of(order));

        listener.sendReceiptNotifications(new ReceiptSendNotificationsEvent(
                List.of(notification),
                fiscalData
        ));

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        checkEvent((CourierPlatformCommunicationDto.ReceiptEvent) eventCaptor.getValue(), notification, order,
                fiscalData);
        verify(receiptService).markNotificationSent(eq(notificationId), eq(now));
    }

    @Test
    void sendEmailTriggerPlatform() {
        var notificationId = 34758L;
        doReturn(true)
                .when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.RECEIPT_EMAIL_NOTIFICATION_ENABLED);

        var fiscalData = mockFiscalData();
        var notification = mockNotification(34758L, "+123456789",
                "test@yandex.ru", ReceiptNotificationType.EMAIL, fiscalData.getReceiptData());

        var order = mockOrder();
        var receiptData = notification.getReceiptData();
        when(receiptService.findOrderByReceiptData(receiptData)).thenReturn(Optional.of(order));

        listener.sendReceiptNotifications(new ReceiptSendNotificationsEvent(
                List.of(notification),
                fiscalData
        ));

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        checkEvent((CourierPlatformCommunicationDto.ReceiptEvent) eventCaptor.getValue(), notification, order,
                fiscalData);
        verify(receiptService).markNotificationSent(eq(notificationId), eq(now));
    }

    @Test
    void sendSmsWithEmptyEmailTriggerPlatformTest() {
        var notificationId = 34758L;
        doReturn(true)
                .when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.RECEIPT_EMAIL_NOTIFICATION_ENABLED);

        var fiscalData = mockFiscalData();
        var notification = mockNotification(34758L, "+123456789",
                EMPTY_EMAIL, ReceiptNotificationType.EMAIL, fiscalData.getReceiptData());

        var order = mockOrder();
        var receiptData = notification.getReceiptData();
        when(receiptService.findOrderByReceiptData(receiptData)).thenReturn(Optional.of(order));

        listener.sendReceiptNotifications(new ReceiptSendNotificationsEvent(
                List.of(notification),
                fiscalData
        ));

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        checkEvent((CourierPlatformCommunicationDto.ReceiptEvent) eventCaptor.getValue(), notification, order,
                fiscalData);
        verify(receiptService).markNotificationSent(eq(notificationId), eq(now));
    }

    private void checkEvent(CourierPlatformCommunicationDto.ReceiptEvent event, ReceiptNotification notification,
                            Order order, ReceiptFiscalData fiscalData) {
        assertThat(event.getEventType()).isEqualTo(CommunicationEventType.RECEIPT_EVENT);
        assertThat(event.getReceiptType()).isEqualTo(ReceiptDataType.INCOME.name());
        assertThat(event.getReceiptDate()).isEqualTo(fiscalData.getDt());
        assertThat(event.getReceiptTotalSum()).isEqualTo(fiscalData.getTotal());
        assertThat(event.getReceiptOfdUrl()).isEqualTo(fiscalData.getOfdUrl());
        assertThat(event.getRecipientYandexUid()).isEqualTo(order.getBuyerYandexUid());
        assertThat(event.getRecipientPhone()).isEqualTo(notification.getPhone());
        if (notification.getType() == ReceiptNotificationType.EMAIL && !EMPTY_EMAIL.equals(notification.getEmail())) {
            assertThat(event.getRecipientEmail()).isEqualTo(notification.getEmail());
        } else {
            assertThat(event.getRecipientEmail()).isNull();
        }
        assertThat(event.getYandexOrderIds()).hasSize(1);
        assertThat(event.getYandexOrderIds()).contains(order.getExternalOrderId());
    }

    private ReceiptFiscalData mockFiscalData() {
        var receiptData = mock(ReceiptData.class);
        when(receiptData.getType()).thenReturn(ReceiptDataType.INCOME);
        var fiscalData = mock(ReceiptFiscalData.class);
        when(fiscalData.getReceiptData()).thenReturn(receiptData);
        lenient().doReturn(LocalDateTime.of(2000, 1, 1, 14, 30)).when(fiscalData).getDt();
        lenient().doReturn(BigDecimal.valueOf(234.56)).when(fiscalData).getTotal();
        return fiscalData;
    }

    private ReceiptNotification mockNotification(long notificationId, String phone,
                                                 String email, ReceiptNotificationType type,
                                                 ReceiptData receiptData) {
        var notification = mock(ReceiptNotification.class);
        when(notification.getId()).thenReturn(notificationId);
        when(notification.getType()).thenReturn(type);
        when(notification.getStatus()).thenReturn(ReceiptNotificationStatus.IN_PROGRESS);
        lenient().doReturn(phone).when(notification).getPhone();
        lenient().doReturn(email).when(notification).getEmail();
        lenient().doReturn(receiptData).when(notification).getReceiptData();
        if (type == ReceiptNotificationType.EMAIL) {
            lenient().doReturn(email).when(notification).getAddress();
        } else {
            lenient().doReturn(phone).when(notification).getAddress();
        }
        return notification;
    }

    private Order mockOrder() {
        var order = mock(Order.class);
        when(order.getExternalOrderId()).thenReturn("55555577");
        when(order.getBuyerYandexUid()).thenReturn(22223344L);
        return order;
    }

    private Order mockLoOrder(String externalOrderId) {
        var order = mock(Order.class);
        when(order.orderExternalIdIsLo()).thenReturn(externalOrderId.contains(LO_PREFIX));
        return order;
    }

}
