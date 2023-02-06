package ru.yandex.market.partner.test.context;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.util.StringTestUtil;

/**
 * Класс для имитации чтения тарифов из YT.
 *
 * @author rinbik
 */
public class YtResponseReaderFromJson implements CloseableIterator<YTreeMapNode> {
    private List<YTreeNode> list;
    private Iterator<YTreeNode> iterator;
    public YtResponseReaderFromJson(String fileName, ObjectMapper objectMapper) {
        read(fileName, objectMapper);
        this.iterator = list.iterator();
    }

    private void read(String fileName, ObjectMapper objectMapper) {
        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            CollectionType collectionType = typeFactory.constructCollectionType(List.class, Map.class);
            String json = StringTestUtil.getString(getClass(), fileName);
            list = mapList((List<Map<String, Object>>) objectMapper.readValue(json, collectionType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<YTreeNode> mapList(List<Map<String, Object>> readValue) {
        replaceBigDecimalByDouble(readValue);
        return new YTreeBuilder().value(readValue).build().asList();
    }

    private void replaceBigDecimalByDouble(List<Map<String, Object>> readValue) {
        for (Map<String, Object> value : readValue) {
            replaceBigDecimalByDouble(value);
        }
    }

    private void replaceBigDecimalByDouble(Map<String, Object> value) {
        if (value != null) {
            for (String key : value.keySet()) {
                Object obj = value.get(key);
                if (obj instanceof List) {
                    replaceBigDecimalByDouble((List<Map<String, Object>>) obj);
                } else if (obj instanceof Map) {
                    replaceBigDecimalByDouble((Map<String, Object>) obj);
                } else if (obj instanceof BigDecimal) {
                    value.put(key, ((BigDecimal) obj).doubleValue());
                }
            }
        }
    }


    @Override
    public void close() throws Exception {

    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public YTreeMapNode next() {
        return (YTreeMapNode) iterator.next();
    }
}
