package ru.yandex.market.notification.safe.task.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.notification.common.service.composer.NoChangeAddressComposer;
import ru.yandex.market.notification.exception.address.AddressException;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.type.NotificationSpamStatus;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.task.job.misc.QuadraticRetryTimeCalculator;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.composer.NotificationAddressComposer;
import ru.yandex.market.notification.service.provider.NotificationAddressProvider;
import ru.yandex.market.notification.service.registry.NotificationAddressResolverRegistry;
import ru.yandex.market.notification.service.registry.NotificationFacadeRegistry;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link PreparePersistentNotificationTask}.
 *
 * @author Vladislav Bauer
 */
public class PreparePersistentNotificationTaskTest extends AbstractNotificationTaskTest {

    private static final Collection<PersistentNotification> NEW_NNS = new ArrayList<>();


    @Test
    public void testContract() {
        final NotificationFacadeRegistry registry = mock(NotificationFacadeRegistry.class);
        final PersistentNotificationFacade facade = mock(PersistentNotificationFacade.class);

        final ExecutorService executor = MoreExecutors.newDirectExecutorService();
        final PreparePersistentNotificationTask task =
            new CustomPreparePersistentNotificationTask(executor, registry, facade, 0);

        assertThat(task.findForPreparing(), equalTo(NEW_NNS));
    }

    @Test
    public void testNoNotification() throws Exception {
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();

        when(notificationDao.findForPreparing(anyInt(), anyInt())).thenReturn(NEW_NNS);

        executeTask(facade, registry);

        verify(notificationDao, times(1)).findForPreparing(anyInt(), anyInt());
        verify(notificationDao, never()).updateStatuses(any());
    }

    @Test
    public void testSpam() throws Exception {
        final PersistentNotification notification = createNotification(NotificationStatus.NEW);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();

        when(facade.getSpamFilterProvider().provide(any())).thenReturn(NotificationSpamStatus.SPAM);
        when(daoFacade.getNotificationDao().findForPreparing(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));

        executeTask(facade, registry);

        verifyResult(facade, notification, NotificationStatus.SPAM);
    }

    @Test
    public void testPreparationError() throws Exception {
        final PersistentNotification notification = createNotification(NotificationStatus.NEW);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();

        when(facade.getSpamFilterProvider().provide(any())).thenReturn(NotificationSpamStatus.NOT_SPAM);
        when(daoFacade.getNotificationDao().findForPreparing(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));
        when(daoFacade.getDestinationDao().findByGroup(anyLong())).thenReturn(null);

        executeTask(facade, registry);

        verifyErrorResult(facade, notification, NotificationStatus.PREPARATION_ERROR);
    }

    @Test
    public void testNoRecipient() throws Exception {
        final PersistentNotification notification = createNotification(NotificationStatus.NEW);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();

        when(facade.getSpamFilterProvider().provide(any())).thenReturn(NotificationSpamStatus.NOT_SPAM);
        when(daoFacade.getNotificationDao().findForPreparing(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));
        when(daoFacade.getDestinationDao().findByGroup(anyLong())).thenReturn(Collections.emptySet());

        executeTask(facade, registry);

        verifyResult(facade, notification, NotificationStatus.NO_RECIPIENT);
    }

    /**
     * Если во время резолва адреса произошла ошибка, необходимо отправить письмо с NO_RECIPIENT.
     */
    @Test
    public void testAliasResolvingException() throws Exception {
        final PersistentNotificationDestination persistentDest = createDestination();
        final NotificationDestination dest = mock(NotificationDestination.class);
        final PersistentNotification notification = createNotification(NotificationStatus.NEW);

        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();

        final NotificationAddressProvider addressProvider = registry.get(NotificationTransport.EMAIL).getAddressResolverRegistry().getAll().iterator().next();
        when(addressProvider.provide(any())).thenThrow(new AddressException("exception during address resolving"));

        when(facade.getSpamFilterProvider().provide(any())).thenReturn(NotificationSpamStatus.NOT_SPAM);
        when(facade.getNotificationComposer().composeDestination(any())).thenReturn(dest);
        when(daoFacade.getNotificationDao().findForPreparing(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));
        when(daoFacade.getDestinationDao().findByGroup(anyLong())).thenReturn(Collections.singleton(persistentDest));

        executeTask(facade, registry);

        verifyResult(facade, notification, NotificationStatus.NO_RECIPIENT);

        verify(daoFacade.getAddressDao(), never()).create(any());
    }

    @Test
    public void testPrepared() throws Exception {
        final PersistentNotificationDestination persistentDest = createDestination();
        final NotificationDestination dest = mock(NotificationDestination.class);
        final PersistentNotification notification = createNotification(NotificationStatus.NEW);

        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final NotificationFacadeRegistry registry = createRegistry();

        when(facade.getSpamFilterProvider().provide(any())).thenReturn(NotificationSpamStatus.NOT_SPAM);
        when(facade.getNotificationComposer().composeDestination(any())).thenReturn(dest);
        when(daoFacade.getNotificationDao().findForPreparing(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));
        when(daoFacade.getDestinationDao().findByGroup(anyLong())).thenReturn(Collections.singleton(persistentDest));

        executeTask(facade, registry);

        verifyResult(facade, notification, NotificationStatus.PREPARED);

        ArgumentCaptor<Collection<PersistentNotificationAddress>> addressCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(daoFacade.getAddressDao()).create(addressCaptor.capture());
        Collection<PersistentNotificationAddress> address = addressCaptor.getValue();
        assertEquals(1, address.size());
    }


    private void executeTask(
        final PersistentNotificationFacade facade, final NotificationFacadeRegistry registry
    ) throws Exception {
        final ExecutorService executor = MoreExecutors.newDirectExecutorService();
        final PreparePersistentNotificationTask task = new PreparePersistentNotificationTask(
                executor, registry, facade, 0, new QuadraticRetryTimeCalculator(), 3);

        task.execute();
    }

    private void verifyResult(
        final PersistentNotificationFacade facade, final PersistentNotification notification,
        final NotificationStatus status
    ) {
        final PersistentDaoFacade daoFacade = facade.getPersistentDaoFacade();
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();
        final long notificationId = notification.getId();

        verify(notificationDao, times(1)).findForPreparing(anyInt(), anyInt());
        verify(notificationDao, times(1)).updateStatuses(Collections.singletonMap(notificationId, status));
        verify(facade.getSpamFilterProvider(), times(1)).provide(any());
    }

    private void verifyErrorResult(
            final PersistentNotificationFacade facade, final PersistentNotification notification,
            final NotificationStatus status
    ) {
        final PersistentDaoFacade daoFacade = facade.getPersistentDaoFacade();
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();
        final long notificationId = notification.getId();

        verify(notificationDao, times(1)).findForPreparing(anyInt(), anyInt());
        Instant instant = notification.getDeliveryData().getDeliveryTime().plusMillis(1_860_000L);
        verify(notificationDao, times(1)).markAsError(Collections.singletonMap(notificationId, instant), status);
        verify(facade.getSpamFilterProvider(), times(1)).provide(any());
    }

    private NotificationFacadeRegistry createRegistry() {
        final NotificationFacadeRegistry registry = mock(NotificationFacadeRegistry.class);
        final NotificationFacade notificationFacade = mock(NotificationFacade.class);
        final NotificationAddress address = mock(NotificationAddress.class);
        final NotificationAddressProvider addressProvider = mock(NotificationAddressProvider.class);
        final NotificationAddressResolverRegistry resolverRegistry = mock(NotificationAddressResolverRegistry.class);
        final NotificationAddressComposer addressComposer = new NoChangeAddressComposer();

        when(notificationFacade.getAddressResolverRegistry()).thenReturn(resolverRegistry);
        when(notificationFacade.getAddressComposer()).thenReturn(addressComposer);
        when(resolverRegistry.getAll()).thenReturn(Collections.singleton(addressProvider));
        when(addressProvider.provide(any())).thenReturn(Collections.singleton(address));

        for (final NotificationTransport transport : NotificationTransport.values()) {
            when(registry.get(transport)).thenReturn(notificationFacade);
        }

        return registry;
    }


    /**
     * Класс, проверяющий контракт {@link PreparePersistentNotificationTask}.
     *
     * @author Vladislav Bauer
     */
    private static class CustomPreparePersistentNotificationTask extends PreparePersistentNotificationTask {

        CustomPreparePersistentNotificationTask(
            @Nonnull final ExecutorService executorService,
            @Nonnull final NotificationFacadeRegistry notificationFacadeRegistry,
            @Nonnull final PersistentNotificationFacade persistentFacade,
            final long delayTimeout
        ) {
            super(executorService, notificationFacadeRegistry, persistentFacade, delayTimeout, new QuadraticRetryTimeCalculator(), 3);
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected Collection<PersistentNotification> findForPreparing() {
            return NEW_NNS;
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected NotificationStatus prepare(@Nonnull final PersistentNotification notification) {
            return NotificationStatus.NEW;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void updateStatuses(@Nonnull final Map<Long, NotificationStatus> statusMap) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void updateErrorStatuses(@Nonnull Collection<PersistentNotification> error) {
            super.updateErrorStatuses(error);
        }
    }

}
