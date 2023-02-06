package ru.yandex.market.crm.campaign.yt;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.impl.ytree.object.FieldsBindingStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.OptionSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

@YTreeObject(
        bindingStrategy = FieldsBindingStrategy.ANNOTATED_ONLY,
        nullSerializationStrategy = NullSerializationStrategy.IGNORE_NULL_FIELDS,
        optionSerializationStrategy = OptionSerializationStrategy.EMPTY_OPTION
)
public class Entity {

    @YTreeField(key = "id")
    private String id;

    @YTreeField(key = "value")
    private Option<String> value;

    @YTreeField(key = "value2")
    private Option<Long> value2;

    public Entity(String id, String value, Long value2) {
        this.id = id;
        this.value = Option.ofNullable(value);
        this.value2 = Option.ofNullable(value2);
    }

    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Entity setId(String id) {
        this.id = id;
        return this;
    }

    public String getValue() {
        return value.getOrNull();
    }

    public Entity setValue(String value) {
        this.value = Option.ofNullable(value);
        return this;
    }

    public Long getValue2() {
        return value2.getOrNull();
    }

    public void setValue2(Long value2) {
        this.value2 = Option.ofNullable(value2);
    }

    @Override
    public String toString() {
        return "Entity{" + id + "," + value + "," + value2 + "}";
    }
}
