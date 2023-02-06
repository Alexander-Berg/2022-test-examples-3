package ru.yandex.market.notification.common;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

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

    @Test
    public void testGetDefaultNameNegative() {
        assertThrows(NullPointerException.class, () -> {
            fail(Named.getDefaultName(null));
        });
    }
}
