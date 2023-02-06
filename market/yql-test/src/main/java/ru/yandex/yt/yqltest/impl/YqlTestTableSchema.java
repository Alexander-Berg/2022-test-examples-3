package ru.yandex.yt.yqltest.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.type_info.TiType;
import ru.yandex.type_info.TypeName;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * Schema in format "name:type".
 *
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YqlTestTableSchema {
    private final List<String> schema = new ArrayList<>();

    public List<String> getSchema() {
        return schema;
    }

    public Map<String, TiType> buildSchemaMap() {
        Map<String, TiType> result = new HashMap<>();
        for (String part : schema) {
            String[] split = part.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid record definition");
            }
            String typeName = split[1];
            result.put(split[0], getYtTypeByName(typeName));
        }
        return result;
    }

    public TableSchema buildYtSchema() {
        Map<String, TiType> schemaMap = buildSchemaMap();

        TableSchema.Builder builder = TableSchema.builder().setUniqueKeys(false);
        schemaMap.forEach((name, type) -> builder.addValue(name, TiType.optional(type)));
        return builder.build();
    }

    public static TiType getYtTypeByName(String name) {
        TypeName typeName = TypeName.fromWireName(name);

        // need to use this hack for some types :(
        // most of types have method with name == type_name. But some are not
        switch (typeName) {
            case Double:
                return TiType.doubleType();
            case Float:
                return TiType.floatType();
            default: {
                try {
                    return (TiType) TiType.class.getDeclaredMethod(typeName.getWireName()).invoke(null);
                } catch (Exception e) {
                    throw new RuntimeException("Can't find yt type by name: " + name, e);
                }
            }
        }
    }


}
