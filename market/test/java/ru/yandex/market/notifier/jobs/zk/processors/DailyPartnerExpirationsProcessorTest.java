package ru.yandex.market.notifier.jobs.zk.processors;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.criteria.EvictionSearch;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.entity.NotifierProperties;
import ru.yandex.market.notifier.orderservice.MbiOrderServiceClient;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.orderservice.client.model.CommonCountResponse;
import ru.yandex.market.orderservice.client.model.DailyDbsExpirationsPagedResponse;
import ru.yandex.market.orderservice.client.model.PartnerExpirationsDto;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DailyPartnerExpirationsProcessorTest extends AbstractWebTestBase {

    @Mock
    MbiOrderServiceClient mbiOrderServiceClient;

    @Autowired
    InboxService inboxService;

    @Autowired
    NotifierProperties notifierProperties;

    @BeforeEach
    public void before() {
        RequestContextHolder.createNewContext();
        notifierProperties.setDailyPartnerExpirationsNotificationEnabled(true);
    }

    @Test
    public void testBatchOfPartners() {
        DailyPartnerExpirationsProcessor dailyPartnerExpirationsProcessor = new DailyPartnerExpirationsProcessor(
                mbiOrderServiceClient,
                inboxService,
                notifierProperties,
                1,
                2
        );

        List<PartnerExpirationsDto> orderServiceData = List.of(
                createPartnerExpirationsDto(1L, 10L, 1L, 0L, 0L, 0L),
                createPartnerExpirationsDto(2L, 8L, 0L, 2L, 3L, 4L),
                createPartnerExpirationsDto(3L, 0L, 0L, 1L, 2L, 3L),
                createPartnerExpirationsDto(4L, 5L, 4L, 3L, 2L, 1L),
                createPartnerExpirationsDto(5L, 10L, 0L, 0L, 0L, 0L)
        );

        initializeMocks(orderServiceData);

        dailyPartnerExpirationsProcessor.processPartnerExpirations();

        verify(mbiOrderServiceClient, times(1)).getPartnersWithExpirationsCount();
        verify(mbiOrderServiceClient, times(3)).getDailyDbsExpirations(anyInt(), anyInt());

        verifyNoMoreInteractions(mbiOrderServiceClient);

        EvictionSearch es = new EvictionSearch(NotificationStatus.NEW, DailyPartnerExpirationsProcessor.DAILY_DBS_EXPIRATIONS_NOTIFICATION, null);
        List<Notification> actualNotifications = inboxService.evictionSearch(es).stream().sorted(Comparator.comparing(Notification::getId)).collect(Collectors.toList());

        assertEquals(5, actualNotifications.size());

        for (int i = 0; i < orderServiceData.size(); ++i) {
            assertExpirations(
                    actualNotifications.get(i),
                    orderServiceData.get(i).getPartnerId(),
                    orderServiceData.get(i).getTotalDeliveryExpirations(),
                    orderServiceData.get(i).getMarketPickupExpirations(),
                    orderServiceData.get(i).getStoragePeriodExpirations(),
                    orderServiceData.get(i).getNonFinalStatusesOrdersCount(),
                    orderServiceData.get(i).getDropOffDeliveryExpirations()
            );
        }
    }

    private void initializeMocks(List<PartnerExpirationsDto> orderServiceData) {
        when(mbiOrderServiceClient.getPartnersWithExpirationsCount())
                .thenReturn(new CommonCountResponse().count(5L));

        when(mbiOrderServiceClient.getDailyDbsExpirations(0, 2)).thenReturn(
                new DailyDbsExpirationsPagedResponse()
                        .addPartnerExpirationsItem(orderServiceData.get(0))
                        .addPartnerExpirationsItem(orderServiceData.get(1))
        );

        when(mbiOrderServiceClient.getDailyDbsExpirations(1, 2)).thenReturn(
                new DailyDbsExpirationsPagedResponse()
                        .addPartnerExpirationsItem(orderServiceData.get(2))
                        .addPartnerExpirationsItem(orderServiceData.get(3))
        );

        when(mbiOrderServiceClient.getDailyDbsExpirations(2, 2)).thenReturn(
                new DailyDbsExpirationsPagedResponse()
                        .addPartnerExpirationsItem(orderServiceData.get(4))
        );
    }

    private void assertExpirations(Notification note, Long partnerId, long totalDeliveryExpirations,
                                   long marketPickupExpirations, long storagePeriodExpirations,
                                   long nonFinalStatusesCount, long dropoffDeliveryExpirations) {
        assertEquals(note.getDeliveryChannels().get(0).getAddress(), partnerId.toString());

        assertEquals(note.getData(),
                String.format(
                        "<expirations>\n" +
                                "  <total-delivery-expirations>%s</total-delivery-expirations>\n" +
                                "  <market-pickup-expirations>%s</market-pickup-expirations>\n" +
                                "  <storage-period-expirations>%s</storage-period-expirations>\n" +
                                "  <non-final-statuses-orders-count>%s</non-final-statuses-orders-count>\n" +
                                "  <drop-off-delivery-expirations>%s</drop-off-delivery-expirations>\n" +
                                "</expirations>",
                        totalDeliveryExpirations, marketPickupExpirations,
                        storagePeriodExpirations, nonFinalStatusesCount, dropoffDeliveryExpirations)
        );
    }

    private PartnerExpirationsDto createPartnerExpirationsDto(long partnerId, long totalDeliveryExpirations,
                                                              long marketPickupExpirations, long storagePeriodExpirations,
                                                              long nonFinalStatusesCount, long dropoffDeliveryExpirations) {
        return new PartnerExpirationsDto()
                .partnerId(partnerId)
                .totalDeliveryExpirations(totalDeliveryExpirations)
                .marketPickupExpirations(marketPickupExpirations)
                .storagePeriodExpirations(storagePeriodExpirations)
                .nonFinalStatusesOrdersCount(nonFinalStatusesCount)
                .dropOffDeliveryExpirations(dropoffDeliveryExpirations);
    }

    @Test
    public void testNoPartnersReturned() {
        DailyPartnerExpirationsProcessor dailyPartnerExpirationsProcessor = new DailyPartnerExpirationsProcessor(
                mbiOrderServiceClient,
                inboxService,
                notifierProperties,
                1,
                2
        );

        when(mbiOrderServiceClient.getPartnersWithExpirationsCount())
                .thenReturn(new CommonCountResponse().count(0L));

        dailyPartnerExpirationsProcessor.processPartnerExpirations();

        verify(mbiOrderServiceClient, times(1)).getPartnersWithExpirationsCount();
        verify(mbiOrderServiceClient, times(0)).getDailyDbsExpirations(anyInt(), anyInt());

        verifyNoMoreInteractions(mbiOrderServiceClient);
    }
}
