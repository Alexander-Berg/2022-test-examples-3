package ru.yandex.market.ff.grid.writer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.grid.DefaultGrid;
import ru.yandex.market.ff.grid.model.row.DefaultGridRow;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.grid.writer.csv.CsvGridWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link CsvGridWriter}.
 *
 * @author avetokhin 16/05/2018.
 */
class CsvGridWriterTest {

    private static final Map<Integer, String> HEADER = ImmutableMap.<Integer, String>builder()
            .put(0, "FIRST")
            .put(1, "SECOND")
            .put(2, "THIRD")
            .put(3, "FOURTH")
            .build();

    private static final String EXPECTED = "FIRST\tSECOND\tTHIRD\tFOURTH\n"
            + "1\t\"\"\"test\"\t\"2\"\"\"\t\"\"\"tt\"\"\"\n";

    @Test
    void testWriteWithQuotes() {
        final GridRow row = new DefaultGridRow(0);
        row.appendCell(new DefaultGridCell(0, 0, "1"));
        row.appendCell(new DefaultGridCell(0, 1, "\"test"));
        row.appendCell(new DefaultGridCell(0, 2, "2\""));
        row.appendCell(new DefaultGridCell(0, 3, "\"tt\""));
        final DefaultGrid grid = new DefaultGrid(HEADER);
        grid.appendRow(row);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new CsvGridWriter().write(grid, outputStream);

        final String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        assertThat(result, equalTo(EXPECTED));
    }

}
