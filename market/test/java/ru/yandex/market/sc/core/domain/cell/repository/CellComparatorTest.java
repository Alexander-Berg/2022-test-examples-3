package ru.yandex.market.sc.core.domain.cell.repository;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.cell.CellQueryService.SC_NUMBER_ASC_ID_DESC;

public class CellComparatorTest {

    @Test
    void orderCellsByScNumberAscAndIdDesc() {
        var cell3 = createCell(1, "1003");
        var cell7 = createCell(2, "1007");
        var cell9 = createCell(3, "1009");
        var cell2 = createCell(4, "1002");
        var cell0 = createCell(5, "1000");
        var cellId6 = createCell(6, null);
        var cellId7 = createCell(7, null);
        var cellId8 = createCell(8, null);

        var cells = List.of(cell3, cellId8, cell7, cell9, cellId6, cellId7, cell2, cell0);
        List<Cell> actual = StreamEx.of(cells)
                .sorted(SC_NUMBER_ASC_ID_DESC)
                .toList();

        assertThat(actual)
                .containsExactly(
                        cell0,
                        cell2,
                        cell3,
                        cell7,
                        cell9,
                        cellId8,
                        cellId7,
                        cellId6
                );
    }

    private static Cell createCell(long id, String number) {
        var cell = new Cell(null, number, null);
        cell.setId(id);
        return cell;
    }
}
