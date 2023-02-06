package ru.yandex.market.mboc.tms.executors.notifications;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.model.data.NotificationData;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.CategoryOffersNeedActionData;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.services.mail.EmailService;
import ru.yandex.market.mboc.common.services.mbi.MbiApiService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class NotificationsSenderExecutorTest extends BaseDbTestClass {

    @Autowired
    private NotificationRepository repository;
    @Autowired
    private SupplierRepository supplierRepository;

    private NotificationsSenderExecutor senderExecutor;
    private EmailService emailServiceMock;
    private MbiApiService mbiApiServiceMock;

    @Before
    public void setUp() {
        emailServiceMock = Mockito.mock(EmailService.class);
        mbiApiServiceMock = Mockito.mock(MbiApiService.class);

        senderExecutor = new NotificationsSenderExecutor(repository, supplierRepository, emailServiceMock,
            mbiApiServiceMock,
            Collections.singletonList("test-to@email.test"),
            Collections.singletonList("content-to@email.test"),
            Collections.emptyList());
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("notifications/tms-suppliers.yml"));
    }

    @Test
    public void testUpdateDbNotifications() throws Exception {
        Mockito.when(emailServiceMock.mailCatMansAboutNewOffers(Mockito.anyList(),
            Mockito.any(NewOffersData.class))).thenReturn(true);
        Mockito.when(emailServiceMock.mailCatMansAboutCategoryOffersNeedAction(Mockito.anyList(),
            Mockito.any(CategoryOffersNeedActionData.class))).thenReturn(true);
        Mockito.when(mbiApiServiceMock.sendProcessedOffersNotificationToSupplier(Mockito.anyLong(), Mockito.anyInt(),
            Mockito.any(NotificationData.class))).thenReturn(true);
        List<Notification> notifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-notifications-sender.yml");
        repository.insertBatch(notifications);

        senderExecutor.execute();

        notifications.stream()
            .filter(notification -> notification.getStatus() == Notification.Status.NEW)
            .forEach(notification -> {
                Notification byId = repository.findById(notification.getId());
                Assertions.assertThat(byId.getStatus()).isEqualTo(Notification.Status.SENT);
                Assertions.assertThat(byId.getStatusTs()).isAfter(notification.getStatusTs());
            });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testFailedToUpdateNotification() throws Exception {
        Mockito.when(emailServiceMock.mailCatMansAboutNewOffers(Mockito.anyList(),
            Mockito.any(NewOffersData.class))).thenReturn(false);
        Mockito.when(emailServiceMock.mailCatMansAboutCategoryOffersNeedAction(Mockito.anyList(),
            Mockito.any(CategoryOffersNeedActionData.class))).thenReturn(false);
        Mockito.when(mbiApiServiceMock.sendProcessedOffersNotificationToSupplier(Mockito.anyLong(), Mockito.anyInt(),
            Mockito.any(NotificationData.class))).thenReturn(false);
        List<Notification> notifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-notifications-sender.yml");
        notifications.forEach(n -> n.setStatusTsInternal(n.getStatusTs().withNano(0)));
        repository.insertBatch(notifications);

        senderExecutor.execute();

        notifications.stream()
            .filter(notification -> notification.getStatus() == Notification.Status.NEW)
            .forEach(notification -> {
                Notification byId = repository.findById(notification.getId());
                if (Arrays.asList(Notification.NotificationType.CAT_MAN_NEW_OFFERS,
                    Notification.NotificationType.CAT_MAN_OFFERS_NEED_ACTION,
                    Notification.NotificationType.SUPPLIER_OFFERS_PROCESSED).contains(byId.getNotificationType()) &&
                    (notification.getSupplierId() == null || notification.getSupplierId() != 6)) {
                    Assertions.assertThat(byId.getStatus()).isEqualTo(Notification.Status.NEW);
                    Assertions.assertThat(byId.getStatusTs()).isEqualTo(notification.getStatusTs());
                } else {
                    Assertions.assertThat(byId.getStatus()).isEqualTo(Notification.Status.SENT);
                    Assertions.assertThat(byId.getStatusTs()).isAfter(notification.getStatusTs());
                }
            });
    }

    @Test
    public void testSkipTestSuppliers() {
        final int testSupplier = 6;
        Supplier supplier = supplierRepository.findById(testSupplier);
        supplierRepository.update(supplier.setTestSupplier(true));

        Mockito.when(emailServiceMock.mailCatMansAboutNewOffers(Mockito.anyList(),
            Mockito.any(NewOffersData.class))).thenReturn(true);
        Mockito.when(emailServiceMock.mailCatMansAboutCategoryOffersNeedAction(Mockito.anyList(),
            Mockito.any(CategoryOffersNeedActionData.class))).thenReturn(true);
        Mockito.when(mbiApiServiceMock.sendProcessedOffersNotificationToSupplier(Mockito.anyLong(), Mockito.anyInt(),
            Mockito.any(NotificationData.class))).thenAnswer(invoc -> {
            int supplierId = invoc.getArgument(1);
            if (supplierId == testSupplier) {
                throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, new byte[0],
                    Charset.defaultCharset());
            }
            return true;
        });

        List<Notification> notifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-notifications-sender.yml");
        repository.insertBatch(notifications);

        senderExecutor.execute();

        notifications.stream()
            .filter(notification -> notification.getStatus() == Notification.Status.NEW)
            .forEach(notification -> {
                Notification byId = repository.findById(notification.getId());
                Assertions.assertThat(byId.getStatus()).isEqualTo(Notification.Status.SENT);
                Assertions.assertThat(byId.getStatusTs()).isAfter(notification.getStatusTs());
            });
    }
}
