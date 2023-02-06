package ru.yandex.market.delivery.transport_manager.event;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.listener.TestEvent;
import ru.yandex.market.delivery.transport_manager.listener.TestEventParallelListeners;
import ru.yandex.market.delivery.transport_manager.listener.TestLisneter;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ThreadPoolEventMulticasterTest extends AbstractContextualTest {
    @Autowired
    private ThreadPoolEventMulticaster multicaster;
    @Autowired
    private TmPropertyService propertyService;
    @Autowired
    private TestLisneter testLisneter;

    @BeforeEach
    void setUp() {
        testLisneter.reset();
        multicaster.prestartAllCoreThreads();
    }

    @Test
    void multicastEventDisabled() {
        String threadName = Thread.currentThread().getName();
        multicaster.multicastEvent(new TestEventParallelListeners());
        assertThat(testLisneter.getCurrentThread1()).isEqualTo(threadName);
        assertThat(testLisneter.getCurrentThread2()).isEqualTo(threadName);
    }

    @Test
    void multicastEventNotSupported() {
        String threadName = Thread.currentThread().getName();
        when(propertyService.getBoolean(TmPropertyKey.MULTITHREAD_EVENT_PROCESSING)).thenReturn(true);
        multicaster.multicastEvent(new TestEvent());
        assertThat(testLisneter.getCurrentThread()).isEqualTo(threadName);
    }

    @Test
    void multicastEvent() {
        String threadName = Thread.currentThread().getName();
        when(propertyService.getBoolean(TmPropertyKey.MULTITHREAD_EVENT_PROCESSING)).thenReturn(true);
        multicaster.multicastEvent(new TestEventParallelListeners(1));
        assertThat(testLisneter.getCurrentThread1()).isNotEqualTo(threadName);
        assertThat(testLisneter.getCurrentThread2()).isNotEqualTo(threadName);
        assertThat(testLisneter.getCurrentThread1()).isNotEqualTo(testLisneter.getCurrentThread2());
    }

    @Test
    void multicastEventTimeout() {
        String threadName = Thread.currentThread().getName();
        when(propertyService.getBoolean(TmPropertyKey.MULTITHREAD_EVENT_PROCESSING)).thenReturn(true);
        assertThatThrownBy(() -> multicaster.multicastEvent(new TestEventParallelListeners(6)))
            .isInstanceOf(TimeoutException.class);
        assertThat(testLisneter.getCurrentThread1()).isNotEqualTo(threadName);
        assertThat(testLisneter.getCurrentThread2()).isNotEqualTo(threadName);
        assertThat(testLisneter.getCurrentThread1()).isNotEqualTo(testLisneter.getCurrentThread2());
    }
}
