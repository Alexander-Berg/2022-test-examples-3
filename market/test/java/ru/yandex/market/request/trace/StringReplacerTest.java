package ru.yandex.market.request.trace;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class StringReplacerTest {

    @Test
    public void shouldReplaceSimple() throws Exception {
        StringReplacer replacer = new StringReplacer(new ArrayList<Pair<String, String>>() {{
            add(Pair.of("a", "b"));
            add(Pair.of("c", "d"));
        }});

        assertEquals("bbddef", replacer.replace("abcdef"));
    }

    @Test
    public void shouldReplaceTskv() throws Exception {
        StringReplacer tskv = StringReplacer.TSKV;

        assertEquals("\\n", tskv.replace("\n"));
        assertEquals("\\\\\\n", tskv.replace("\\\n"));

        assertEquals("\\\n", tskv.revert("\\\\\\n"));
    }
}
