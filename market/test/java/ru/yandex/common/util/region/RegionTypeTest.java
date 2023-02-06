package ru.yandex.common.util.region;

import org.junit.Test;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link RegionType}.
 *
 * @author Vladislav Bauer
 */
public class RegionTypeTest {

    @Test
    public void testGetRegionTypeByCodePositive() {
        for (final RegionType actualType : RegionType.values()) {
            final int code = actualType.getCode();
            final RegionType expectedType = RegionType.getRegionTypeByCode(code, true);

            assertThat(expectedType, equalTo(actualType));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRegionTypeByCodeNegative() {
        final int maxCode = Stream.of(RegionType.values())
            .map(RegionType::getCode)
            .max(Comparator.naturalOrder())
            .orElseThrow(RuntimeException::new);

        final int unknownCode = maxCode + 1;
        assertThat(RegionType.getRegionTypeByCode(unknownCode, false), nullValue());
        fail(String.valueOf(RegionType.getRegionTypeByCode(unknownCode, true)));
    }

}
