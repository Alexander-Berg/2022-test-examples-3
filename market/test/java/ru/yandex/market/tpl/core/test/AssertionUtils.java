package ru.yandex.market.tpl.core.test;

import java.util.Optional;

import lombok.experimental.UtilityClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public class AssertionUtils {

    public static <T> T assertPresent(Optional<T> optional) {
        assertThat(optional).isPresent();
        return optional.get();
    }

}
