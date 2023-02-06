package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud;

import org.junit.Test;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud.util.TimeUtils;

import java.util.Calendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by oroboros on 12.02.16.
 */
public class TimeUtilsTest {
    @Test
    public void mustTruncate() {
        Calendar c = Calendar.getInstance();
        c.set(2003, 11, 28, 15, 45, 50);

        TimeUtils.trunc(c);

        assertThat(c.get(Calendar.YEAR), is(2003));
        assertThat(c.get(Calendar.MONTH), is(11));
        assertThat(c.get(Calendar.DATE), is(28));
        assertThat(c.get(Calendar.HOUR), is(0));
        assertThat(c.get(Calendar.MINUTE), is(0));
        assertThat(c.get(Calendar.SECOND), is(0));
    }
}
