package ru.yandex.market.olap2.load;

import org.junit.Test;
import ru.yandex.market.olap2.util.CharUtil;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PartitionSwapperTest {
    @Test
    public void testQlist() {
        assertThat(CharUtil.qlist("a", "b", "c"),
            is("'a','b','c'"));
        assertThat(CharUtil.qlist("a"),
            is("'a'"));
        assertThat(CharUtil.qlist(),
            is("''"));
    }
}
