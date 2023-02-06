package ru.yandex.market.mbi.msapi.clicks;

import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbi.msapi.clicks.MarketstatApiLine.TRANS_ID;
import static ru.yandex.market.mbi.msapi.clicks.RollbacksWriter.splittedInList;

/**
 * @author aostrikov
 */
public class RollbacksWriterTest {
    @Test
    public void shouldGenerateSimpleRollacksExpression() {
        List<Integer> ids = asList(1, 2, 3, 4, 5);

        assertEquals("trans_id IN (?,?,?,?,?)", splittedInList(ids.iterator(), TRANS_ID + " IN (", ")", " OR ", 10));
    }

    @Test
    public void shouldGenerateComplexRollacksExpression() {
        List<Integer> ids = asList(1, 2, 3, 4, 5);

        assertEquals("trans_id IN (?,?,?) OR trans_id IN (?,?)",
                splittedInList(ids.iterator(), TRANS_ID + " IN (", ")",
                        " OR ", 3));
    }
}
