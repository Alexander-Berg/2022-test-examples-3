package ru.yandex.direct.mysql.ytsync.synchronizator.tables;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.common.row.FlatRow;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TableWriteBufferTest {
    private static final String TABLE = "//foo/bar";
    private static final String TABLE_2 = "//foo/bar2";
    private static final TableSchema SCHEMA = new TableSchema.Builder()
            .addKeyExpression("__hash__", ColumnValueType.INT64, "int64(farm_hash(a, b))")
            .addKey("a", ColumnValueType.INT64)
            .addKey("b", ColumnValueType.INT64)
            .addValue("c", ColumnValueType.INT64)
            .build();

    private static final TableSchema SCHEMA_2 = new TableSchema.Builder()
            .addKeyExpression("__hash__", ColumnValueType.INT64, "int64(farm_hash(a, b))")
            .addKey("a", ColumnValueType.INT64)
            .addKey("b", ColumnValueType.INT64)
            .addValue("c", ColumnValueType.INT64)
            .addValue("d", ColumnValueType.INT64)
            .build();

    private static final YTreeNode A = YTree.builder().value(0).build();
    private static final YTreeNode B = YTree.builder().value(1).build();
    private static final YTreeNode C = YTree.builder().value(2).build();
    private static final YTreeNode D = YTree.builder().value(3).build();
    private static final YTreeNode E = YTree.builder().value(4).build();

    private final TableWriteBuffer buffer = new TableWriteBuffer(TABLE, SCHEMA);
    private final TableWriteBuffer buffer2 = new TableWriteBuffer(TABLE_2, SCHEMA_2);

    @Test
    public void simpleDelete() {
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(1));
        assertThat(buffer.addDelete(FlatRow.of(C, D)), is(1));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B),
                FlatRow.of(C, D)
        ));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void simpleInsert() {
        assertThat(buffer.addInsert(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(D, E, A)), is(1));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(A, B, C),
                FlatRow.of(D, E, A)
        ));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void simpleUpdate() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(D, E, A)), is(1));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(A, B, C),
                FlatRow.of(D, E, A)
        ));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void simpleUpdateRedundant() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C), FlatRow.of(null, null, C)), is(0));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void deleteThenInsert() {
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(1));
        assertThat(buffer.addDelete(FlatRow.of(B, C)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(A, B, C)), is(0));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(B, C)
        ));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(A, B, C)
        ));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void deleteThenUpdateIgnored() {
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(1));
        assertThat(buffer.addDelete(FlatRow.of(B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(0));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B),
                FlatRow.of(B, C)
        ));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void insertThenUpdate() {
        assertThat(buffer.addInsert(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(A, C, D)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, B, E)), is(0));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(A, B, E),
                FlatRow.of(A, C, D)
        ));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void insertThenDelete() {
        assertThat(buffer.addInsert(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(B, C, D)), is(1));
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(0));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(B, C, D)
        ));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void updateThenInsert() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, C, D)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(A, B, E)), is(0));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(A, C, D)
        ));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(A, B, E)
        ));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void updateThenUpdate() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, B, E)), is(0));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(A, B, E)
        ));
        assertThat(buffer.getDeletes(), is(empty()));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void updateThenDelete() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, C, D)), is(1));
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(0));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(A, C, D)
        ));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getInserts(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void simpleChangePrimaryKey() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B), FlatRow.of(B, D, null)), is(2));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(B, D, null)
        ));
        assertThat(buffer.getRemapping(), containsInAnyOrder(
                FlatRow.of(B, D, A, B)
        ));
        assertThat(buffer.getInserts(), is(empty()));
    }

    @Test
    public void insertThenChangePrimaryKey() {
        assertThat(buffer.addInsert(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, B), FlatRow.of(B, D, null)), is(1));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(B, D, C)
        ));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void updateThenChangePrimaryKey() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B, C)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(A, B), FlatRow.of(B, D, null)), is(1));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getUpdates(), containsInAnyOrder(
                FlatRow.of(B, D, C)
        ));
        assertThat(buffer.getRemapping(), containsInAnyOrder(
                FlatRow.of(B, D, A, B)
        ));
        assertThat(buffer.getInserts(), is(empty()));
    }

    @Test
    public void changePrimaryKeyThenInsert() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B), FlatRow.of(B, D, null)), is(2));
        assertThat(buffer.addInsert(FlatRow.of(B, D, E)), is(0));
        assertThat(buffer.getDeletes(), containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        assertThat(buffer.getInserts(), containsInAnyOrder(
                FlatRow.of(B, D, E)
        ));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void complexTransaction() {
        assertThat(buffer.addDelete(FlatRow.of(A, B)), is(1));
        assertThat(buffer.addInsert(FlatRow.of(B, C, D)), is(1));
        assertThat(buffer.addUpdate(FlatRow.of(C, D), FlatRow.of(D, E, null)), is(2));
        assertThat(buffer.addUpdate(FlatRow.of(E, E), FlatRow.of(A, A, null)), is(2));
        assertThat(buffer.addUpdate(FlatRow.of(E, D), FlatRow.of(D, D, null)), is(2));
        assertThat(buffer.addUpdate(FlatRow.of(D, D, D)), is(0));
        assertThat(buffer.addUpdate(FlatRow.of(E, A, B)), is(1));
        MockTransaction tx = new MockTransaction();
        MockTransaction.TableData data = tx.getTableData(TABLE);
        data.lookupData.put(FlatRow.of(C, D), FlatRow.of(C, D, B));
        buffer.apply(tx).join();

        // Вызов apply должен был сделать правильные вызовы
        MatcherAssert.assertThat(data.insertedRows, containsInAnyOrder(
                FlatRow.of(B, C, D),
                FlatRow.of(D, E, B),
                FlatRow.of(D, D, D)
        ));
        MatcherAssert.assertThat(data.updatedRows, containsInAnyOrder(
                FlatRow.of(E, A, B)
        ));
        MatcherAssert.assertThat(data.deletedKeys, containsInAnyOrder(
                FlatRow.of(A, B),
                FlatRow.of(C, D),
                FlatRow.of(E, D),
                FlatRow.of(E, E)
        ));

        // После успешного apply в буфере должны остаться данные до их очистки
        assertThat(buffer.getDeletes(), is(not(empty())));
        assertThat(buffer.getInserts(), is(not(empty())));
        assertThat(buffer.getUpdates(), is(not(empty())));
        assertThat(buffer.getRemapping(), is(empty()));
    }

    @Test
    public void remappingWithSubsequentPartialUpdate() {
        assertThat(buffer2.addUpdate(FlatRow.of(E, D), FlatRow.of(D, D, null, null)), is(2));
        assertThat(buffer2.addUpdate(FlatRow.of(D, D, null, D)), is(0));
        assertThat(buffer2.addUpdate(FlatRow.of(D, D), FlatRow.of(C, D, null, null)), is(1));
        assertThat(buffer2.addUpdate(FlatRow.of(C, D, null, B)), is(0));
        MockTransaction tx = new MockTransaction();
        MockTransaction.TableData data = tx.getTableData(TABLE_2);
        buffer2.apply(tx).join();

        // Вызов apply должен был сделать правильные вызовы
        MatcherAssert.assertThat(data.insertedRows, is(empty()));
        MatcherAssert.assertThat(data.updatedRows, containsInAnyOrder(
                FlatRow.of(C, D, null, B)
        ));
        MatcherAssert.assertThat(data.deletedKeys, containsInAnyOrder(
                FlatRow.of(E, D),
                FlatRow.of(D, D)
        ));

        // После успешного apply в буфере должны остаться данные до их очистки
        assertThat(buffer2.getDeletes(), is(not(empty())));
        assertThat(buffer2.getInserts(), is(empty()));
        assertThat(buffer2.getUpdates(), is(not(empty())));
        assertThat(buffer2.getRemapping(), is(empty()));
    }

    @Test
    public void updatePrimaryKeyFullRow() {
        assertThat(buffer.addUpdate(FlatRow.of(A, B), FlatRow.of(A, C, D)), is(2));
        MockTransaction tx = new MockTransaction();
        tx.getTableData(TABLE).lookupData.put(FlatRow.of(A, B), FlatRow.of(A, B, C));
        buffer.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        MatcherAssert.assertThat(tx.getTableData(TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(A, C, D)
        ));

        // После успешного apply в буфере должны остаться данные до их очистки
        assertThat(buffer.getDeletes(), is(not(empty())));
        assertThat(buffer.getInserts(), is(not(empty())));
        assertThat(buffer.getUpdates(), is(empty()));
        assertThat(buffer.getRemapping(), is(empty()));
    }
}
