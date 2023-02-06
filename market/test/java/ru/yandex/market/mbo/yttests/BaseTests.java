package ru.yandex.market.mbo.yttests;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.operations.TestYtUtils;

public abstract class BaseTests {

    @SafeVarargs
    protected static List<YTreeMapNode> wrap(Map<String, Object>... data) {
        return Arrays.stream(data).map(m -> YTree.node(m).mapNode()).collect(Collectors.toList());
    }

    protected static <TInput> Stream<TInput> toStream(Iterator<TInput> iterator) {
        return TestYtUtils.toStream(iterator);
    }
}
