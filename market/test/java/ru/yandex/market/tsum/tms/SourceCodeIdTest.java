package ru.yandex.market.tsum.tms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.annotation.PersistenceConstructor;

import ru.yandex.market.tsum.pipe.engine.definition.common.SourceCodeEntity;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;

public class SourceCodeIdTest {

    @Test
    public void uniqueSourceCodeIdTest() {
        Collection<Class<? extends SourceCodeEntity>> entities = new ReflectionsSourceCodeProvider(
            ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE
        ).load();

        Map<UUID, Integer> classesById = new HashMap<>();
        for (Class<? extends SourceCodeEntity> entity : entities) {
            Optional<SourceCodeEntity> instance = tryToCreateInstance(entity);
            if (instance.isPresent() && instance.get().getSourceCodeId() != null) {
                classesById.compute(
                    instance.get().getSourceCodeId(),
                    (k, v) -> (v == null ? 0 : v) + 1
                );
            }
        }
        List<String> duplicatedIds = classesById.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey().toString())
            .collect(Collectors.toList());
        Assert.assertEquals("Some SourceCodeEntity has duplicate id: " + duplicatedIds.toString(), 0, duplicatedIds.size());
    }

    private Optional<SourceCodeEntity> tryToCreateInstance(Class<? extends SourceCodeEntity> clazz) {
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
            .filter(
                c -> c.getParameterCount() == 0 ||
                    c.getAnnotation(PersistenceConstructor.class) != null &&
                        c.getAnnotation(JsonCreator.class) != null
            )
            .collect(Collectors.toList());

        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
            try {
                return Optional.of((SourceCodeEntity) constructor.newInstance(
                    IntStream.range(0, constructor.getParameterCount())
                        .mapToObj(i -> null)
                        .toArray()
                ));
            } catch (
                InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException ignore
            ) {
            }
        }
        return Optional.empty();
    }

}
