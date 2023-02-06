package ru.yandex.market.notification.safe.service.provider.impl;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.type.NotificationSpamStatus;
import ru.yandex.market.notification.safe.service.provider.SpamFilterProvider;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ExcludableSpamFilterProvider}.
 *
 * @author avetokhin 11/08/16.
 */
public class ExcludableSpamFilterProviderTest {

    private static final Set<Long> EXCLUDED_TYPES = new HashSet<>();

    private static final NotificationType CODE_TYPE_NO_EXCLUDE_1 = new CodeNotificationType(5L);
    private static final NotificationType CODE_TYPE_NO_EXCLUDE_2 = new NotificationType() {
    };
    private static final NotificationType CODE_TYPE_EXCLUDE_1 = new CodeNotificationType(10L);
    private static final NotificationType CODE_TYPE_EXCLUDE_2 = new CodeNotificationType(20L);

    static {
        EXCLUDED_TYPES.add(10L);
        EXCLUDED_TYPES.add(20L);
        EXCLUDED_TYPES.add(30L);
    }

    @Test
    public void testExclude() {
        testExcludeForType(CODE_TYPE_EXCLUDE_1);
        testExcludeForType(CODE_TYPE_EXCLUDE_2);
    }

    @Test
    public void testNotExclude() {
        testNotExcludeForType(CODE_TYPE_NO_EXCLUDE_1);
        testNotExcludeForType(CODE_TYPE_NO_EXCLUDE_2);
    }

    private void testExcludeForType(final NotificationType type) {
        final SpamFilterProvider decoratedProvider = prepareFilterProviderMock();
        final ExcludableSpamFilterProvider provider =
            new ExcludableSpamFilterProvider(decoratedProvider, EXCLUDED_TYPES);

        final NotificationSpamStatus status = provider.provide(createNotification(type));

        assertThat(status, equalTo(NotificationSpamStatus.NOT_SPAM));
        verifyZeroInteractions(decoratedProvider);
    }

    private void testNotExcludeForType(final NotificationType type) {
        final SpamFilterProvider decoratedProvider = prepareFilterProviderMock();
        final ExcludableSpamFilterProvider provider =
            new ExcludableSpamFilterProvider(decoratedProvider, EXCLUDED_TYPES);

        final PersistentNotification notification = createNotification(type);
        final NotificationSpamStatus status = provider.provide(notification);

        assertThat(status, equalTo(NotificationSpamStatus.SPAM));
        verify(decoratedProvider).provide(notification);
    }

    private PersistentNotification createNotification(final NotificationType type) {
        //noinspection ConstantConditions
        return new PersistentNotification(1L, 1L, type, null, null, null, null, null);
    }

    /**
     * Создать мок провайдера, который всегда возвращает {@link NotificationSpamStatus#SPAM}.
     */
    private SpamFilterProvider prepareFilterProviderMock() {
        final SpamFilterProvider provider = mock(SpamFilterProvider.class);
        when(provider.provide(any(PersistentNotification.class))).thenReturn(NotificationSpamStatus.SPAM);
        return provider;
    }

}
