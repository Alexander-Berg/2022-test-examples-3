package ru.yandex.market.abo.util;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 20.06.18.
 * Проверяет, что все значения энума есть в базе
 */
public abstract class EnumDbConsistencyTest<T> extends EmptyTest {

    @Test
    void testEnumConsistency() {
        Sets.SetView<T> diff = Sets.symmetricDifference(getDbIds(), getEnumIds());
        assertTrue(diff.isEmpty(), "diff between db and enum values! " + diff);
    }

    protected abstract Set<T> getDbIds();

    protected abstract Set<T> getEnumIds();

}
