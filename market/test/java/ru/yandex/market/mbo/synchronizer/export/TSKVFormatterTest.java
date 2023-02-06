package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 02.06.2015
 */
@SuppressWarnings("checkstyle:lineLength")
public class TSKVFormatterTest {

    @Test
    public void testEmpty() throws Exception {
        TSKVFormatter<Void> formatter = new TSKVFormatter<Void>() {
            @Override
            protected void writeRecord(Void aVoid) throws IOException {
                // pass
            }
        };

        assertEquals("tskv\n", new String(formatter.format(null), UTF_8));
    }

    @Test
    public void testSimple() throws Exception {
        TSKVFormatter<String> formatter = new TSKVFormatter<String>() {
            @Override
            protected void writeRecord(String value) throws IOException {
                putValue("simpleKey", value);
                putValue("anotherKey", "fixedValue");
            }
        };

        assertEquals("tskv\tsimpleKey=124\tanotherKey=fixedValue\n", new String(formatter.format("124"), UTF_8));
        assertEquals("tskv\tsimpleKey=value with spaces\tanotherKey=fixedValue\n", new String(formatter.format("value with spaces"), UTF_8));
        assertEquals("tskv\tsimpleKey=value\\\twith\\\ttabs\tanotherKey=fixedValue\n", new String(formatter.format("value\twith\ttabs"), UTF_8));
        assertEquals("tskv\tsimpleKey=value with tab (\\\t) and new line(\\\n) and return (\\\r) and slash(\\\\) and quote(\\\") and equal(=) and zero(\\\0)\tanotherKey=fixedValue\n",
            new String(formatter.format("value with tab (\t) and new line(\n) and return (\r) and slash(\\) and quote(\") and equal(=) and zero(\0)"), UTF_8));
    }

    @Test
    public void testExample() throws Exception {
        TSKVFormatter<Void> formatter = new TSKVFormatter<Void>() {
            @Override
            protected void writeRecord(Void aVoid) throws IOException {
                putValue("ключ=содержащий равно", "значение=содержащее равно");
            }
        };

        assertEquals("tskv\tключ\\=содержащий равно=значение=содержащее равно\n", new String(formatter.format(null), UTF_8));
    }

    @Test
    public void testEscapeKey() throws Exception {
        TSKVFormatter<String[]> formatter = new TSKVFormatter<String[]>() {
            @Override
            protected void writeRecord(String[] values) throws IOException {
                int i = 0;
                while (i < values.length) {
                    putValue(values[i++], values[i++]);
                }
            }
        };

        assertEquals("tskv\tключ\\\tс\\\tтабуляцией=значение 1\tключ\\\nс\\\nпереносом строки=значение 2\n",
            new String(formatter.format(new String[]{"ключ\tс\tтабуляцией", "значение 1", "ключ\nс\nпереносом строки", "значение 2"}), UTF_8));
    }
}
