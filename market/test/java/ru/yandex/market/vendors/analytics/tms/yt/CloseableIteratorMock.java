package ru.yandex.market.vendors.analytics.tms.yt;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import ru.yandex.bolts.collection.CloseableIteratorF;
import ru.yandex.market.vendors.analytics.core.utils.json.ObjectMapperFactory;

public class CloseableIteratorMock implements CloseableIteratorF {

    private final Iterator<JsonNode> iterator;

    public CloseableIteratorMock(Collection<Object> collection) {
        var jsonMapper = ObjectMapperFactory.getInstance();
        iterator = collection.stream()
                .map(n -> (JsonNode) jsonMapper.valueToTree(n))
                .collect(Collectors.toList())
                .iterator();
    }

    @Override
    public void close() {
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public JsonNode next() {
        return iterator.next();
    }
}
