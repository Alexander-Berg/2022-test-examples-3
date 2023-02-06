package ru.yandex.market.antifraud.filter;

import org.joda.time.DateTime;
import org.junit.Test;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ShowGeneratorTest {
    @Test
    public void smokeTest() {
        TestShow show = ShowGenerator.uniqueShow(DateTime.now());
        asserts(show);
    }

    @Test
    public void smokeTestWithRowid() {
        TestShow show = ShowGenerator.uniqueShow(DateTime.now(), "rowid_1");
        asserts(show);
        assertThat(show.getRowid(), is("rowid_1"));

    }

    private void asserts(TestShow show) {
        assertTrue(show.getRowid().length() > 0);
        assertTrue(show.getShowUid().length() > 0);
        assertTrue(show.getPp() > 0);
        assertThat(show.getFilter(), is(FilterConstants.FILTER_0));
    }
}
