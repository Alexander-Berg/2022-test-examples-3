package ru.yandex.direct.dbutil;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.assertj.core.api.SoftAssertions;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.dbutil.ConditionsAccumulatorTest.TestTable.TEST_TABLE;

public class ConditionsAccumulatorTest {

    @SuppressWarnings("unused")
    static class TestTable extends TableImpl<TestTableRecord> {
        static final TestTable TEST_TABLE = new TestTable();

        public final TableField<TestTableRecord, Long> id =
                createField("ID", org.jooq.impl.SQLDataType.BIGINT, this, "");

        public final TableField<TestTableRecord, Long> secondId =
                createField("SECOND_ID", org.jooq.impl.SQLDataType.BIGINT, this, "");

        public final TableField<TestTableRecord, String> text =
                createField("TEXT", org.jooq.impl.SQLDataType.CLOB, this, "");

        TestTable() {
            super(DSL.name("test_table"));
        }

        private TestTable(Name alias, Table<TestTableRecord> aliased) {
            super(alias, null, aliased, null, DSL.comment(""));
        }

        @Override
        public Class<? extends TestTableRecord> getRecordType() {
            return TestTableRecord.class;
        }

        @Override
        public TestTable as(String alias) {
            return new TestTable(DSL.name(alias), this);
        }
    }

    static class TestTableRecord extends TableRecordImpl<TestTableRecord> {
        public TestTableRecord() {
            super(TEST_TABLE);
        }
    }


    private final SoftAssertions softly = new SoftAssertions();
    private final ConditionsAccumulator accumulator = new ConditionsAccumulator();

    @Test
    public void emptyAccumulatorTest() {
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isFalse();
        softly.assertThat(accumulator.getConditions()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void singleNotEmptyCollectionConditionTest() {
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(1);
        softly.assertAll();
    }

    @Test
    public void singleEmptyCollectionConditionTest() {
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.id::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(1);
        softly.assertAll();
    }

    @Test
    public void twoNotEmptyCollectionConditionTest() {
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(() -> singletonList(2L), Function.identity(), TEST_TABLE.secondId::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(2);
        softly.assertAll();
    }

    @Test
    public void twoEmptyCollectionConditionsTest() {
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.id::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.secondId::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(2);
        softly.assertAll();
    }

    @Test
    public void twoNotEmptyAndOneEmptyCollectionConditionsTest1() {
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(() -> singletonList(2L), Function.identity(), TEST_TABLE.secondId::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void twoNotEmptyAndOneEmptyCollectionConditionsTest2() {
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(() -> singletonList(2L), Function.identity(), TEST_TABLE.secondId::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void twoNotEmptyAndOneEmptyCollectionConditionsTest3() {
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        accumulator.add(() -> singletonList(2L), Function.identity(), TEST_TABLE.secondId::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void oneNotEmptyAndTwoEmptyCollectionConditionsTest1() {
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.secondId::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void oneNotEmptyAndTwoEmptyCollectionConditionsTest2() {
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.secondId::in);
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void oneNotEmptyAndTwoEmptyCollectionConditionsTest3() {
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.secondId::in);
        accumulator.add(Collections::emptyList, Function.identity(), TEST_TABLE.text::in);
        accumulator.add(() -> singletonList(1L), Function.identity(), TEST_TABLE.id::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isTrue();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(3);
        softly.assertAll();
    }

    @Test
    public void oneFieldConditionTest() {
        accumulator.add(() -> 1L, Function.identity(), TEST_TABLE.id::eq);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(1);
        softly.assertAll();
    }

    @Test
    public void twoFieldConditionsTest() {
        accumulator.add(() -> 1L, Function.identity(), TEST_TABLE.id::eq);
        accumulator.add(() -> "test", Function.identity(), TEST_TABLE.text::eq);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isTrue();
        softly.assertThat(accumulator.getConditions()).hasSize(2);
        softly.assertAll();
    }

    @Test
    public void nullFieldConditionTest() {
        accumulator.add(this::nullValue, Function.identity(), TEST_TABLE.id::eq);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isFalse();
        softly.assertThat(accumulator.getConditions()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void nullCollectionConditionTest() {
        accumulator.add(this::nullLongCollection, Function.identity(), TEST_TABLE.id::in);
        softly.assertThat(accumulator.containsEmptyCondition()).isFalse();
        softly.assertThat(accumulator.containsConditions()).isFalse();
        softly.assertThat(accumulator.getConditions()).isEmpty();
        softly.assertAll();
    }

    private Long nullValue() {
        return null;
    }

    private Collection<Long> nullLongCollection(){
        return null;
    }
}
