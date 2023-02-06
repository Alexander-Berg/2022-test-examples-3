package ru.yandex.market.tsum.tms.tasks.multitesting;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.startrek.client.model.Event;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.01.2018
 */
public class TestEventBuilder {
    private Instant updatedAt;
    private List<Event.FieldChange> fieldChanges;

    public static TestEventBuilder anEvent() {
        return new TestEventBuilder();
    }

    public TestEventBuilder updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public TestEventBuilder fieldChanges(Event.FieldChange... fieldChanges) {
        this.fieldChanges = Arrays.asList(fieldChanges);
        return this;
    }

    public Event build() {
        return new Event(
            null,
            null,
            updatedAt == null ? null : new org.joda.time.Instant(updatedAt.toEpochMilli()),
            null,
            null,
            null,
            null,
            fieldChanges == null ? null : Cf.toList(fieldChanges),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
