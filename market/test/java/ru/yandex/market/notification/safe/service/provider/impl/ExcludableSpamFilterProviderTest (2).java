package ru.yandex.market.notification.safe.service.provider.impl;

import java.util.Set;

import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ExcludableSpamFilterProvider}.
 *
 * @author avetokhin 11/08/16.
 */
public class ExcludableSpamFilterProviderTest {
    private static final Set<Long> EXCLUDED_TYPES = Set.of(
            10L,
            20L,
            30L
    );

    private static final NotificationType CODE_TYPE_NO_EXCLUDE_1 = new CodeNotificationType(5L);
    private static final NotificationType CODE_TYPE_NO_EXCLUDE_2 = new CodeNotificationType(6L);
    private static final NotificationType CODE_TYPE_EXCLUDE_1 = new CodeNotificationType(10L);
    private static final NotificationType CODE_TYPE_EXCLUDE_2 = new CodeNotificationType(20L);

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

    private static void testExcludeForType(NotificationType type) {
        var decoratedProvider = prepareFilterProviderMock();
        var provider = new ExcludableSpamFilterProvider(decoratedProvider, EXCLUDED_TYPES);

        var status = provider.provide(createNotification(type));

        assertThat(status, equalTo(NotificationSpamStatus.NOT_SPAM));
        verifyNoInteractions(decoratedProvider);
    }

    private static void testNotExcludeForType(NotificationType type) {
        var decoratedProvider = prepareFilterProviderMock();
        var provider = new ExcludableSpamFilterProvider(decoratedProvider, EXCLUDED_TYPES);

        var notification = createNotification(type);
        var status = provider.provide(notification);

        assertThat(status, equalTo(NotificationSpamStatus.SPAM));
        verify(decoratedProvider).provide(notification);
    }

    private static PersistentNotification createNotification(NotificationType type) {
        //noinspection ConstantConditions
        return new PersistentNotification(1L, 1L, type, null, null, null, null, null,
                false);
    }

    /**
     * Создать мок провайдера, который всегда возвращает {@link NotificationSpamStatus#SPAM}.
     */
    private static SpamFilterProvider prepareFilterProviderMock() {
        var provider = mock(SpamFilterProvider.class);
        when(provider.provide(any(PersistentNotification.class))).thenReturn(NotificationSpamStatus.SPAM);
        return provider;
    }

}
