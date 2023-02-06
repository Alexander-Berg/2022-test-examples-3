package ru.yandex.market.logistics.lom.utils;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class YtTestUtils {

    @Nonnull
    public static <T> CloseableIterator<T> getIterator(Iterable<T> iterable) {
        return CloseableIterator.wrap(iterable.iterator());
    }

    @Nonnull
    public static YTreeMapNode buildMapNode(Map<String, ?> map) {
        YTreeBuilder yTreeBuilder = new YTreeBuilder().beginMap();

        map.forEach((key, value) -> yTreeBuilder.key(key).value(value));

        return (YTreeMapNode) yTreeBuilder.endMap().build();
    }
}
