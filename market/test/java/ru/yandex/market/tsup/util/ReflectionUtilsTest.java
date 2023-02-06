package ru.yandex.market.tsup.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.data_provider.util.ReflectionUtils;

class ReflectionUtilsTest {

    @Test
    void getMethod() {
        Optional<Method> method = ReflectionUtils.getMethod(B.class, "getSomething", Dto.class);
        Assertions
            .assertThat(
                method
                    .map(Method::getGenericReturnType)
                    .map(t -> (ParameterizedType) t)
                    .map(ParameterizedType::getActualTypeArguments)
                    .map(t -> t[0]))
            .isEqualTo(Optional.of(String.class));
    }

    interface A<P extends Dto, R> {
        List<R> getSomething(P p);
    }

    static class B implements A<ExtendedDto, String> {

        @Override
        public List<String> getSomething(ExtendedDto p) {
            return List.of("aaaa");
        }
    }

    static class Dto {

    }

    static class ExtendedDto extends Dto {

    }
}
