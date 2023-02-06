package ru.yandex.direct.intapi.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import ru.yandex.direct.ytwrapper.YtUtils;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static ru.yandex.inside.yt.kosher.cypress.CypressNodeType.TABLE;

public class TablesUtils {

    private TablesUtils() {
    }

    public static String generatePrefix() {
        return UUID.randomUUID().toString();
    }

    /**
     * Создаёт динамическую таблицу
     */
    public static void createTable(
            YtOperator ytOperator,
            Optional<GUID> transactionId,
            YPath path,
            List<ColumnInfo> columns
    ) {
        Map<String, YTreeNode> attrs = Map.of(
                "dynamic", YTree.booleanNode(true),
                YtUtils.SCHEMA_ATTR, buildSchema(columns, true)
        );
        ytOperator.getYt().cypress().create(transactionId, true, path, TABLE, true, false, false, attrs);
        ytOperator.mount(path, 30_000);
    }

    /**
     * Возвращает схему для таблицы
     *
     * @param columns список колонок таблицы
     * @param sorted  true, если нужна сортированная таблица
     */
    private static YTreeNode buildSchema(List<ColumnInfo> columns, boolean sorted) {
        YTreeBuilder schemaBuilder = YTree.listBuilder();
        for (ColumnInfo column : columns) {
            addColumn(schemaBuilder, column.name, column.type, column.expression, column.isKey && sorted);
        }
        return YTree.builder()
                .beginAttributes()
                .key("strict").value(true)
                .key("unique_keys").value(sorted)
                .endAttributes()
                .value(schemaBuilder.buildList())
                .build();
    }

    private static void addColumn(YTreeBuilder builder, String name, String type,
                                  @Nullable String expression, boolean sorted) {
        builder.beginMap()
                .key("name").value(name)
                .key("type").value(type);
        if (expression != null) {
            builder.key("expression").value(expression);
        }
        if (sorted) {
            builder.key("sort_order").value("ascending");
        }
        builder.key("required").value(false);
        builder.endMap();
    }
}
