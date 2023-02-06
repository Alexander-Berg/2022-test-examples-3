package ru.yandex.market.ff.grid.reader;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

/**
 * Базовый класс для тестирования чтения Excel файлов в {@link Grid}.
 *
 * @author avetokhin 15/01/18.
 */
abstract class BaseExcelGridReaderTest extends BaseGridReaderTest {
    private static final String FILLED_WITH_EMPTY_LINES_AT_THE_END = "filled_with_empty_lines_at_the_end";

    protected final ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);

    @Test
    void testReadFileWithEmptyLinesAtTheEnd() {
        Grid grid = getReader().read(getResource(FILLED_WITH_EMPTY_LINES_AT_THE_END));

        softly.assertThat(grid.getNumberOfRows()).overridingErrorMessage("DefaultGrid must have 3 rows").isEqualTo(3);
        softly.assertThat(grid.getCells(FIRST_COLUMN)).overridingErrorMessage("First column must have 3 rows")
            .hasSize(3);
    }

}
