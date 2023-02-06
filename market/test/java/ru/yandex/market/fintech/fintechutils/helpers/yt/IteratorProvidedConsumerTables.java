package ru.yandex.market.fintech.fintechutils.helpers.yt;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class IteratorProvidedConsumerTables extends IntermediateTables {

    private final List<YTreeMapNode> dataProvider;

    public IteratorProvidedConsumerTables(List<YTreeMapNode> dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public <T> void read(YPath path, YTableEntryType<T> entryType, Consumer<T> consumer) {
        Iterator<T> iterator = provideIterator();
        iterator.forEachRemaining(consumer);
    }

    // erase type
    private Iterator provideIterator() {
        return dataProvider.iterator();
    }
}
