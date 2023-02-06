package ru.yandex.market.yql_test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

public class YtValueParser {

    private final Map<String, YtColumnDefinition> definitionsMap;

    public YtValueParser(List<YtColumnDefinition> definitions) {
        definitionsMap = definitions.stream()
                .collect(Collectors.toMap(YtColumnDefinition::getName, Function.identity()));
    }

    public Object parse(String columnName, String strValue) {
        YtColumnDefinition definition = definitionsMap.get(columnName);
        if (definition == null) {
            throw new IllegalStateException("No def in schema for column " + columnName);
        }
        if (strValue == null) {
            return null;
        }
        YtMetaType metaType = definition.getMetaType();
        YtType type = definition.getType();
        switch (metaType) {
            case NOT:
            case OPTIONAL:
                return parseByTypeV3(strValue, type);
            case LIST:
                return parseList(strValue, type);
            default:
                throw new IllegalStateException("Not supported meta type v3: " + type);
        }
    }

    private static Object parseByTypeV3(String strValue, YtType type) {
        switch (type) {
            case STRING:
            case UTF8:
                return strValue;
            case INT32:
                return Integer.parseInt(strValue);
            case INT64:
                return Long.parseLong(strValue);
            case UINT32:
            case UINT64:
                return Long.parseUnsignedLong(strValue);
            case DOUBLE:
                return Double.parseDouble(strValue);
            case BOOL:
                return Boolean.parseBoolean(strValue);
            case YSON:
                return json2yson(strValue);
            default:
                throw new IllegalStateException("Not supported type v3: " + type);
        }
    }

    private static YTreeNode json2yson(String json) {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
        try {
            JsonNode jsonNode = new ObjectMapper(f).readTree(json);
            return YtUtils.json2yson(YTree.builder(), jsonNode)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> parseList(String strList, YtType type) {
        return Arrays.stream(strList.split(","))
                .map(str -> parseByTypeV3(str, type)).collect(Collectors.toList());
    }
}
