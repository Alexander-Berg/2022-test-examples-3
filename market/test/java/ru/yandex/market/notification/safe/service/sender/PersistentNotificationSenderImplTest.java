package ru.yandex.market.notification.safe.service.sender;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.notification.common.model.destination.ShopDestination;
import ru.yandex.market.notification.model.context.GroupedNotificationContext;
import ru.yandex.market.notification.model.transport.NotificationTransportType;
import ru.yandex.market.notification.safe.composer.PersistentNotificationComposer;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDestinationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationGroupDao;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.PersistentNotificationGroup;
import ru.yandex.market.notification.safe.model.PersistentNotificationResult;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.service.provider.SpamHashProvider;
import ru.yandex.market.notification.safe.service.sender.impl.PersistentNotificationSenderImpl;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.registry.NotificationFacadeRegistry;
import ru.yandex.market.notification.simple.model.context.GroupedNotificationContextImpl;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.simple.service.provider.complex.ComplexNotificationContentProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тетсы для {@link PersistentNotificationSenderImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationSenderImplTest {

    private static final long GROUP_ID = 2L;
    private static final long NOTIFICATION_ID = 3L;
    private static final CodeNotificationType NOTIFICATION_TYPE = new CodeNotificationType(1L);


    @Test
    public void testSend() {
        final NotificationFacadeRegistry registry = createRegistry();
        final PersistentNotificationFacade facade = createFacade();

        final Set<NotificationTransportType> transports = Collections.singleton(NotificationTransport.EMAIL);
        final GroupedNotificationContext context = new GroupedNotificationContextImpl(
            NOTIFICATION_TYPE,
            NotificationPriority.HIGH,
            transports,
            Collections.singleton(mock(ShopDestination.class)),
            Instant.now(),
            null,
            null
        );

        final PersistentNotificationSender sender = createPersistentNotificationSender(registry, facade);
        final PersistentNotificationResult result = sender.send(context);

        assertThat(result, notNullValue());
        assertThat(result.getGroupId(), equalTo(GROUP_ID));

        final Map<NotificationTransportType, Long> notifications = result.getNotifications();
        assertThat(notifications.values(), hasSize(transports.size()));
        assertThat(notifications.values().iterator().next(), equalTo(NOTIFICATION_ID));
    }


    private PersistentNotificationSender createPersistentNotificationSender(
        final NotificationFacadeRegistry registry, final PersistentNotificationFacade facade
    ) {
        return new PersistentNotificationSenderImpl(registry, facade) {
            @Nonnull
            @Override
            protected ComplexNotificationContentProvider createContentProvider(@Nonnull final NotificationFacade f) {
                return mock(ComplexNotificationContentProvider.class);
            }
        };
    }

    private NotificationFacadeRegistry createRegistry() {
        return mock(NotificationFacadeRegistry.class);
    }

    private PersistentNotificationFacade createFacade() {
        final PersistentDaoFacade daoFacade = createDaoFacade();
        final PersistentNotificationFacade facade = mock(PersistentNotificationFacade.class);

        when(facade.getPersistentNotificationComposer()).thenReturn(mock(PersistentNotificationComposer.class));
        when(facade.getSpamHashProvider()).thenReturn(mock(SpamHashProvider.class));
        when(facade.getPersistentDaoFacade()).thenReturn(daoFacade);

        return facade;
    }

    private PersistentDaoFacade createDaoFacade() {
        final PersistentNotificationGroupDao groupDao = createGroupDao();
        final PersistentNotificationDao notificationDao = createNotificationDao();
        final PersistentNotificationDestinationDao destinationDao = createDestinationDao();
        final PersistentDaoFacade daoFacade = mock(PersistentDaoFacade.class);
        final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        when(daoFacade.getNotificationGroupDao()).thenReturn(groupDao);
        when(daoFacade.getNotificationDao()).thenReturn(notificationDao);
        when(daoFacade.getDestinationDao()).thenReturn(destinationDao);
        when(daoFacade.getTransactionTemplate()).thenReturn(transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            final TransactionCallback<Object> callback = (TransactionCallback<Object>) invocation.getArguments()[0];
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        return daoFacade;
    }

    private PersistentNotificationDao createNotificationDao() {
        final PersistentNotificationDao notificationDao = mock(PersistentNotificationDao.class);
        final PersistentNotification notification = new PersistentNotification(
            NOTIFICATION_ID, GROUP_ID, NOTIFICATION_TYPE, NotificationStatus.NEW, Instant.now(),
            mock(PersistentBinaryData.class), mock(PersistentDeliveryData.class), ""
        );

        when(notificationDao.create(any())).thenReturn(Collections.singleton(notification));

        return notificationDao;
    }

    private PersistentNotificationDestinationDao createDestinationDao() {
        final PersistentNotificationDestinationDao destinationDao = mock(PersistentNotificationDestinationDao.class);
        final PersistentNotificationDestination destination = mock(PersistentNotificationDestination.class);

        when(destinationDao.create(any())).thenReturn(Collections.singleton(destination));

        return destinationDao;
    }

    private PersistentNotificationGroupDao createGroupDao() {
        final PersistentNotificationGroupDao groupDao = mock(PersistentNotificationGroupDao.class);
        final PersistentBinaryData groupData = new PersistentBinaryData(new byte[]{}, "");
        final PersistentNotificationGroup group = new PersistentNotificationGroup(GROUP_ID, groupData);

        when(groupDao.create(any())).thenReturn(group);

        return groupDao;
    }

}
