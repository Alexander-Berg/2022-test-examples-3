package ru.yandex.market.notification.service.provider.complex;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.model.data.NotificationTemplate;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.provider.NotificationContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.registry.NotificationDataProviderRegistry;
import ru.yandex.market.notification.simple.service.provider.complex.ComplexNotificationContentProvider;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        var originContent = createContent();
        var context = createContext(originContent);
        var facade = createFacade(originContent);

        var complexProvider = new ComplexNotificationContentProvider(facade);
        var providedContent = complexProvider.provide(context);

        assertThat(providedContent, equalTo(originContent));

        verifyNoInteractions(facade);
    }

    /*
     * Отсутствие {@link NotificationContext#getContent()} запускает {@link NotificationContentProvider}.
     */
    @Test
    public void testHasNotContent() {
        var originContent = createContent();
        var context = createContext(null);
        var facade = createFacade(originContent);

        var complexProvider = new ComplexNotificationContentProvider(facade);
        var providedContent = complexProvider.provide(context);

        assertThat(providedContent, equalTo(originContent));

        verify(facade, times(1)).getContentProvider();
        verify(facade, times(1)).getTemplateProvider();
        verify(facade, times(1)).getExtraDataProviderRegistry();
        verifyNoMoreInteractions(facade);
    }


    private NotificationContent createContent() {
        return mock(NotificationContent.class);
    }

    private NotificationContext createContext(NotificationContent originContent) {
        var context = mock(NotificationContext.class);
        var optional = Optional.ofNullable(originContent);

        when(context.getContent()).thenReturn(optional);

        return context;
    }

    private NotificationFacade createFacade(NotificationContent providedContent) {
        var contentProvider = mock(NotificationContentProvider.class);
        when(contentProvider.provide(any())).thenReturn(providedContent);

        var templateProvider = mock(NotificationTemplateProvider.class);
        when(templateProvider.provide(any())).thenReturn(mock(NotificationTemplate.class));

        var dataProviderRegistry = mock(NotificationDataProviderRegistry.class);
        when(dataProviderRegistry.getAll()).thenReturn(emptySet());

        var facade = mock(NotificationFacade.class);
        when(facade.getTemplateProvider()).thenReturn(templateProvider);
        when(facade.getContentProvider()).thenReturn(contentProvider);
        when(facade.getExtraDataProviderRegistry()).thenReturn(dataProviderRegistry);

        return facade;
    }

}
