package ru.yandex.autotests.market.stat.dictionaries_yt.dao;

import com.google.common.collect.Maps;
import com.hazelcast.util.StringUtil;
import ru.yandex.autotests.market.stat.date.DatePatterns;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;
import ru.yandex.autotests.market.stat.handlers.FieldsHandler;
import ru.yandex.autotests.market.stat.mappers.RowMapperUtils;
import ru.yandex.autotests.market.stat.util.ReflectionUtils;
import ru.yandex.autotests.market.stat.util.data.IgnoreField;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.ytree.YTreeBooleanNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeDoubleNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeIntegerNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.yandex.autotests.market.stat.util.ReflectionUtils.setField;

/**
 * Created by kateleb on 07.06.17.
 */
public class YtParseUtils {

    public static final Class<org.beanio.annotation.Field> ANNOTATION_CLASS = org.beanio.annotation.Field.class;
    public static final Class<IgnoreField> IGNORE_ANNOTATION_CLASS = IgnoreField.class;


    static <T extends DictionaryRecord> List<T> parseData(IteratorF<YTreeMapNode> ytreeIterator, Class recordClass) {
        List<T> list = new ArrayList<>();
        while (ytreeIterator.hasNext()) {
            Map<String, String> values = YtParseUtils.nodeToMapString(recordClass, ytreeIterator.next());
            T record = ReflectionUtils.instantiate((Class<T>) recordClass);
            list.add(parseFields(record, values));
        }
        return list;
    }

    private static Map<String, String> nodeToMapString(Class recordClass, YTreeNode node) {
        Map<String, Class> fieldTypes = Arrays.stream(recordClass.getDeclaredFields())
            .filter(f -> f.getAnnotation(IGNORE_ANNOTATION_CLASS) == null)
            .collect(toMap(
                field -> RowMapperUtils.getFieldName(field),
                Field::getType
            ));
        return node.asMap().entrySet().stream()
            .filter(en -> fieldTypes.get(en.getKey()) != null)
            .map(e -> Maps.immutableEntry(
                e.getKey(),
                valueToString(extractValueFromNode(e.getValue()), fieldTypes.get(e.getKey()))
            ))
            .filter(e -> e.getValue() != null)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Object processNull(Object it, Class fieldClass) {
        if (it == null || "#".equals(it.toString()) || isNullOfSpecificClasses(it, fieldClass)) {
            return null;
        }
        return it;
    }

    private static boolean isNullOfSpecificClasses(Object it, Class<?> fieldClass) {
        return "".equals(it.toString()) && fieldClass != null && (
            fieldClass.isAssignableFrom(Date.class) ||
                fieldClass.isAssignableFrom(Timestamp.class));
    }

    private static String valueToString(Object it, Class fieldClass) {
        return Optional.ofNullable(processNull(it, fieldClass)).map(Object::toString).orElse(null);
    }

    private static Object extractValueFromNode(YTreeNode e) {
        if (e instanceof YTreeStringNode) {
            return e.stringValue();
        }

        if (e instanceof YTreeDoubleNode) {
            return e.doubleValue();
        }

        if (e instanceof YTreeIntegerNode) {
            return e.longValue();
        }

        if (e instanceof YTreeBooleanNode) {
            return e.boolValue();
        }

        if (e instanceof YTreeListNode) {
            return e.asList().stream().map(YtParseUtils::extractValueFromNode).collect(toList());
        }

        return e.toString().equals("") ? null : e.toString();
    }

    private static <T extends DictionaryRecord> T parseFields(T record,
                                                              Map<String, String> values) {
        checkNotNull(record);
        checkNotNull(values);
        Arrays.stream(record.getClass().getDeclaredFields())
            .filter(f -> f.getAnnotation(IGNORE_ANNOTATION_CLASS) == null)
            .forEach(
                field -> {
                    String sourceValue = values.get(RowMapperUtils.getFieldName(field));
                    setFieldValue(record, field, sourceValue);
                });
        return record;
    }


    private static <R extends DictionaryRecord> void setFieldValue(R record, Field field, String sourceValue) {
        if (StringUtil.isNullOrEmpty(sourceValue)) {
            setField(field, record, null);
            return;
        }

        if ((field.getAnnotation(ANNOTATION_CLASS) != null) && (!field.getAnnotation(ANNOTATION_CLASS).handlerName().isEmpty())) {
            Object objectValue;
            FieldsHandler handler = FieldsHandler.getByName(field.getAnnotation(ANNOTATION_CLASS).handlerName());

            objectValue = handler.parse(sourceValue);

            if (objectValue != null) {
                setField(field, record, objectValue);
                return;
            }
        }

        if (field.getType().isAssignableFrom(String.class)) {
            setField(field, record, sourceValue);
        } else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
            LocalDateTime value = DatePatterns.parseByFirstMatchingPattern(sourceValue);
            setField(field, record, value);
        } else if (field.getType().isAssignableFrom(Boolean.class) || field.getType().isAssignableFrom(boolean.class)) {
            setField(field, record, Boolean.valueOf(sourceValue));
        } else if (field.getType().isAssignableFrom(Long.class)) {
            setField(field, record, Long.valueOf(sourceValue));
        } else {
            throw new RuntimeException("I'm not prepared to type " + field.getType());
        }
    }

}
