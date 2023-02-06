package ru.yandex.market.tsup.core.event;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.event.impl.demo.DemoEventPayload;
import ru.yandex.market.tsup.core.event.impl.demo.DemoEventSubscriber;

class EventDistributorTest extends AbstractContextualTest {
    private EventDistributor eventDistributor;

    @Mock
    private DemoEventSubscriber demoEventSubscriber;

    @BeforeEach
    void setUp() {
        Mockito.when(demoEventSubscriber.getEventType()).thenReturn(EventType.DEMO);
        eventDistributor = new EventDistributor(List.of(demoEventSubscriber));
    }

    @Test
    void publish() {
        eventDistributor.distribute(EventType.DEMO, new DemoEventPayload("aaabbb"));
        Mockito.verify(demoEventSubscriber).accept(new DemoEventPayload("aaabbb"));
    }
}
