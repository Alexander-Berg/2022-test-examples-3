package ru.yandex.market.ff.grid.model.cell;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.exception.InvalidCellDataTypeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit тесты для {@link GridCell}.
 *
 * @author avetokhin 20/09/17.
 */
class DefaultGridCellTest {

    /**
     * Проверить конвертацию в String.
     */
    @Test
    void getValueAsString() {
        assertThat(str(createGrid("")), equalTo(""));
        assertThat(str(createGrid("abc")), equalTo("abc"));
        assertThat(str(createGrid("123")), equalTo("123"));
        assertThat(str(createGrid("123;asd")), equalTo("123;asd"));
        assertThat(str(createGrid(null)), nullValue());
    }

    /**
     * Проверить валидные кейзы конвертации в Long.
     */
    @Test
    void getValueAsLongValid() {
        assertThat(lng(createGrid("123")), equalTo(123L));
        assertThat(lng(createGrid("123.44")), equalTo(123L));
        assertThat(lng(createGrid("123,55")), equalTo(123L));
        assertThat(lng(createGrid(" 123,55   ")), equalTo(123L));
        assertThat(lng(createGrid("-123")), equalTo(-123L));
        assertThat(lng(createGrid("10000000000000001")), equalTo(10000000000000001L));

        assertThat(lng(createGrid(null)), nullValue());
        assertThat(lng(createGrid("")), nullValue());
    }

    /**
     * Проверить, что выбрасываются исключения в случае невозможности конвертировать в Long.
     */
    @Test
    void getValueAsLongInvalid() {
        testNumberException(() -> lng(createGrid("ab")));
        testNumberException(() -> lng(createGrid("123;123")));
    }

    private void testNumberException(final Runnable test) {
        try {
            test.run();
        } catch (InvalidCellDataTypeException e) {
            return;
        }
        fail("Expected InvalidCellDataTypeException exception");
    }

    private Long lng(DefaultGridCell gridCell) {
        return gridCell.getValueAsLong().orElse(null);
    }

    private String str(DefaultGridCell gridCell) {
        return gridCell.getRawValue().orElse(null);
    }

    private static DefaultGridCell createGrid(final String value) {
        return new DefaultGridCell(0, 0, value);
    }

}
