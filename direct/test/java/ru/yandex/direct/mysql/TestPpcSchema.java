package ru.yandex.direct.mysql;

import java.io.IOException;
import java.io.Serializable;

import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.direct.mysql.schema.ColumnSchema;
import ru.yandex.direct.mysql.schema.DatabaseSchema;
import ru.yandex.direct.mysql.schema.ServerSchema;
import ru.yandex.direct.mysql.schema.TableSchema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TestPpcSchema {
    private static final ServerSchema serverSchema = loadSchema("ppc1-schema-20170117.json");

    @Test
    public void verifyDefaultsParser() {
        for (DatabaseSchema databaseSchema : serverSchema.getDatabases()) {
            for (TableSchema tableSchema : databaseSchema.getTables()) {
                for (ColumnSchema columnSchema : tableSchema.getColumns()) {
                    String defaultValue = columnSchema.getDefaultValue();
                    if (defaultValue != null) {
                        MySQLColumnType columnType = MySQLColumnType.getCached(columnSchema.getColumnType());
                        // Проверяем, что мы способны распарсить все DEFAULT значения в схеме ppc
                        Serializable a = columnType.parseValue(defaultValue);
                        // Проверяем, что закешированные значения корректны
                        Serializable b = columnType.getCachedDefaultValue(defaultValue);
                        Serializable c = columnType.getCachedDefaultValue(defaultValue);
                        assertEquals(a, b);
                        assertSame(b, c);
                    }
                }
            }
        }
    }

    private static ServerSchema loadSchema(String filename) {
        try {
            return ServerSchema.fromJson(Resources
                    .toByteArray(Resources.getResource(TestPpcSchema.class, filename)));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load schema from " + filename);
        }
    }
}
