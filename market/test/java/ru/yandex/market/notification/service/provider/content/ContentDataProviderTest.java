package ru.yandex.market.notification.service.provider.content;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.MbiNotificationData;
import ru.yandex.market.notification.model.data.NotificationData;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vladislav Bauer
 */
public class ContentDataProviderTest {

    private static final ContentDataProvider PROVIDER = new ContentDataProvider();


    @Test
    void testEmpty() {
        checkEmpty(PROVIDER.provide(context(null, emptySet())));
        checkEmpty(PROVIDER.provide(context(list(), emptySet())));
        checkEmpty(PROVIDER.provide(context(new MbiNotificationData(null, null, null), emptySet())));
        checkEmpty(PROVIDER.provide(context(new MbiNotificationData(list(), null, null), emptySet())));
    }

    @Test
    void testSingle() {
        checkSize(PROVIDER.provide(context(
                new MbiNotificationData(list(new Object()), null, null), emptySet()
        )), 1);

        checkSize(PROVIDER.provide(context(
                new MbiNotificationData(list(new Object()), null, null), singleton(data())
        )), 1);
    }

    @Test
    void testMultiple() {
        checkSize(PROVIDER.provide(context(
                new MbiNotificationData(list(new Object(), new Object()), null, null), emptySet()
        )), 2);

        checkSize(PROVIDER.provide(context(
                new MbiNotificationData(list(new Object()), null, null), singleton(list(new Object()))
        )), 2);
    }

    @Test
    void testSameContext() {
        var data = new MbiNotificationData(list(new Object(), new Object()), null, null);
        Collection<NotificationData> extraData = singleton(list(data()));
        NotificationContentProviderContext context = context(data, extraData);

        for (int i = 0; i < 3; i++) {
            checkSize(PROVIDER.provide(context), 3);

            ArrayListNotificationData<?> templateData =
                    context.getParent().getData().orElse(null)
                            .cast(MbiNotificationData.class).getTemplateData().orElse(null);

            assertThat(templateData, hasSize(2));
            assertThat(context.getExtraData(), hasSize(1));
        }
    }


    private static void checkEmpty(NotificationData data) {
        checkSize(data, 0);
    }

    private static void checkSize(NotificationData data, int size) {
        assertThat(data, instanceOf(ArrayListNotificationData.class));
        assertThat((Collection<?>) data, hasSize(size));
    }

    private static NotificationData data() {
        return mock(NotificationData.class);
    }

    private static ArrayListNotificationData<Object> list(Object... data) {
        return new ArrayListNotificationData<>(Arrays.asList(data));
    }

    private static NotificationContentProviderContext context(
            NotificationData data,
            Collection<NotificationData> extraData
    ) {
        NotificationContentProviderContext providerContext = mock(NotificationContentProviderContext.class);
        NotificationContext notificationContext = mock(NotificationContext.class);
        when(providerContext.getParent()).thenReturn(notificationContext);
        when(notificationContext.getData()).thenReturn(Optional.ofNullable(data));
        when(providerContext.getExtraData()).thenReturn(extraData);
        return providerContext;
    }

}
