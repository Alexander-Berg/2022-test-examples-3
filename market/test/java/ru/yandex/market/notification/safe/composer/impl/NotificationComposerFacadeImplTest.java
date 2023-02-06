package ru.yandex.market.notification.safe.composer.impl;

import org.junit.Test;

import ru.yandex.market.notification.safe.composer.NotificationComposerFacade;
import ru.yandex.market.notification.service.registry.type.NotificationAddressTypeRegistry;
import ru.yandex.market.notification.service.registry.type.NotificationContentTypeRegistry;
import ru.yandex.market.notification.service.registry.type.NotificationDestinationTypeRegistry;
import ru.yandex.market.notification.service.serial.DataSerializer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link NotificationComposerFacadeImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationComposerFacadeImplTest {

    @Test
    public void testConstruction() {
        final NotificationContentTypeRegistry contentTypeRegistry = mock(NotificationContentTypeRegistry.class);
        final NotificationAddressTypeRegistry addressTypeRegistry = mock(NotificationAddressTypeRegistry.class);
        final NotificationDestinationTypeRegistry destinationTypeRegistry = mock(NotificationDestinationTypeRegistry.class);
        final DataSerializer dataSerializer = mock(DataSerializer.class);

        final NotificationComposerFacade facade = new NotificationComposerFacadeImpl(
            contentTypeRegistry,
            addressTypeRegistry,
            destinationTypeRegistry,
            dataSerializer
        );

        assertThat(facade.getContentTypeDetector(), equalTo(contentTypeRegistry));
        assertThat(facade.getAddressTypeDetector(), equalTo(addressTypeRegistry));
        assertThat(facade.getDestinationTypeDetector(), equalTo(destinationTypeRegistry));
        assertThat(facade.getDataSerializer(), equalTo(dataSerializer));
    }

}
