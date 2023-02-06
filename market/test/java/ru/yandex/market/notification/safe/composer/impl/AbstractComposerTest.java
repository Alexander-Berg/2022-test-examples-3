package ru.yandex.market.notification.safe.composer.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Ignore;

import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.safe.composer.NotificationComposerFacade;
import ru.yandex.market.notification.service.registry.type.NotificationAddressTypeRegistry;
import ru.yandex.market.notification.service.registry.type.NotificationContentTypeRegistry;
import ru.yandex.market.notification.service.registry.type.NotificationDestinationTypeRegistry;
import ru.yandex.market.notification.service.serial.DataSerializer;
import ru.yandex.market.notification.simple.service.registry.type.NotificationAddressTypeRegistryImpl;
import ru.yandex.market.notification.simple.service.registry.type.NotificationContentTypeRegistryImpl;
import ru.yandex.market.notification.simple.service.registry.type.NotificationDestinationTypeRegistryImpl;
import ru.yandex.market.notification.simple.service.serial.XmlDataSerializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Базовый класс для unit-тестов composer'ов.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class AbstractComposerTest {

    static final byte[] TEST_DATA = "<data/>".getBytes(StandardCharsets.UTF_8);
    static final String TEST_TYPE = "TEST_TYPE";
    static final String DATA = "data";


    @Nonnull
    NotificationComposerFacade createFacade() {
        final DataSerializer dataSerializer = new XmlDataSerializer();
        final NotificationAddressTypeRegistry addressTypeRegistry = new NotificationAddressTypeRegistryImpl(
            Collections.singletonMap(TestAddress.class, TEST_TYPE)
        );
        final NotificationContentTypeRegistry contentTypeRegistry = new NotificationContentTypeRegistryImpl(
            Collections.singletonMap(TestContent.class, TEST_TYPE)
        );
        final NotificationDestinationTypeRegistry destinationTypeRegistry = new NotificationDestinationTypeRegistryImpl(
            Collections.singletonMap(TestDestination.class, TEST_TYPE)
        );

        final NotificationComposerFacade facade = mock(NotificationComposerFacade.class);
        when(facade.getAddressTypeDetector()).thenReturn(addressTypeRegistry);
        when(facade.getContentTypeDetector()).thenReturn(contentTypeRegistry);
        when(facade.getDestinationTypeDetector()).thenReturn(destinationTypeRegistry);
        when(facade.getDataSerializer()).thenReturn(dataSerializer);

        return facade;
    }



    @Immutable
    @XmlRootElement(name = DATA)
    static class TestAddress implements NotificationAddress {

        TestAddress() {
        }

    }

    @Immutable
    @XmlRootElement(name = DATA)
    static class TestContent implements NotificationContent {

        TestContent() {
        }

    }

    @Immutable
    @XmlRootElement(name = DATA)
    static class TestDestination implements NotificationDestination {

        TestDestination() {
        }

    }

}
