package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.validation.Violation;
import ru.yandex.market.ff.grid.validation.ViolationsContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link UniqueValidationRule}.
 *
 * @author avetokhin 15/02/18.
 */
class UniqueValidationRuleTest {

    private final UniqueValidationRule rule = new UniqueValidationRule("error");

    @Test
    void testErrors() {
        final List<GridCell> cells = Arrays.asList(
                new DefaultGridCell(0, 1, "AA"),
                new DefaultGridCell(0, 1, "BB"),
                new DefaultGridCell(0, 1, "AA"),
                new DefaultGridCell(0, 1, " AA"),
                new DefaultGridCell(0, 1, "AA "),
                new DefaultGridCell(0, 1, " AA ")
        );
        final Grid grid = mockGrid(cells);

        final ViolationsContainer violationsContainer = new ViolationsContainer();
        rule.applyToColumn(grid, 1, violationsContainer);

        final Multimap<GridCell, Violation> cellViolations = violationsContainer.getCellViolations();
        assertThat(cellViolations.size(), equalTo(5));
        assertThat(cellViolations.containsKey(cells.get(0)), equalTo(true));
        assertThat(cellViolations.containsKey(cells.get(2)), equalTo(true));
        assertThat(cellViolations.containsKey(cells.get(3)), equalTo(true));
        assertThat(cellViolations.containsKey(cells.get(4)), equalTo(true));
        assertThat(cellViolations.containsKey(cells.get(5)), equalTo(true));
    }

    @Test
    void testNoErrors() {
        final Grid grid = mockGrid(Arrays.asList(
                new DefaultGridCell(0, 1, "AA"),
                new DefaultGridCell(0, 1, "aa"),
                new DefaultGridCell(0, 1, "BB"),
                new DefaultGridCell(0, 1, "CC"),
                new DefaultGridCell(0, 1, ""),
                new DefaultGridCell(0, 1, ""),
                new DefaultGridCell(0, 1, null),
                new DefaultGridCell(0, 1, null)
        ));

        final ViolationsContainer violationsContainer = new ViolationsContainer();
        rule.applyToColumn(grid, 1, violationsContainer);
        assertThat(violationsContainer.isEmpty(), equalTo(true));
    }

    private Grid mockGrid(List<GridCell> cells) {
        final Grid grid = mock(Grid.class);
        when(grid.getCells(1)).thenReturn(cells);
        return grid;
    }

}
