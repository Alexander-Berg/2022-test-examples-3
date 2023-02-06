package ru.yandex.market.notification.simple.service.provider.complex;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.model.data.NotificationTemplate;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.provider.NotificationContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.registry.NotificationDataProviderRegistry;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ComplexNotificationContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class ComplexNotificationContentProviderTest {

    /*
     * Наличие {@link NotificationContext#getContent()} предотвращает запуск {@link NotificationContentProvider}.
     */
    @Test
    public void testHasContent() {
        final NotificationContent originContent = createContent();
        final NotificationContext context = createContext(originContent);
        final NotificationFacade facade = createFacade(originContent);

        final ComplexNotificationContentProvider complexProvider = new ComplexNotificationContentProvider(facade);
        final NotificationContent providedContent = complexProvider.provide(context);

        assertThat(providedContent, equalTo(originContent));

        verifyZeroInteractions(facade);
    }

    /*
     * Отсутствие {@link NotificationContext#getContent()} запускает {@link NotificationContentProvider}.
     */
    @Test
    public void testHasNotContent() {
        final NotificationContent originContent = createContent();
        final NotificationContext context = createContext(null);
        final NotificationFacade facade = createFacade(originContent);

        final ComplexNotificationContentProvider complexProvider = new ComplexNotificationContentProvider(facade);
        final NotificationContent providedContent = complexProvider.provide(context);

        assertThat(providedContent, equalTo(originContent));

        verify(facade, times(1)).getContentProvider();
        verify(facade, times(1)).getTemplateProvider();
        verify(facade, times(1)).getExtraDataProviderRegistry();
        verifyNoMoreInteractions(facade);
    }


    private NotificationContent createContent() {
        return mock(NotificationContent.class);
    }

    private NotificationContext createContext(final NotificationContent originContent) {
        final NotificationContext context = mock(NotificationContext.class);
        final Optional<NotificationContent> optional = Optional.ofNullable(originContent);

        when(context.getContent()).thenReturn(optional);

        return context;
    }

    private NotificationFacade createFacade(final NotificationContent providedContent) {
        final NotificationContentProvider contentProvider = mock(NotificationContentProvider.class);
        when(contentProvider.provide(any())).thenReturn(providedContent);

        final NotificationTemplateProvider templateProvider = mock(NotificationTemplateProvider.class);
        when(templateProvider.provide(any())).thenReturn(mock(NotificationTemplate.class));

        final NotificationDataProviderRegistry dataProviderRegistry = mock(NotificationDataProviderRegistry.class);
        when(dataProviderRegistry.getAll()).thenReturn(emptySet());

        final NotificationFacade facade = mock(NotificationFacade.class);
        when(facade.getTemplateProvider()).thenReturn(templateProvider);
        when(facade.getContentProvider()).thenReturn(contentProvider);
        when(facade.getExtraDataProviderRegistry()).thenReturn(dataProviderRegistry);

        return facade;
    }

}
