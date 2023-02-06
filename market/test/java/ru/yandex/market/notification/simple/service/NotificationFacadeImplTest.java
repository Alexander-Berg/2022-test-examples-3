package ru.yandex.market.notification.simple.service;

import org.junit.Test;

import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.NotificationTransportService;
import ru.yandex.market.notification.service.composer.NotificationAddressComposer;
import ru.yandex.market.notification.service.provider.NotificationContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.registry.NotificationAddressResolverRegistry;
import ru.yandex.market.notification.service.registry.NotificationDataProviderRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link NotificationFacadeImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationFacadeImplTest {

    @Test
    public void testConstruction() {
        final NotificationAddressResolverRegistry addressResolverRegistry =
            mock(NotificationAddressResolverRegistry.class);

        final NotificationDataProviderRegistry dataProviderRegistry = mock(NotificationDataProviderRegistry.class);
        final NotificationContentProvider contentProvider = mock(NotificationContentProvider.class);
        final NotificationTemplateProvider templateProvider = mock(NotificationTemplateProvider.class);
        final NotificationTransportService transportService = mock(NotificationTransportService.class);
        final NotificationAddressComposer addressComposer = mock(NotificationAddressComposer.class);

        final NotificationFacade facade = new NotificationFacadeImpl(
            addressResolverRegistry,
            dataProviderRegistry,
            contentProvider,
            templateProvider,
            transportService,
            addressComposer
        );


        assertThat(facade.getAddressResolverRegistry(), equalTo(addressResolverRegistry));
        assertThat(facade.getExtraDataProviderRegistry(), equalTo(dataProviderRegistry));
        assertThat(facade.getContentProvider(), equalTo(contentProvider));
        assertThat(facade.getTemplateProvider(), equalTo(templateProvider));
        assertThat(facade.getTransportService(), equalTo(transportService));
        assertThat(facade.getAddressComposer(), equalTo(addressComposer));
    }

}
