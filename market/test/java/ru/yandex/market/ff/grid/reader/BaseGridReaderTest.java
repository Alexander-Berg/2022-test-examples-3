package ru.yandex.market.ff.grid.reader;

import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.grid.Grid;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Базовый класс для тестирования {@link GridReader}.
 *
 * @author avetokhin 18/09/17.
 */
public abstract class BaseGridReaderTest {

    private static final String FILLED_EXAMPLE = "filled_example";
    private static final String FILLED_WITH_SKIPS = "filled_with_skips";
    private static final String VARIOUS_TYPES = "various_types";
    private static final String UNEVENLY_FILLED = "unevenly_filled";

    static final int FIRST_COLUMN = 0;
    static final int SECOND_COLUMN = 1;
    static final int THIRD_COLUMN = 2;

    SoftAssertions softly;

    @BeforeEach
    void beforeTest() {
        softly = new SoftAssertions();
    }

    @AfterEach
    void afterTest() {
        softly.assertAll();
    }

    protected abstract String getExtension();

    protected abstract GridReader getReader();

    @Test
    void testHeadersExist() throws Exception {
        Grid grid = getReader().read(getResource(FILLED_EXAMPLE));

        assertHeaderExistence(FIRST_COLUMN, grid);
        assertHeaderExistence(SECOND_COLUMN, grid);
        assertHeaderExistence(THIRD_COLUMN, grid);
    }

    @Test
    void testRowsAreValid() throws Exception {
        Grid grid = getReader().read(getResource(FILLED_EXAMPLE));

        assertCellValue(grid, 0, 0, "1");
        assertCellValue(grid, 0, 1, "2");
        assertCellValue(grid, 0, 2, "3");
        assertCellValue(grid, 1, 0, "2");
        assertCellValue(grid, 1, 1, "\"4");
        assertCellValue(grid, 1, 2, "6");
        assertCellValue(grid, 2, 0, "3");
        assertCellValue(grid, 2, 1, "6\"");
        assertCellValue(grid, 2, 2, "9");
    }

    @Test
    void testGetValuesByColumnName() throws Exception {
        Grid grid = getReader().read(getResource(FILLED_EXAMPLE));

        assertValuesInColumn(grid, FIRST_COLUMN, asList("1", "2", "3"));
        assertValuesInColumn(grid, SECOND_COLUMN, asList("2", "\"4", "6\""));
        assertValuesInColumn(grid, THIRD_COLUMN, asList("3", "6", "9"));
    }

    @Test
    void testReadFileWithSkips() throws Exception {
        Grid grid = getReader().read(getResource(FILLED_WITH_SKIPS));

        assertCellValue(grid, 0, 0, "value");
        assertCellValue(grid, 0, 1, "");
        assertCellValue(grid, 1, 0, "");
        assertCellValue(grid, 1, 1, "value");
        assertCellValue(grid, 2, 0, "value");
        assertCellValue(grid, 2, 1, "");
    }

    @Test
    void testReadingVariousTypes() throws Exception {
        Grid grid = getReader().read(getResource(VARIOUS_TYPES));

        List<GridCell> cells = grid.getCells(FIRST_COLUMN);
        softly.assertThat(cells).describedAs("Cell size assertion").hasSize(6);
    }

    @Test
    void testReadUnevenlyFilledFile() throws Exception {
        Grid grid = getReader().read(getResource(UNEVENLY_FILLED));

        softly.assertThat(grid.getNumberOfColumns()).overridingErrorMessage("DefaultGrid must have 3 columns")
            .isEqualTo(3);
        softly.assertThat(grid.getNumberOfRows()).overridingErrorMessage("DefaultGrid must have 9 rows")
            .isEqualTo(9);
        softly.assertThat(grid.getCells(FIRST_COLUMN)).overridingErrorMessage("First column must have 9 rows")
            .hasSize(9);
        softly.assertThat(grid.getCells(SECOND_COLUMN)).overridingErrorMessage("Second column must have 9 rows")
            .hasSize(9);
        softly.assertThat(grid.getCells(THIRD_COLUMN)).overridingErrorMessage("Third column must have 9 rows")
            .hasSize(9);
    }


    private void assertValuesInColumn(Grid grid, int columnIndex, List<?> expectedValues) {
        List<GridCell> cells = grid.getCells(columnIndex);

        for (int i = 0; i < cells.size(); i++) {
            GridCell cell = cells.get(i);
            Object expectedValue = expectedValues.get(i);

            assertCellValue(cell.getRowIndex(), cell.getColumnIndex(), expectedValue, cell);
        }
    }

    private void assertCellValue(Grid grid, int rowIndex, int cellIndex, @Nullable Object expectedValue) {
        GridCell cell = grid.getRow(rowIndex).getCell(cellIndex);

        assertCellValue(rowIndex, cellIndex, expectedValue, cell);
    }

    private void assertCellValue(int rowIndex, int cellIndex, @Nullable Object expectedValue, GridCell cell) {
        Object actualValue = cell.getRawValue().orElse(null);
        softly.assertThat(actualValue)
                .overridingErrorMessage(getCellWrongValueError(
                        rowIndex,
                        cellIndex,
                        expectedValue,
                        actualValue
                )).isEqualTo(expectedValue);
    }

    private String getCellWrongValueError(int rowIndex, int cellIndex, @Nullable Object expectedValue,
                                          Object actualValue) {
        return format("Cell [%d,%d] should be filled with [%s], actual [%s]",
                rowIndex,
                cellIndex,
                expectedValue,
                actualValue
        );
    }


    private void assertHeaderExistence(int header, Grid grid) {
        softly.assertThat(grid.hasColumn(header))
                .withFailMessage(getHeaderMissingError(header))
                .isTrue();
    }

    private String getHeaderMissingError(int headerColumnIndex) {
        return format("File must have header with index [%s]",
                headerColumnIndex
        );
    }

    InputStream getResource(final String name) {
        return getSystemResourceAsStream(String.format("reader/%s.%s", name, getExtension()));
    }

}
