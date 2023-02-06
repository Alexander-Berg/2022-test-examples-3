package ru.yandex.market.loyalty.core.trigger;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventType;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

@RunWith(Parameterized.class)
@Ignore
public class EventTypeValidationTest {
    private final TriggerEventType<?> eventType;

    public EventTypeValidationTest(TriggerEventType<?> eventType) {
        this.eventType = eventType;
    }

    @Parameterized.Parameters
    public static Iterable<?> eventTypes() {
        return Arrays.stream(TriggerEventType.class.getDeclaredFields())
                .filter(field -> field.getType().equals(TriggerEventType.class))
                .map(field -> makeExceptionsUnchecked(() -> field.get(null)))
                .collect(Collectors.toList());
    }

    @Test
    @Ignore
    public void validateEventType() {
        assertEquals(
                eventType,
                eventType.getEventConstructor()
                        .construct(123L, Collections.emptyMap(), null, null, 1, TriggerEventProcessedResult.IN_QUEUE,
                                false)
                        .getEventType()
        );
    }
}
