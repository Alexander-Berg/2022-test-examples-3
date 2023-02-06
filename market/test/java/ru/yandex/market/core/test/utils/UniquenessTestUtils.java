package ru.yandex.market.core.test.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;

import ru.yandex.common.util.id.HasId;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Утилитный класс для проверки уникальности данных.
 */
public final class UniquenessTestUtils {

    private UniquenessTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static <ID, T extends Enum<T> & HasId<ID>> void checkUniqueness(final Class<T> idEnum) {
        final List<T> types = EnumUtils.getEnumList(idEnum);
        final Set<ID> ids = types.stream()
                .map(HasId::getId)
                .collect(Collectors.toSet());

        assertThat(ids, hasSize(types.size()));
    }

}
