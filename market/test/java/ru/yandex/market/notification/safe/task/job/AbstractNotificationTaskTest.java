package ru.yandex.market.notification.safe.task.job;

import java.time.Instant;
import java.util.Collections;

import org.junit.Ignore;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.notification.safe.composer.NotificationComposer;
import ru.yandex.market.notification.safe.composer.PersistentNotificationComposer;
import ru.yandex.market.notification.safe.dao.PersistentNotificationAddressDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDestinationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationGroupDao;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.service.provider.SpamFilterProvider;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Базовый класс для unit-тестов {@link ru.yandex.market.notification.service.task.job.NotificationTask}.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class AbstractNotificationTaskTest {

    PersistentNotificationFacade createFacade(final PersistentDaoFacade daoFacade) {
        final PersistentNotificationFacade facade = mock(PersistentNotificationFacade.class);
        final SpamFilterProvider spamFilterProvider = mock(SpamFilterProvider.class);
        final NotificationComposer composer = mock(NotificationComposer.class);
        final PersistentNotificationComposer persistentComposer = mock(PersistentNotificationComposer.class);
        final PersistentNotificationAddress address = mock(PersistentNotificationAddress.class);

        when(persistentComposer.composeAddress(anyLong(), anyLong(), any())).thenReturn(address);
        when(facade.getSpamFilterProvider()).thenReturn(spamFilterProvider);
        when(facade.getPersistentDaoFacade()).thenReturn(daoFacade);
        when(facade.getNotificationComposer()).thenReturn(composer);
        when(facade.getPersistentNotificationComposer()).thenReturn(persistentComposer);

        return facade;
    }

    PersistentDaoFacade createPersistentDaoFacade() {
        final PersistentDaoFacade daoFacade = mock(PersistentDaoFacade.class);
        final PersistentNotificationGroupDao groupDao = mock(PersistentNotificationGroupDao.class);
        final PersistentNotificationDao notificationDao = mock(PersistentNotificationDao.class);
        final PersistentNotificationDestinationDao destinationDao = mock(PersistentNotificationDestinationDao.class);
        final PersistentNotificationAddressDao addressDao = mock(PersistentNotificationAddressDao.class);
        final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        when(addressDao.create(any())).thenReturn(Collections.singleton(mock(PersistentNotificationAddress.class)));
        when(daoFacade.getNotificationGroupDao()).thenReturn(groupDao);
        when(daoFacade.getNotificationDao()).thenReturn(notificationDao);
        when(daoFacade.getDestinationDao()).thenReturn(destinationDao);
        when(daoFacade.getAddressDao()).thenReturn(addressDao);
        when(daoFacade.getTransactionTemplate()).thenReturn(transactionTemplate);

        return daoFacade;
    }

    PersistentNotification createNotification(final NotificationStatus status) {
        final Instant now = Instant.now();
        final CodeNotificationType type = new CodeNotificationType(3L);
        final PersistentBinaryData binaryData = mock(PersistentBinaryData.class);
        final PersistentDeliveryData deliveryData = new PersistentDeliveryData(
            NotificationTransport.EMAIL, NotificationPriority.NORMAL, now, now, 3
        );

        return new PersistentNotification(1L, 2L, type, status, now, binaryData, deliveryData, "");
    }

    PersistentNotificationDestination createDestination() {
        final PersistentBinaryData binaryData = mock(PersistentBinaryData.class);
        return new PersistentNotificationDestination(1L, 1L, binaryData);
    }

}
