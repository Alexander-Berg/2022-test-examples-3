package ru.yandex.market.notification.test.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Disabled;

import ru.yandex.common.util.id.HasId;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс для unit-тестов моделей.
 *
 * @author Vladislav Bauer
 */
@Disabled
public abstract class AbstractModelTest {

    protected <T> void checkBasicMethods(@Nonnull final T object, @Nonnull final T same, @Nonnull final T other) {
        assertThat(object.toString(), not(isEmptyOrNullString()));

        assertThat(object.equals(new Object()), equalTo(false));
        assertThat(object, not(equalTo(other)));
        assertThat(object, equalTo(same));
        assertThat(object.hashCode(), equalTo(same.hashCode()));
        assertThat(object.toString(), equalTo(same.toString()));
    }

    protected <T extends Enum<T> & HasId<?>> void checkEnum(@Nonnull final Class<T> enumClass, final int count) {
        final T[] enumConstants = enumClass.getEnumConstants();
        final Collection<?> codes = Arrays.stream(enumConstants)
            .map(enumConstant -> enumConstant.getId())
            .collect(Collectors.toSet());

        assertThat(enumConstants, arrayWithSize(count));
        assertThat(codes, hasSize(count));
    }

    protected <T> void assertThatCode(@Nonnull final HasId<T> transport, @Nonnull final T code) {
        assertThat(transport.getId(), equalTo(code));
    }

}
