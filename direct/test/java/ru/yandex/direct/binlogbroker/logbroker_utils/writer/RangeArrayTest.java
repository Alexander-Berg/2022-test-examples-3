package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.AbstractLogbrokerWriterImpl.RangeArray.STRING_SIZE_LIMIT;

/**
 * Тест для массива, который схлопывает идущие подряд числа подряд в красивую строку из интервалов.
 */
public class RangeArrayTest {

    private void addValues(AbstractLogbrokerWriterImpl.RangeArray rangeArray, long... values) {
        for (long value : values) {
            rangeArray.addValue(value);
        }
    }

    @Test
    public void testRangeArray() {
        var rangeArray = new AbstractLogbrokerWriterImpl.RangeArray();
        assertThat(rangeArray.getSize()).isEqualTo(0);
        assertThat(rangeArray.toString()).isEqualTo("");

        addValues(rangeArray, 2L, 3L, 4L, 5L, 7L, 9L);
        assertThat(rangeArray.getSize()).isEqualTo(6);
        assertThat(rangeArray.toString()).isEqualTo("2-5,7,9");
    }

    @Test
    public void testRepeated() {
        var rangeArray = new AbstractLogbrokerWriterImpl.RangeArray();
        addValues(rangeArray, 2L, 2L, 2L, 2L, 3L, 3L);
        assertThat(rangeArray.getSize()).isEqualTo(2);
        assertThat(rangeArray.toString()).isEqualTo("2-3");
    }

    @Test
    public void testRangeArrayInverted() {
        var rangeArray = new AbstractLogbrokerWriterImpl.RangeArray();
        addValues(rangeArray, 2L, 4L, 3L, 9L, 7L, 5L, 3L);
        assertThat(rangeArray.getSize()).isEqualTo(6);
        assertThat(rangeArray.toString()).isEqualTo("2-5,7,9");
    }

    @Test
    public void testToStringSizeTrim() {
        var rangeArray = new AbstractLogbrokerWriterImpl.RangeArray();
        for (int i = 0; i < STRING_SIZE_LIMIT; i += 2) {
            rangeArray.addValue(i);
        }
        var str = rangeArray.toString();
        assertThat(str.length()).isEqualTo(STRING_SIZE_LIMIT);
        assertThat(str).endsWith("...");

    }
}
