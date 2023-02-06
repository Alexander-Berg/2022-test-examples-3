package ru.yandex.market.returns;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.notification.history.service.PartnerLastNotificationService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.order.returns.OrderReturnDao;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PartnerUnredeemedReturnsNotificationExecutorTest extends FunctionalTest {

    @Autowired
    private OrderCheckpointDao orderCheckpointDao;

    @Autowired
    private OrderReturnDao orderReturnDao;

    @Autowired
    private PartnerLastNotificationService partnerLastNotificationService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Mock
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private PartnerUnredeemedReturnsNotificationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new PartnerUnredeemedReturnsNotificationExecutor(
                orderCheckpointDao,
                partnerLastNotificationService,
                campaignService,
                supplierService,
                notificationService,
                transactionTemplate,
                Clock.fixed(LocalDateTime.of(2020, 10, 27, 6, 0, 0)
                        .toInstant(OffsetDateTime.now().getOffset()), ZoneOffset.systemDefault()),
                orderReturnDao,
                partnerTypeAwareService
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerUnredeemedReturnsNotificationExecutorTest.before.csv")
    void testDoJob() {
        NamedContainer unredeemedContainer606 = new NamedContainer("unredeemed", 2);
        NamedContainer unredeemedContainer707 = new NamedContainer("unredeemed", 1);
        NamedContainer returnedContainer707 = new NamedContainer("returned", 2);
        NamedContainer returnedContainer808 = new NamedContainer("returned", 1);
        NamedContainer unredeemedContainer909 = new NamedContainer("unredeemed", 1);

        executor.doJob(null);
        ArgumentCaptor<NotificationSendContext> contextCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);

        Mockito.verify(notificationService, Mockito.times(4)).send(contextCaptor.capture());

        var sentNotifications = contextCaptor.getAllValues();
        Assertions.assertThat(sentNotifications).hasSize(4);

        var notifiedShops = sentNotifications.stream()
                .map(NotificationSendContext::getShopId)
                .collect(Collectors.toList());
        Assertions.assertThat(notifiedShops).containsExactlyInAnyOrder(606L, 707L, 808L, 909L);

        var data606 = sentNotifications.stream()
                .filter(notification -> notification.getShopId() == 606)
                .map(NotificationSendContext::getData)
                .findAny().orElse(null);
        Assertions.assertThat(data606).isNotNull().contains(unredeemedContainer606);

        var data707 = sentNotifications.stream()
                .filter(notification -> notification.getShopId() == 707)
                .map(NotificationSendContext::getData)
                .findAny().orElse(null);
        Assertions.assertThat(data707).isNotNull().containsAll(List.of(unredeemedContainer707, returnedContainer707));

        var data808 = sentNotifications.stream()
                .filter(notification -> notification.getShopId() == 808)
                .map(NotificationSendContext::getData)
                .findAny().orElse(null);
        Assertions.assertThat(data808).isNotNull().contains(returnedContainer808);

        assertFalse(sentNotifications.stream()
                .filter(notification -> notification.getShopId() == 908)
                .map(NotificationSendContext::getData)
                .findAny().isPresent());

        var data909 = sentNotifications.stream()
                .filter(notification -> notification.getShopId() == 909)
                .map(NotificationSendContext::getData)
                .findAny().orElse(null);
        Assertions.assertThat(data909).isNotNull().contains(unredeemedContainer909);
    }

    @Test
    @DbUnitDataSet(before = "PartnerUnredeemedReturnsNotificationExecutorTest.closed.before.csv")
    void testClosedCampaigns() {
        NamedContainer returnedContainer808 = new NamedContainer("returned", 1);
        NamedContainer unredeemedContainer909 = new NamedContainer("unredeemed", 1);

        executor.doJob(null);

        Mockito.verify(notificationService, Mockito.never()).send(any());
    }
}
