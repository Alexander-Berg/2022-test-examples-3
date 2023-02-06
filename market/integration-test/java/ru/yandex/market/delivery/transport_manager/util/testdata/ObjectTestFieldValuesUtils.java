package ru.yandex.market.delivery.transport_manager.util.testdata;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.admin.dto.property.DetailStringPropertyDto;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.AnyConstructor;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.DefaultConstructor;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.Instantiator;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.JsonBuilderWithDefaultConstructor;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.JsonCreatorConstructor;
import ru.yandex.market.delivery.transport_manager.util.testdata.instantiator.JsonCreatorStaticMethod;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class ObjectTestFieldValuesUtils {
    private static final Set<Class<?>> IGNORING_CLASSES = Set.of(
        DetailStringPropertyDto.class //generics
    );

    private static final List<Instantiator> INSTANTIATORS = List.of(
        new DefaultConstructor(),
        new JsonCreatorConstructor(),
        new JsonCreatorStaticMethod(),
        new JsonBuilderWithDefaultConstructor(new DefaultConstructor()),
        new JsonBuilderWithDefaultConstructor(new AnyConstructor()),
        new AnyConstructor()
    );

    @SneakyThrows
    public static <T> T createAndFillInstance(Class<T> dtoClass, boolean jsonOnly) {
        if (IGNORING_CLASSES.contains(dtoClass)) {
            return null;
        }

       return INSTANTIATORS.stream()
            .filter(i -> !jsonOnly || i.isJsonOnly())
            .map(i -> i.newInstance(dtoClass, jsonOnly))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException(
                "Class " + dtoClass + " has no sutable initializer!"));
    }

    @SneakyThrows
    public static <T> T callStaticMethod(Method m, boolean jsonOnly) {
        m.setAccessible(true);

        Class[] parameterTypes = m.getParameterTypes();
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class pType = parameterTypes[i];
            // TODO: добавить поддержку массивов
            SetFieldValuesUtil.setSomeValue(pType, params::add, null, jsonOnly);
        }
        return (T) m.invoke(null, params.toArray());
    }

    @SneakyThrows
    public static <T> T callConstructor(Constructor c, boolean jsonOnly) {
        c.setAccessible(true);

        Class[] parameterTypes = c.getParameterTypes();
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class pType = parameterTypes[i];
            // TODO: добавить поддержку массивов
            SetFieldValuesUtil.setSomeValue(pType, params::add, null, jsonOnly);
        }
        return (T) c.newInstance(params.toArray());
    }

    @SneakyThrows
    public static <T> T callBuilder(Object builder) {
        return (T) builder.getClass().getMethod("build").invoke(builder);
    }
}
