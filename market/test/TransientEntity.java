package ru.yandex.market.jmf.trigger.test;

import ru.yandex.market.jmf.trigger.TriggerSnapshotable;

public interface TransientEntity extends TriggerSnapshotable {
    Long getAttrInt();

    String getAttrStr();
}
