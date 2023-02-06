package ru.yandex.market.logistics.iris.core.index.dummy;

import javax.annotation.Nullable;

import ru.yandex.market.logistics.iris.core.index.change.ChangeType;
import ru.yandex.market.logistics.iris.core.index.change.ReferenceIndexChange;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;

public class TestReferenceIndexChange<T> extends ReferenceIndexChange<T> {

    public TestReferenceIndexChange(Field<T> field,
                                    @Nullable FieldValue<T> value,
                                    ChangeType changeType) {
        super(field, value, changeType);
    }
}