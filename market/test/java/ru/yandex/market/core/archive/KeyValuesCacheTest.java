package ru.yandex.market.core.archive;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.KeyValues;
import ru.yandex.market.core.archive.type.KeyColumnType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.core.archive.ArchivingFunctionalTest.key;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class KeyValuesCacheTest {
    private KeyValuesCache valuesCache = new KeyValuesCache();

    @BeforeEach
    void init() {
        valuesCache = new KeyValuesCache();
    }

    @Test
    @DisplayName("Проверка добавления и чтения обычного PK")
    void testNextSinglePk() {
        Key key = key("TEST", new KeyPart("ID", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID", 3L).build();

        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        List<Object> expected = Arrays.asList(3L);
        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 0);
            assertEquals(expected, actual);
        });
    }

    @Test
    @DisplayName("Проверка повторного чтения, когда уже нет значений")
    void testNextSinglePkRepeatEmpty() {
        Key key = key("TEST", new KeyPart("ID", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID", 3L).build();

        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM", getter -> {
            List<Object> actual = getter.get(key, 0);
            assertTrue(actual.isEmpty());
        });
    }

    @Test
    @DisplayName("Проверка повторного чтения после добавления данных")
    void testNextSinglePkRepeat() {
        Key key = key("TEST", new KeyPart("ID", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID", 3L).build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);
        });

        keyValues = new KeyValues.Builder(key)
                .addValue("ID", 5L).build();
        addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(5L);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L, 5L);
            assertEquals(expected, actual);
        });
    }

    @Test
    @DisplayName("Проверка добавления и чтения составного PK")
    void testNextMultiplePk() {
        Key key = key("TEST", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);

            actual = getter.get(key, 1);
            expected = Arrays.asList("MY1");
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 1);
            List<Object> expected = Arrays.asList("MY1");
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 1);
            assertTrue(actual.isEmpty());
        });
    }

    @Test
    @DisplayName("Проверка повторного чтения составного PK")
    void testNextMultiplePkRepeat() {
        Key key = key("TEST", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 1);
            List<Object> expected = Arrays.asList("MY1");
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 1);
            assertTrue(actual.isEmpty());
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 1);
            assertTrue(actual.isEmpty());
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            assertTrue(actual.isEmpty());
        });

        keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 4L).addValue("ID2", "MY2").nextRow()
                .addValue("ID1", 5L).addValue("ID2", "MY3").nextRow().build();
        addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(4L, 5L);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(4L, 5L);
            assertEquals(expected, actual);

            actual = getter.get(key, 1);
            expected = Arrays.asList("MY2", "MY3");
            assertEquals(expected, actual);
        });
    }

    @Test
    @DisplayName("Проверка сброса истории чтения")
    void testReset() {
        Key key = key("TEST", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);
        });

        valuesCache.resetReadHistory();

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 1);
            List<Object> expected = Arrays.asList("MY1");
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);
        });
    }

    @Test
    @DisplayName("Проверка чтения значений, которых нет в кэше")
    void testNullKeySet() {
        Key key = key("TEST", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 4);
            assertNull(actual);
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key("TEST10", new KeyPart("ID", KeyColumnType.NUMBER)), 0);
            assertNull(actual);
        });
    }

    @Test
    @DisplayName("Вставка дубликатов")
    void testDuplicate() {
        Key key = key("TEST", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").nextRow()
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        assertEquals(1, keyValues.getValues(0, 0).size());
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        addRes = valuesCache.addValues(keyValues);
        assertFalse(addRes);

        keyValues = new KeyValues.Builder(key)
                .addValue("ID1", 4L).addValue("ID2", "MY2").nextRow()
                .addValue("ID1", 3L).addValue("ID2", "MY1").build();
        assertEquals(2, keyValues.getValues(0, 0).size());
        addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            List<Object> expected = Arrays.asList(3L, 4L);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM2", getter -> {
            List<Object> actual = getter.get(key, 1);
            List<Object> expected = Arrays.asList("MY1", "MY2");
            assertEquals(expected, actual);
        });
    }

    @Test
    @DisplayName("Несколько чтений в одной транзакции")
    void testTransaction() {
        Key key = key("TEST", new KeyPart("ID", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("ID", 3L).build();

        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> expected = Arrays.asList(3L);

            List<Object> actual = getter.get(key, 0);
            assertEquals(expected, actual);

            actual = getter.get(key, 0);
            assertEquals(expected, actual);
        });

        valuesCache.transactionReading("FROM1", getter -> {
            List<Object> actual = getter.get(key, 0);
            assertTrue(actual.isEmpty());
        });
    }

    @Test
    @DisplayName("Чтение из разных таблиц")
    void testReadingFromSeveralTables() {
        Key key1 = key("TEST1", new KeyPart("ID", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key1)
                .addValue("ID", 3L).build();
        boolean addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        Key key2 = key("TEST2", new KeyPart("ID", KeyColumnType.NUMBER));
        keyValues = new KeyValues.Builder(key2)
                .addValue("ID", 4L).nextRow()
                .addValue("ID", 5L).build();
        addRes = valuesCache.addValues(keyValues);
        assertTrue(addRes);

        valuesCache.transactionReading("FROM", getter -> {
            List<Object> actual = getter.get(key1, 0);
            List<Object> expected = Arrays.asList(3L);
            assertEquals(expected, actual);

            actual = getter.get(key2, 0);
            expected = Arrays.asList(4L, 5L);
            assertEquals(expected, actual);
        });
    }
}
