package ru.yandex.market.mboc.common.infrastructure.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author yuramalinov
 * @created 27.08.18
 */
@SuppressWarnings({"unchecked", "checkstyle:MagicNumber"})
public class UnstableInitTest {
    private Object marker = new Object();
    private ScheduledExecutorService executor;
    private volatile boolean failSupplierResponse;
    private Supplier<Object> failingSupplier = () -> {
        if (failSupplierResponse) {
            throw new RuntimeException("This is fine, just testing failures!");
        } else {
            return marker;
        }
    };

    @Before
    public void setup() {
        executor = Executors.newSingleThreadScheduledExecutor();
        failSupplierResponse = true;
    }

    @Test
    public void testItStartsFine() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, () -> marker);
        assertTrue(init.isAvailable());
        assertEquals(marker, init.get());
    }

    @Test
    public void testItWillTriggerActions() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, () -> marker);
        assertTrue(init.isAvailable());
        assertEquals(marker, init.get());

        Consumer<Object> mock = Mockito.mock(Consumer.class);
        init.whenAvailable(mock);
        Mockito.verify(mock).accept(marker);

        mock = Mockito.mock(Consumer.class);
        assertTrue(init.ifAvailable(mock));
        Mockito.verify(mock).accept(marker);
    }

    @Test
    public void testItShowsFail() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, failingSupplier);
        assertFalse(init.isAvailable());

        Consumer<Object> whenMock = Mockito.mock(Consumer.class);
        init.whenAvailable(whenMock);
        Mockito.verifyZeroInteractions(whenMock);

        Consumer<Object> ifMock = Mockito.mock(Consumer.class);
        assertFalse(init.ifAvailable(ifMock));
        Mockito.verifyZeroInteractions(ifMock);
    }

    @Test
    public void testLateInit() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, failingSupplier, 10, TimeUnit.MILLISECONDS);
        assertFalse(init.isAvailable());

        Consumer<Object> whenMock = Mockito.mock(Consumer.class);
        init.whenAvailable(whenMock);
        Mockito.verifyZeroInteractions(whenMock);

        failSupplierResponse = false;

        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
            Mockito.verify(whenMock).accept(marker));
    }

    @Test
    public void testInstantChaining() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, () -> marker, 10, TimeUnit.MILLISECONDS);
        assertTrue(init.isAvailable());

        UnstableInit<Object> init2 = UnstableInit.when(init).then("test", executor, realInit -> marker);
        assertTrue(init2.isAvailable()); // Instant init
    }

    @Test
    public void testChaining() {
        UnstableInit<Object> init = new UnstableInit<>("test", executor, failingSupplier, 10, TimeUnit.MILLISECONDS);
        assertFalse(init.isAvailable());

        Supplier<Object> mock = Mockito.mock(Supplier.class);
        Mockito.when(mock.get()).thenReturn(marker);
        UnstableInit<Object> init2 = UnstableInit.when(init).then("test", executor, __ -> mock.get());
        assertFalse(init2.isAvailable());
        Mockito.verifyZeroInteractions(mock); // Not called

        failSupplierResponse = false;

        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(init::isAvailable);
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(init2::isAvailable);
        assertTrue(init.isAvailable());
        assertTrue(init2.isAvailable());

        Mockito.verify(mock, Mockito.times(1)).get();
    }

    @Test
    public void testChainingWithDoubleInits() {
        UnstableInit<Object> init1 = new UnstableInit<>("test1", executor, failingSupplier, 100, TimeUnit.MILLISECONDS);
        assertFalse(init1.isAvailable());
        UnstableInit<Object> init2 = new UnstableInit<>("test2", executor, failingSupplier, 100, TimeUnit.MILLISECONDS);
        assertFalse(init2.isAvailable());

        Supplier<Object> mock = Mockito.mock(Supplier.class);
        Mockito.when(mock.get()).thenReturn(marker);
        UnstableInit<Object> initResult = UnstableInit.when(init1, init2).then("test", executor, (a, b) -> mock.get());
        assertFalse(initResult.isAvailable());
        Mockito.verifyZeroInteractions(mock); // Not called

        failSupplierResponse = false;

        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(init1::isAvailable);
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(init2::isAvailable);
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(initResult::isAvailable);
        assertTrue(init1.isAvailable());
        assertTrue(init2.isAvailable());
        assertTrue(initResult.isAvailable());

        Mockito.verify(mock, Mockito.times(1)).get();
    }
}
