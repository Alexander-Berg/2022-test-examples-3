package ru.yandex.market.delivery.mdbapp.components.service.notification;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.notification.events.CapacityMessageDuplicatorEventListener;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CapacityCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.CapacityCounterRepository;
import ru.yandex.market.delivery.mdbapp.enums.PechkinMdbChannels;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TelegramNotificationServiceTest extends MockContextualTest {

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Autowired
    private CapacityCounterRepository capacityCounterRepository;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @Before
    public void before() {
        doNothing().when(pechkinHttpClient).sendMessage(any());
    }

    @After
    public void after() {
        reset(lmsClient);
        reset(pechkinHttpClient);
    }

    @Sql("/data/repository/notification/capacity_for_notification_50_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationHalfCapacity() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.FULFILLMENT)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.WAREHOUSE.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_90_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationLowCapacity() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.FULFILLMENT)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.WAREHOUSE.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_110_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationOverflowCapacity() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.FULFILLMENT)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.WAREHOUSE.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_110_percent_already_overflow.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationAlreadyOverflowCapacity() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.SORTING_CENTER)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.SORTING_CENTER.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_110_percent_already_overflow_resend.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationAlreadyOverflowCapacityResend() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.SORTING_CENTER)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.SORTING_CENTER.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_110_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationOverflowCapacityDelivery() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.DELIVERY)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.DELIVERY.getName()
        ));
    }

    @Sql("/data/repository/notification/capacity_for_notification_110_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationOverflowCapacityDropship() {
        when(lmsClient.getPartner(anyLong()))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.DROPSHIP)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);

        verifyPechkinMessages(List.of(
            CapacityMessageDuplicatorEventListener.CHANNEL,
            PechkinMdbChannels.DROPSHIP.getName()
        ));
    }

    protected void verifyPechkinMessages(List<String> channels) {
        ArgumentCaptor<MessageDto> captor = ArgumentCaptor.forClass(MessageDto.class);
        verify(pechkinHttpClient, times(channels.size())).sendMessage(captor.capture());
        var capturedChannels =
            captor.getAllValues()
                .stream()
                .map(MessageDto::getChannel)
                .collect(java.util.stream.Collectors.toList());

        softly.assertThat(capturedChannels).containsExactlyInAnyOrderElementsOf(channels);
    }

    @Sql("/data/repository/notification/capacity_for_notification_50_percent.sql")
    @Sql(value = "/data/repository/notification/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void testNotificationNotOverflowCapacityDeliveryNotSend() {
        when(lmsClient.getPartner(145L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(1L)
                .readableName("Партнер")
                .partnerType(PartnerType.DELIVERY)
                .build())
            );

        List<CapacityCounter> capacityCounters = capacityCounterRepository.findAll();

        telegramNotificationService.sendWarehouseWarnNotifications(capacityCounters);

        verify(lmsClient).getPartner(145L);
        verify(pechkinHttpClient, never()).sendMessage(any());
    }
}
