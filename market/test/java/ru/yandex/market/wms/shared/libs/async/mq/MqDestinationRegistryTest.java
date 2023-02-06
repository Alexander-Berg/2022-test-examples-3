package ru.yandex.market.wms.shared.libs.async.mq;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MqDestinationRegistryTest {
    /**
     * Проверяем, что очередь, полученная из списка созданных при инициализации, не создается повторно.
     */
    @Test
    public void cachedDestinationNotRecreated() {
        MqAdminClient adminClient = mock(MqAdminClient.class);
        when(adminClient.fetchKnownDestinations()).thenReturn(Collections.singletonList("abc"));

        MqDestinationRegistry reg = new MqDestinationRegistry(adminClient);
        reg.registerDestinationIfNecessary("abc");

        verify(adminClient, never()).createDestination("abc");
    }

    /**
     * Проверяем, что при успешной обработке очереди она не обрабатывается повторно.
     */
    @Test
    public void createdDestinationCached() {
        MqAdminClient adminClient = mock(MqAdminClient.class);
        when(adminClient.fetchKnownDestinations()).thenReturn(Collections.emptyList());

        MqDestinationRegistry reg = new MqDestinationRegistry(adminClient);
        reg.registerDestinationIfNecessary("abc");
        reg.registerDestinationIfNecessary("abc");

        verify(adminClient).createDestination("abc");
    }

    /**
     * Проверяем, что при ошибке очередь не попадает в список "обработанных"
     */
    @Test
    public void destinationNotCachedOnException() {
        MqAdminClient adminClient = mock(MqAdminClient.class);
        when(adminClient.fetchKnownDestinations()).thenReturn(Collections.emptyList());

        MqDestinationRegistry reg = new MqDestinationRegistry(adminClient);

        doThrow(new RuntimeException()).doNothing().when(adminClient).createDestination("abc");

        assertThrows(RuntimeException.class, () -> reg.registerDestinationIfNecessary("abc"));
        reg.registerDestinationIfNecessary("abc");

        verify(adminClient, times(2)).createDestination("abc");
    }
}
