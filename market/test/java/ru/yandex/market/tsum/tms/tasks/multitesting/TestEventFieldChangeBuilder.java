package ru.yandex.market.tsum.tms.tasks.multitesting;

import ru.yandex.bolts.collection.Option;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.FieldRef;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.01.2018
 */
public class TestEventFieldChangeBuilder {
    private FieldRef field;
    private Option<?> from = Option.empty();
    private Option<?> to = Option.empty();

    public static TestEventFieldChangeBuilder anEventFieldChange() {
        return new TestEventFieldChangeBuilder();
    }

    public TestEventFieldChangeBuilder field(FieldRef field) {
        this.field = field;
        return this;
    }

    public TestEventFieldChangeBuilder from(Object from) {
        this.from = Option.of(from);
        return this;
    }

    public TestEventFieldChangeBuilder to(Object to) {
        this.to = Option.of(to);
        return this;
    }

    public Event.FieldChange build() {
        return new Event.FieldChange(field, from, to);
    }
}
