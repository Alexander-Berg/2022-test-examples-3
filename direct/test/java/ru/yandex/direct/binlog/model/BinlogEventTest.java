package ru.yandex.direct.binlog.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
public class BinlogEventTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    /**
     * Метод validate должен падать при неправильных полях и не должен падать на правильном объекте.
     */
    @Test
    public void testValidate() {
        ImmutableMap<String, Consumer<BinlogEvent>> requiredSetters =
                ImmutableMap.<String, Consumer<BinlogEvent>>builder()
                        .put("utcTimestamp", e -> e.setUtcTimestamp(LocalDateTime.now(ZoneId.of("UTC"))))
                        .put("source", e -> e.setSource("ppc:1"))
                        .put("serverUuid", e -> e.setServerUuid(UUID.randomUUID().toString()))
                        .put("transactionId", e -> e.setTransactionId(123L))
                        .put("queryIndex", e -> e.setQueryIndex(456))
                        .put("db", e -> e.setDb("ppc"))
                        .put("table", e -> e.setTable("table"))
                        .put("operation", e -> e.setOperation(Operation.INSERT))
                        .build();

        BinlogEvent goodEvent = new BinlogEvent()
                .withAddedRows(new BinlogEvent.Row()
                        .withRowIndex(789)
                        .withPrimaryKey(Map.of("foo", "bar"))
                        .withBefore(Map.of())
                        .withAfter(Map.of("foo", "bar")));
        requiredSetters.values().forEach(c -> c.accept(goodEvent));
        softly.assertThatCode(goodEvent::validate)
                .describedAs("When all fields are set, no exception expected")
                .doesNotThrowAnyException();

        for (String skippedField : requiredSetters.keySet()) {
            BinlogEvent badEvent = new BinlogEvent();
            requiredSetters.forEach((field, consumer) -> {
                if (!skippedField.equals(field)) {
                    consumer.accept(badEvent);
                }
            });
            softly.assertThatCode(badEvent::validate)
                    .describedAs("When " + skippedField + " is not set, exception is expected")
                    .hasMessage(skippedField + " should not be null");
        }
    }

    /**
     * В зависимости от {@link BinlogEvent#getOperation()} должны быть указаны либо
     * {@link BinlogEvent#getRows()}, либо {@link BinlogEvent#getSchemaChanges()}.
     */
    @Test
    public void testOperationRowSchema() {
        Supplier<BinlogEvent> partOfEvent = () -> new BinlogEvent()
                .withUtcTimestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .withSource("ppc:1")
                .withServerUuid(UUID.randomUUID().toString())
                .withTransactionId(123L)
                .withQueryIndex(456)
                .withDb("ppc")
                .withTable("table");

        softly.assertThatCode(partOfEvent.get().withOperation(Operation.INSERT)::validate)
                .hasMessage("When operation = INSERT rows should not be empty");
        softly.assertThatCode(partOfEvent.get().withOperation(Operation.UPDATE)::validate)
                .hasMessage("When operation = UPDATE rows should not be empty");
        softly.assertThatCode(partOfEvent.get().withOperation(Operation.DELETE)::validate)
                .hasMessage("When operation = DELETE rows should not be empty");
        softly.assertThatCode(partOfEvent.get().withOperation(Operation.SCHEMA)::validate)
                .hasMessage("When operation = SCHEMA schemaChanges should not be empty");

        softly.assertThatCode(
                partOfEvent.get()
                        .withOperation(Operation.INSERT)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(789)
                                .withPrimaryKey(Map.of("foo", "bar"))
                                .withBefore(Map.of())
                                .withAfter(Map.of(
                                        "foo", "bar",
                                        "hurr", "durr")))
                        ::validate)
                .describedAs("Valid operation = INSERT")
                .doesNotThrowAnyException();
        softly.assertThatCode(
                partOfEvent.get()
                        .withOperation(Operation.UPDATE)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(789)
                                .withPrimaryKey(Map.of("foo", "bar"))
                                .withBefore(Map.of())
                                .withAfter(Map.of("hurr", "durr")))
                        ::validate)
                .describedAs("Valid operation = UPDATE")
                .doesNotThrowAnyException();
        softly.assertThatCode(
                partOfEvent.get()
                        .withOperation(Operation.DELETE)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(789)
                                .withPrimaryKey(Map.of("foo", "bar"))
                                .withBefore(Map.of())
                                .withAfter(Map.of()))
                        ::validate)
                .describedAs("Valid operation = DELETE")
                .doesNotThrowAnyException();
        softly.assertThatCode(
                partOfEvent.get()
                        .withOperation(Operation.SCHEMA)
                        .withAddedSchemaChanges(new DropTable("table"))
                        ::validate)
                .describedAs("Valid operation = SCHEMA")
                .doesNotThrowAnyException();

        BinlogEvent bothRowsAndSchemaChanges = partOfEvent.get()
                .withAddedRows(new BinlogEvent.Row()
                        .withRowIndex(789)
                        .withPrimaryKey(Map.of("foo", "bar"))
                        .withAfter(Map.of(
                                "foo", "bar",
                                "hurr", "durr")))
                .withAddedSchemaChanges(new DropTable("table"));
        softly.assertThatCode(bothRowsAndSchemaChanges.withOperation(Operation.INSERT)::validate)
                .hasMessage("When operation = INSERT schemaChanges should be empty");
        softly.assertThatCode(bothRowsAndSchemaChanges.withOperation(Operation.UPDATE)::validate)
                .hasMessage("When operation = UPDATE schemaChanges should be empty");
        softly.assertThatCode(bothRowsAndSchemaChanges.withOperation(Operation.DELETE)::validate)
                .hasMessage("When operation = DELETE schemaChanges should be empty");
        softly.assertThatCode(bothRowsAndSchemaChanges.withOperation(Operation.SCHEMA)::validate)
                .hasMessage("When operation = SCHEMA rows should be empty");
    }

    @Test
    public void testValidateRow() {
        BinlogEvent event = new BinlogEvent()
                .withUtcTimestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .withSource("ppc:1")
                .withServerUuid(UUID.randomUUID().toString())
                .withTransactionId(123L)
                .withQueryIndex(456)
                .withDb("ppc")
                .withTable("table")
                .withOperation(Operation.INSERT);

        ImmutableMap<String, Consumer<BinlogEvent.Row>> requiredSetters =
                ImmutableMap.<String, Consumer<BinlogEvent.Row>>builder()
                        .put("rowIndex", r -> r.setRowIndex(789))
                        .put("primaryKey", r -> r.setPrimaryKey(Map.of("foo", "bar")))
                        .put("before", r -> r.setBefore(Map.of()))
                        .put("after", r -> r.setAfter(Map.of("foo", "bar")))
                        .build();

        BinlogEvent.Row goodRow = new BinlogEvent.Row();
        requiredSetters.values().forEach(c -> c.accept(goodRow));
        softly.assertThatCode(goodRow::validate)
                .describedAs("When all fields are set, no exception expected")
                .doesNotThrowAnyException();

        event.addRows(goodRow);
        softly.assertThatCode(event::validate)
                .describedAs("When all fields are set, no exception expected (validating from event)")
                .doesNotThrowAnyException();

        for (String skippedField : requiredSetters.keySet()) {
            BinlogEvent.Row badRow = new BinlogEvent.Row();
            requiredSetters.forEach((field, consumer) -> {
                if (!skippedField.equals(field)) {
                    consumer.accept(badRow);
                }
            });
            softly.assertThatCode(badRow::validate)
                    .describedAs("When " + skippedField + " is not set, exception is expected")
                    .hasMessage(skippedField + " should not be null");

            event.withRows(ImmutableList.of(badRow));
            softly.assertThatCode(event::validate)
                    .describedAs("When " + skippedField + " is not set, exception is expected (validating from event)")
                    .hasMessage("rows[0]: " + skippedField + " should not be null");
        }

        BinlogEvent.Row notBadRow = new BinlogEvent.Row()
                .withRowIndex(789)
                .withPrimaryKey(Map.of())
                .withBefore(Map.of())
                .withAfter(Map.of("foo", "bar"));
        softly.assertThatCode(notBadRow::validate)
                .describedAs("When primaryKey is empty, exception is not expected (DIRECT-81764)")
                .doesNotThrowAnyException();

        event.withRows(ImmutableList.of(goodRow, notBadRow));
        softly.assertThatCode(event::validate)
                .describedAs("When primaryKey is empty, exception is not expected (DIRECT-81764)")
                .doesNotThrowAnyException();
    }

    /**
     * Объект после сериализации и последующей десериализации должен быть эквивалентен исходному.
     * Версия для operation ∈ {INSERT, UPDATE, DELETE}.
     */
    @Test
    public void toAndFromProtobufDml() {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withUtcTimestamp(LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS))
                .withSource("ppc:1")
                .withServerUuid(UUID.randomUUID().toString())
                .withTransactionId(123L)
                .withQueryIndex(456)
                .withDb("ppc")
                .withTable("table")
                .withOperation(Operation.INSERT)
                .withAddedRows(
                        new BinlogEvent.Row()
                                .withRowIndex(789)
                                .withPrimaryKey(Map.of("foo", "bar"))
                                .withBefore(Map.of("foo", "bar"))
                                .withAfter(Map.of("foo", "bar")));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf())).isEqualTo(binlogEvent);

        binlogEvent.setOperation(Operation.UPDATE);
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf())).isEqualTo(binlogEvent);

        binlogEvent.setOperation(Operation.DELETE);
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf())).isEqualTo(binlogEvent);

        binlogEvent.addRows(new BinlogEvent.Row()
                .withRowIndex(777)
                .withPrimaryKey(Map.of("hurr", "durr"))
                .withBefore(Map.of("hurr", "durr"))
                .withAfter(Map.of("hurr", "durr")));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf())).isEqualTo(binlogEvent);
    }

    /**
     * Объект после сериализации и последующей десериализации должен быть эквивалентен исходному.
     * Версия для operation = SCHEMA.
     */
    @Test
    public void toAndFromProtobufDdl() {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withUtcTimestamp(LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS))
                .withSource("ppc:1")
                .withServerUuid(UUID.randomUUID().toString())
                .withTransactionId(123L)
                .withQueryIndex(456)
                .withDb("ppc")
                .withTable("table")
                .withOperation(Operation.SCHEMA)
                .withAddedSchemaChanges(new CreateTable()
                        .withAddedColumns(
                                new CreateOrModifyColumn()
                                        .withColumnName("foo")
                                        .withColumnType(ColumnType.INTEGER)
                                        .withNullable(false),
                                new CreateOrModifyColumn()
                                        .withColumnName("bar")
                                        .withColumnType(ColumnType.INTEGER)
                                        .withNullable(false),
                                new CreateOrModifyColumn()
                                        .withColumnName("baz")
                                        .withColumnType(ColumnType.FIXED_POINT)
                                        .withNullable(true))
                        .withPrimaryKey(ImmutableList.of("foo", "bar")))
                .validate();
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("CreateTable")
                .isEqualTo(binlogEvent);

        binlogEvent.withSchemaChanges(ImmutableList.of(
                new Truncate()));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("Truncate")
                .isEqualTo(binlogEvent);

        binlogEvent.withSchemaChanges(ImmutableList.of(
                new CreateOrModifyColumn()
                        .withColumnName("baz")
                        .withColumnType(ColumnType.FIXED_POINT)
                        .withNullable(false),
                new CreateOrModifyColumn()
                        .withColumnName("baaaz")
                        .withColumnType(ColumnType.STRING)
                        .withNullable(true)));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("CreateOrModifyColumn")
                .isEqualTo(binlogEvent);

        binlogEvent.withSchemaChanges(ImmutableList.of(new DropColumn()
                .withColumnName("old_column")));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("DropColumn")
                .isEqualTo(binlogEvent);

        binlogEvent.withSchemaChanges(ImmutableList.of(new RenameColumn()
                .withOldColumnName("baz")
                .withNewColumnName("buzz")));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("RenameColumn")
                .isEqualTo(binlogEvent);

        binlogEvent.withSchemaChanges(ImmutableList.of(new DropTable("xxx")));
        softly.assertThat(BinlogEvent.fromProtobuf(binlogEvent.toProtobuf()))
                .describedAs("DropTable")
                .isEqualTo(binlogEvent);
    }

    @Test
    public void testFromTemplateEmptyEvent() {
        final BinlogEvent emptyEvent = new BinlogEvent();
        assertEquals(emptyEvent, BinlogEvent.fromTemplate(emptyEvent));
    }

    @Test
    public void testFromTemplateWithoutRowsOrSchemaChanges() {
        final BinlogEvent event = new BinlogEvent()
                .withSource("source")
                .withDb("db")
                .withUtcTimestamp(LocalDateTime.now())
                .withTable("table")
                .withTransactionId(12345L)
                .withQueryIndex(54321)
                .withServerUuid("server-uuid")
                .withOperation(Operation.UPDATE);
        assertEquals(event, BinlogEvent.fromTemplate(event));
    }

    @Test
    public void testFromTemplateWithRowsOrSchemaChanges() {
        BinlogEvent event = new BinlogEvent()
                .withSource("source")
                .withDb("db")
                .withUtcTimestamp(LocalDateTime.now())
                .withTable("table")
                .withTransactionId(12345L)
                .withQueryIndex(54321)
                .withServerUuid("server-uuid")
                .withOperation(Operation.UPDATE)
                .withAddedRows(
                        new BinlogEvent.Row()
                                .withRowIndex(789)
                                .withPrimaryKey(Map.of("abc", "123"))
                                .withBefore(Map.of("cde", "345"))
                                .withAfter(Map.of("cde", "456"))
                )
                .validate();
        BinlogEvent copy = BinlogEvent.fromTemplate(event);
        softly.assertThat(copy.getRows()).isEmpty();
        softly.assertThat(event).isNotEqualTo(copy);
        softly.assertThat(copy).isEqualTo(event.withRows(Collections.emptyList()));

        event = event
                .withOperation(Operation.SCHEMA)
                .withAddedSchemaChanges(
                        new CreateOrModifyColumn()
                                .withColumnName("xxx")
                                .withColumnType(ColumnType.BYTES)
                                .withNullable(true),
                        new CreateOrModifyColumn()
                                .withColumnName("zzz")
                                .withColumnType(ColumnType.STRING)
                                .withNullable(false)
                )
                .validate();
        copy = BinlogEvent.fromTemplate(event);
        softly.assertThat(copy.getRows()).isEmpty();
        softly.assertThat(event).isNotEqualTo(copy);
        softly.assertThat(copy).isEqualTo(event.withSchemaChanges(Collections.emptyList()));
    }
}
