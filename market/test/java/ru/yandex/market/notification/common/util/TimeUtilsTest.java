package ru.yandex.market.notification.common.util;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link TimeUtils}.
 *
 * @author Vladislav Bauer
 */
public class TimeUtilsTest {

    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(TimeUtils.class);
    }

    @Test
    public void testNToMs() {
        assertThat(TimeUtils.nsToMs(1000000), equalTo(1L));
    }

}
