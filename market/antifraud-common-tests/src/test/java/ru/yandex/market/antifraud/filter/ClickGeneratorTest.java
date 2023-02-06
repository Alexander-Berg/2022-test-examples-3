package ru.yandex.market.antifraud.filter;

import org.joda.time.DateTime;
import org.junit.Test;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ClickGeneratorTest {
    @Test
    public void smokeTest() {
        TestClick c = ClickGenerator.generateUniqueClick(DateTime.now());
        assertTrue(c.get("rowid", String.class).length() > 0);
        assertTrue(c.get("show_uid", String.class).length() > 0);
        assertTrue(c.get("pp", Integer.class) > 0);
        assertThat(c.getFilter(), is(FilterConstants.FILTER_0));
    }
}
