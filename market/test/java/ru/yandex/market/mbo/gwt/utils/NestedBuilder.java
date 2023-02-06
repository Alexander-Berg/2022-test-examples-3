package ru.yandex.market.mbo.gwt.utils;

import java.util.function.Function;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2019
 */
public abstract class NestedBuilder<Parent, TargetObject> {

    private final Function<TargetObject, Parent> toParent;

    public NestedBuilder(Function<TargetObject, Parent> toParent) {
        this.toParent = toParent;
    }

    protected abstract TargetObject buildObject();

    public Parent end() {
        return toParent.apply(buildObject());
    }
}
