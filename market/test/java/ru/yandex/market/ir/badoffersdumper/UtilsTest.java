package ru.yandex.market.ir.badoffersdumper;

import org.junit.Test;
import ru.yandex.market.ir.badoffersdumper.Range;
import ru.yandex.market.ir.badoffersdumper.Utils;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author inenakhov
 */
public class UtilsTest {
    @Test
    public void split() throws Exception {
        List<Range> ranges = Utils.split(1);
        assertEquals(1, ranges.size());
        assertEquals(Range.ALL, ranges.get(0));
    }

    @Test
    public void split3() throws Exception {
        List<Range> ranges = Utils.split(4);
        assertEquals(4, ranges.size());
    }
}
