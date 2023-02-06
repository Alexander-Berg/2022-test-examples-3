package ru.yandex.market.fintech.banksint.util;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;


public class YTreeMapNodeBuilder {

    private final Map<String, YTreeNode> attributes = new HashMap<>();

    public YTreeMapNodeBuilder addLong(String name, long value) {
        attributes.put(name, new YTreeIntegerNodeImpl(true, value, null));
        return this;
    }

    public YTreeMapNodeBuilder addString(String name, String value) {
        attributes.put(name, new YTreeStringNodeImpl(value, null));
        return this;
    }

    public YTreeMapNodeBuilder addDouble(String name, double value) {
        attributes.put(name, new YTreeDoubleNodeImpl(value, null));
        return this;
    }

    public YTreeMapNodeBuilder addNull(String name) {
        attributes.put(name, null);
        return this;
    }


    public YTreeMapNode build() {
        final YTreeMapNode entries = new YTreeMapNodeImpl(null);
        attributes.forEach(entries::put);
        return entries;
    }
}

