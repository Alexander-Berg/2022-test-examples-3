package ru.yandex.crypta.graph2.dao.yt.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.BenderYtSchemaExtractor;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.ProtobufYtSchemaExtractor;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.YTreeYtSchemaExtractor;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.YtSchemaExtractor;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeFlattenField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderFlatten;
import ru.yandex.misc.bender.annotation.BenderPart;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.junit.Assert.assertEquals;


public class YtSchemaExtractorTest {

    private static final TableSchema EXPECTED_SCHEMA_FOR_ENTITY_1 = TableSchema.builder()
            .setUniqueKeys(false)
            .setStrict(true)
            .addValue("simpleField", ColumnValueType.INT64)
            .addValue("sameColumn", ColumnValueType.STRING)
            .addValue("nested_field", ColumnValueType.STRING)
            .build();

    private static final TableSchema EXPECTED_UNION_SCHEMA = TableSchema.builder()
            .setUniqueKeys(false)
            .setStrict(false)
            .addValue("nested_field", ColumnValueType.STRING)
            .addValue("simpleField2", ColumnValueType.DOUBLE)
            .addValue("sameColumn", ColumnValueType.STRING)
            .addValue("simpleField", ColumnValueType.INT64)
            .build();

    private static final TableSchema EXPECTED_SORTED_SCHEMA_1 = TableSchema.builder()
            .setUniqueKeys(false)
            .setStrict(true)
            .addKey("nested_field", ColumnValueType.STRING)
            .addValue("simpleField", ColumnValueType.INT64)
            .addValue("sameColumn", ColumnValueType.STRING)
            .build();

    private static final TableSchema EXPECTED_SORTED_SCHEMA_2 = TableSchema.builder()
            .setUniqueKeys(false)
            .setStrict(true)
            .addKey("simpleField", ColumnValueType.INT64)
            .addValue("nested_field", ColumnValueType.STRING)
            .addValue("sameColumn", ColumnValueType.STRING)
            .build();

    private TableSchema canonizeSchema(TableSchema schema) {
        List<ColumnSchema> columns = schema.getColumns();
        List<ColumnSchema> canonizedColumns = new ArrayList<>();
        columns.stream()
                .filter(c -> c.getSortOrder() != null)
                .forEachOrdered(canonizedColumns::add);
        columns.stream()
                .filter(c -> c.getSortOrder() == null)
                .sorted(Comparator.comparing(ColumnSchema::getName))
                .forEachOrdered(canonizedColumns::add);
        return schema.toBuilder()
                .setColumns(canonizedColumns)
                .build();
    }

    @Test
    public void testBenderYtSchemaGenerator() {
        YtSchemaExtractor gen = new BenderYtSchemaExtractor();
        TableSchema schema = gen.extractTableSchema(Entity1.class);

        assertEquals(canonizeSchema(EXPECTED_SCHEMA_FOR_ENTITY_1), canonizeSchema(schema));
    }

    @Test
    public void testBenderYtSchemaGeneratorUnionSchema() {
        YtSchemaExtractor gen = new BenderYtSchemaExtractor();
        TableSchema schema = gen.extractUnionTableSchema(Cf.list(Entity1.class, Entity2.class, Entity3.class));
        schema = schema.toBuilder().setStrict(false).build();

        assertEquals(canonizeSchema(EXPECTED_UNION_SCHEMA), canonizeSchema(schema));
    }

    @Test
    public void testYsonYtSchemaGenerator() {
        YtSchemaExtractor gen = new YTreeYtSchemaExtractor();
        TableSchema schema = gen.extractTableSchema(Entity1.class);

        assertEquals(canonizeSchema(EXPECTED_SCHEMA_FOR_ENTITY_1), canonizeSchema(schema));
    }

    @Test
    public void testYsonYtSchemaGeneratorUnionSchema() {
        YtSchemaExtractor gen = new YTreeYtSchemaExtractor();
        TableSchema schema = gen.extractUnionTableSchema(Cf.list(Entity1.class, Entity2.class, Entity3.class));
        schema = schema.toBuilder().setStrict(false).build();

        assertEquals(canonizeSchema(EXPECTED_UNION_SCHEMA), canonizeSchema(schema));
    }

    @Test
    public void testProtoYtSchemaGenerator() {
        YtSchemaExtractor gen = new ProtobufYtSchemaExtractor();
        TableSchema schema = gen.extractTableSchema(YtSchemaExtractorTestProtos.ProtoEntity1.class);

        assertEquals(canonizeSchema(EXPECTED_SCHEMA_FOR_ENTITY_1), canonizeSchema(schema));
    }

    @Test
    public void testProtoYtSchemaGeneratorUnionSchema() {
        YtSchemaExtractor gen = new ProtobufYtSchemaExtractor();
        TableSchema schema = gen.extractUnionTableSchema(Cf.list(YtSchemaExtractorTestProtos.ProtoEntity1.class,
                YtSchemaExtractorTestProtos.ProtoEntity3.class));
        schema = schema.toBuilder().setStrict(false).build();

        assertEquals(canonizeSchema(EXPECTED_UNION_SCHEMA), canonizeSchema(schema));
    }

    @Test
    public void testKeyColumn() {
        YtSchemaExtractor gen = new ProtobufYtSchemaExtractor();
        TableSchema schema = gen.extractUnionTableSchema(Cf.list(YtSchemaExtractorTestProtos.ProtoEntity1.class));

        TableSchema sortedSchema = schema.toBuilder().sortBy(List.of("nested_field")).build();
        assertEquals(canonizeSchema(EXPECTED_SORTED_SCHEMA_1), canonizeSchema(sortedSchema));

        TableSchema sortedSchema2 = schema.toBuilder().sortBy(List.of("simpleField")).build();
        assertEquals(canonizeSchema(EXPECTED_SORTED_SCHEMA_2), canonizeSchema(sortedSchema2));
    }

    @Test
    public void testUnicode() {
        String a = "��2pr!�>�ϸ�psy͉o��j<'�v����?g@yandex.ru";
        String b = "��2pr!�>�ϸ�psy͉o��j<'�v����?g@yandex.ru";
        assertEquals(a, b);
    }

    @Bendable
    @YTreeObject
    private static class Entity1 {
        @BenderPart
        @YTreeField
        @SuppressWarnings("UnusedVariable")
        private int simpleField;
        @BenderPart
        @YTreeField
        @SuppressWarnings("UnusedVariable")
        private String sameColumn;
        @BenderFlatten
        @YTreeField
        @YTreeFlattenField
        @SuppressWarnings("UnusedVariable")
        private Entity2 flattenEntity;
    }

    @Bendable
    @YTreeObject
    private static class Entity2 {
        @BenderPart(name = "nested_field")
        @YTreeField(key = "nested_field")
        @SuppressWarnings("UnusedVariable")
        private String nestedField;
    }

    @Bendable
    @YTreeObject
    private static class Entity3 {
        @BenderPart
        @YTreeField
        @SuppressWarnings("UnusedVariable")
        private double simpleField2;
        @BenderPart
        @YTreeField
        @SuppressWarnings("UnusedVariable")
        private String sameColumn;
    }
}
