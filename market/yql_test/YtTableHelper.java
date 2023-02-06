package ru.yandex.market.yql_test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.jetbrains.annotations.NotNull;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.yql_test.service.QryTblTestService;

/**
 * Class for:
 * 1. Creation of YT static tables with schemas
 * 2. Filling tables
 */
public class YtTableHelper {

    private final Yt yt;
    private final QryTblTestService qryTblTestService;
    private boolean used = false;

    public YtTableHelper(Yt yt, QryTblTestService qryTblTestService) {
        this.yt = yt;
        this.qryTblTestService = qryTblTestService;
    }

    public void createTable(String schemaName, String schema, ITable data) {
        used = true;
        List<YtColumnDefinition> definitions = parseColumnDefinitions(schema);
        try {
            createTable(data, definitions);
        } catch (DataSetException e) {
            throw new IllegalStateException("Unable to create/fill table rows into " + schemaName, e);
        }
    }

    public void cleanTestDir() {
        if (yt.cypress().exists(qryTblTestService.getTestDir())) {
            yt.cypress().remove(qryTblTestService.getTestDir());
        }
    }

    public boolean isUsed() {
        return used;
    }

    private List<YtColumnDefinition> parseColumnDefinitions(String schema) {
        schema = schema.substring(schema.indexOf("["));
        schema = schema.replace(";", ",")
                .replaceAll("%false", "false")
                .replaceAll("%true", "true")
                .replaceAll("=", ":");

        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
        ArrayNode columns;
        try {
            columns = (ArrayNode) new ObjectMapper(f).readTree(schema);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return StreamSupport.stream(columns.spliterator(), false)
                .map(column -> new YtColumnDefinition(
                    column.get("name").asText(),
                    getTypeV3(column),
                    getMetaTypeV3(column),
                    YtUtils.json2yson(YTree.builder(), column).build()))
                .collect(Collectors.toList());
    }

    private YtType getTypeV3(JsonNode column) {
        JsonNode type = column.get("type_v3");
        return YtType.of(type.isContainerNode() ? type.get("item").asText() : type.asText());
    }

    private YtMetaType getMetaTypeV3(JsonNode column) {
        JsonNode type = column.get("type_v3");
        return type.isContainerNode() ? YtMetaType.of(type.get("type_name").asText()) : YtMetaType.NOT;
    }

    private void createTable(ITable data, List<YtColumnDefinition> definitions) throws DataSetException {
        YtValueParser valueParser = new YtValueParser(definitions);
        Column[] columns = data.getTableMetaData().getColumns();
        String tableName = qryTblTestService.processTableName(data.getTableMetaData().getTableName());
        YPath tablePath = YPath.simple(tableName);
        recreateTable(tablePath);
        setTableSchema(tablePath, toYtTableSchema(definitions));
        yt.tables().write(
                tablePath,
                YTableEntryTypes.YSON,
                toTreeNodes(data, valueParser, columns)
        );
    }

    @NotNull
    private List<YTreeMapNode> toTreeNodes(ITable data, YtValueParser valueParser, Column[] columns)
            throws DataSetException {
        List<YTreeMapNode> rows = new ArrayList<>(data.getRowCount());
        for (int i = 0; i < data.getRowCount(); i++) {
            YTreeBuilder row = YTree.mapBuilder();
            for (Column column : columns) {
                String columnName = column.getColumnName();
                String strValue = (String) data.getValue(i, columnName);
                Object value = valueParser.parse(columnName, strValue);
                row
                        .key(columnName)
                        .value(value);
            }
            rows.add(row.buildMap());
        }
        return rows;
    }

    private void recreateTable(YPath path) {
        yt.cypress().remove(path);
        yt.cypress().create(path, CypressNodeType.TABLE, true);
    }

    private void setTableSchema(YPath path, YTreeListNode defs) {
        yt.tables().alterTable(
                path,
                Optional.of(false),
                Optional.of(defs)
        );
    }

    private YTreeListNode toYtTableSchema(List<YtColumnDefinition> ytColumnDefinitions) {
        YTreeBuilder builder = YTree.listBuilder();
        ytColumnDefinitions.forEach(d -> builder.value(d.getYTreeNode()));
        return builder.buildList();
    }
}
