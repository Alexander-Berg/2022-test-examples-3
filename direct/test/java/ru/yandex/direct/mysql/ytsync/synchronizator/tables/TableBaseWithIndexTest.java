package ru.yandex.direct.mysql.ytsync.synchronizator.tables;

import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
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

public class TableBaseWithIndexTest {
    private static final String MAIN_TABLE = "//foo/bar";
    private static final TableSchema MAIN_SCHEMA = new TableSchema.Builder()
            .addKeyExpression("__hash__", ColumnValueType.INT64, "int64(farm_hash(a, b))")
            .addKey("a", ColumnValueType.INT64)
            .addKey("b", ColumnValueType.INT64)
            .addValue("c", ColumnValueType.INT64)
            .build();

    private static final String INDEX_TABLE = "//foo/bar.index";
    private static final TableSchema INDEX_SCHEMA = new TableSchema.Builder()
            .addKey("b", ColumnValueType.INT64)
            .addValue("a", ColumnValueType.INT64)
            .build();

    private final TableBase indexTable =
            new TableBase(null, INDEX_TABLE, INDEX_SCHEMA);
    private final TableBaseWithIndex mainTable =
            new TableBaseWithIndex(null, MAIN_TABLE, MAIN_SCHEMA, indexTable);

    private static final YTreeNode A = YTree.builder().value(0).build();
    private static final YTreeNode B = YTree.builder().value(1).build();
    private static final YTreeNode C = YTree.builder().value(2).build();
    private static final YTreeNode D = YTree.builder().value(3).build();
    private static final YTreeNode E = YTree.builder().value(4).build();
    private static final YTreeNode F = YTree.builder().value(5).build();

    @Test
    public void insertIsIndexed() {
        mainTable.addInsert(FlatRow.of(A, B, C));
        MockTransaction tx = new MockTransaction();
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(A, B, C)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(B, A)
        ));
    }

    @Test
    public void deleteIsIndexed() {
        mainTable.addDelete(FlatRow.of(A, B));
        MockTransaction tx = new MockTransaction();
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(B)
        ));
    }

    @Test
    public void partialUpdateReadsIndex() {
        mainTable.addUpdate(FlatRow.of(null, B, C));
        MockTransaction tx = new MockTransaction();
        tx.getTableData(INDEX_TABLE).lookupData.put(FlatRow.of(B), FlatRow.of(B, A));
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).updatedRows, containsInAnyOrder(
                FlatRow.of(A, B, C)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).deletedKeys, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).insertedRows, is(empty()));
    }

    @Test
    public void partialUpdateMissingKey() {
        mainTable.addUpdate(FlatRow.of(null, B, C));
        MockTransaction tx = new MockTransaction();
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).updatedRows, is(empty()));
    }

    @Test
    public void partialUpdateThenFullKey() {
        mainTable.addUpdate(FlatRow.of(null, B, C));
        mainTable.addUpdate(FlatRow.of(A, B, D));
        assertThat(mainTable.getIncompleteOps().keySet(), is(empty()));
        MockTransaction tx = new MockTransaction();
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).updatedRows, containsInAnyOrder(
                FlatRow.of(A, B, D)
        ));
    }

    @Test
    public void partialUpdatedPrimaryKeyChain() {
        mainTable.addUpdate(FlatRow.of(null, B, null), FlatRow.of(null, C, null));
        mainTable.addUpdate(FlatRow.of(null, C, null), FlatRow.of(null, D, null));
        MockTransaction tx = new MockTransaction();
        tx.getTableData(MAIN_TABLE).lookupData.put(FlatRow.of(A, B), FlatRow.of(A, B, E));
        tx.getTableData(INDEX_TABLE).lookupData.put(FlatRow.of(B), FlatRow.of(B, A));
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(A, B),
                FlatRow.of(A, C)
        ));
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(A, D, E)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(B),
                FlatRow.of(C)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(D, A)
        ));
    }

    @Test
    @Ignore("Тест не проходит: отсутствует блокировка операций над затронутыми ранее неизвестными ключами")
    public void partialUpdatedPrimaryKeyOutOfOrder() {
        // Если в данном тесте заменить первый null на A, то он проходит
        mainTable.addUpdate(FlatRow.of(null, B, null), FlatRow.of(null, C, E));
        // Проблема в том, что когда нам для ключа C становится известна первая колонка,
        // отсутствует код, который бы автоматически распространил это знание на ключ B,
        // операция над которым должна примениться первой. Так как этого не происходит,
        // мы делаем обновление третьей колонки на E после F, а не до.
        mainTable.addUpdate(FlatRow.of(A, C, null), FlatRow.of(A, C, F));
        MockTransaction tx = new MockTransaction();
        tx.getTableData(MAIN_TABLE).lookupData.put(FlatRow.of(A, B), FlatRow.of(A, B, C));
        tx.getTableData(INDEX_TABLE).lookupData.put(FlatRow.of(B), FlatRow.of(B, A));
        mainTable.apply(tx).join();
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(A, B)
        ));
        MatcherAssert.assertThat(tx.getTableData(MAIN_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(A, C, F)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).updatedRows, is(empty()));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).deletedKeys, containsInAnyOrder(
                FlatRow.of(B)
        ));
        MatcherAssert.assertThat(tx.getTableData(INDEX_TABLE).insertedRows, containsInAnyOrder(
                FlatRow.of(C, A)
        ));
    }
}
