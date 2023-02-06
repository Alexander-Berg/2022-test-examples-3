package ru.yandex.travel.yt.test;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import ru.yandex.travel.yt.exceptions.InvalidMappingException;
import ru.yandex.travel.yt.mappings.TableMapping;
import ru.yandex.travel.yt.mappings.YtColumn;
import ru.yandex.travel.yt.mappings.YtTable;

import static org.junit.Assert.assertEquals;
import static ru.yandex.travel.yt.test.Utils.assertThrows;


@YtTable(tableName = "TestTable")
class RecordWithName {
    @YtColumn
    private String foo;

    @YtColumn
    private String bar;

    public RecordWithName() {
    }
}

@YtTable(tableName = "AnotherTable", ttl = 42)
class RecordWithBoth {
    @YtColumn
    private String foo;

    @YtColumn
    private String bar;

    public RecordWithBoth() {
    }
}


@YtTable
class RecordWithNone {
    @YtColumn
    private String foo;

    @YtColumn
    private String bar;
}


@YtTable(tableName = "TestTable")
class RecordWithRedefinedFields {
    @YtColumn(columnName = "baz")
    private String foo;

    @YtColumn(columnName = "qux")
    private String baz;
}

@YtTable(tableName = "TestTable")
class RecordWithDuplicateColumns {
    @YtColumn
    private String foo;

    @YtColumn(columnName = "foo")
    private String bar;
}

@YtTable(tableName = "TestTable")
class RecordWithNoDefaultConstructor {
    @YtColumn
    private String foo;

    @YtColumn
    private String bar;

    public RecordWithNoDefaultConstructor(String foo, String bar) {
        this.foo = foo;
        this.bar = bar;
    }
}

public class MappingCreationTest {

    @Test
    public void TestImplicitCreation() {
        TableMapping<RecordWithName> mapping = new TableMapping<>(
                RecordWithName.class);
        assertEquals("TestTable", mapping.getTableName());
        assertEquals(0, mapping.getTtl());
        assertEquals(new HashSet<>(Arrays.asList("foo", "bar")), mapping.getMappedColumns());
    }

    @Test
    public void TestExplicitCreation() {
        TableMapping<RecordWithName> mapping = new TableMapping<>(
                RecordWithName.class,
                "overriddenName", 42L);
        assertEquals("overriddenName", mapping.getTableName());
        assertEquals(42, mapping.getTtl());
        assertEquals(new HashSet<>(Arrays.asList("foo", "bar")), mapping.getMappedColumns());
    }

    @Test
    public void TestValueRedefinition() {
        assertEquals(new HashSet<>(Arrays.asList("baz", "qux")),
                new TableMapping<>(RecordWithRedefinedFields.class).getMappedColumns());
    }


    @Test
    public void TestFailsOnCreationWithNoName() {
        assertThrows(InvalidMappingException.class, () -> new TableMapping<>(RecordWithNone.class));
    }

    @Test
    public void TestFailsOnCreationWithDuplicateColumns() {
        assertThrows(InvalidMappingException.class, () -> new TableMapping<>(RecordWithDuplicateColumns.class));
    }

    @Test
    public void TestFailsOnCreationIfNoDefaultConstructor() {
        assertThrows(InvalidMappingException.class, () -> new TableMapping<>(RecordWithNoDefaultConstructor.class));
    }
}
