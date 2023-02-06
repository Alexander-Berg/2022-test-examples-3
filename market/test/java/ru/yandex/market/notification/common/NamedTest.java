package ru.yandex.market.notification.common;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link Named}.
 *
 * @author Vladislav Bauer
 */
public class NamedTest {

    @Test
    public void testGetDefaultNamePositive() {
        assertThat(
            Named.getDefaultName(new Object()),
            equalTo(Object.class.getSimpleName())
        );
    }

    @Test(expected = NullPointerException.class)
    public void testGetDefaultNameNegative() {
        fail(Named.getDefaultName(null));
    }

}
