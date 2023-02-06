package ru.yandex.market.core.cache;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.misc.EnumRandomizer;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.transcoders.WhalinTranscoder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.common.cache.memcached.MemCachedService;
import ru.yandex.common.cache.memcached.cacheable.LocalMemCacheable;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;
import ru.yandex.common.util.reflect.ReflectionUtils;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit-тесты для проверки {@link MemCachedService}.
 *
 * @author Vladislav Bauer
 */
public abstract class AbstractCacheServicesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheServicesTest.class);
    private static final EnhancedRandom OBJECT_GENERATOR = createObjectGenerator();
    private static final List<Transcoder<Object>> TRANSCODERS = List.of(new WhalinTranscoder());

    private final String[] packages;


    protected AbstractCacheServicesTest(@Nonnull final String... packages) {
        this.packages = packages;
    }

    private static EnhancedRandom createObjectGenerator() {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(Map.class, (Supplier<Map>) HashMap::new)
                .randomize(ParamValue.class, (Supplier<ParamValue>) () -> new BooleanParamValue(0, 0, 0, false))
                .randomize(FeatureCustomCutoffType.class, new EnumRandomizer<>(CommonCutoffs.class))
                .build();
    }

    @Test
    void testPackages() {
        Reflections.log = null;

        for (final String packageName : packages) {
            checkPackage(packageName);
        }
    }

    private void checkPackage(final String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final List<Class<? extends MemCachedService>> serviceClasses = findMemCachedServices(reflections);

        for (final Class<? extends MemCachedService> serviceClass : serviceClasses) {
            checkServiceClass(serviceClass);
        }
    }

    private List<Class<? extends MemCachedService>> findMemCachedServices(Reflections reflections) {
        final Set<Class<? extends MemCachedService>> allServiceClasses =
                reflections.getSubTypesOf(MemCachedService.class);

        return allServiceClasses.stream()
                .filter(c -> !c.isInterface())
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    private void checkServiceClass(final Class<? extends MemCachedService> serviceClass) {
        final MemCachedService service = createMemCachedService(serviceClass);
        final Collection<Field> cacheFields = findCacheableFields(service);

        for (final Field field : cacheFields) {
            checkField(field);
        }
    }

    private Collection<Field> findCacheableFields(final MemCachedService service) {
        final Class<? extends MemCachedService> serviceClass = service.getClass();
        final Field[] declaredFields = serviceClass.getDeclaredFields();

        return Arrays.stream(declaredFields)
                .filter(f -> MemCacheable.class.isAssignableFrom(f.getType()))
                .filter(f -> {
                    final Object fieldValue = ReflectionUtils.readPrivateField(service, f.getName());
                    return !(fieldValue instanceof LocalMemCacheable);
                })
                .sorted(Comparator.comparing(Field::getName))
                .collect(Collectors.toList());
    }

    private MemCachedService createMemCachedService(final Class<? extends MemCachedService> serviceClass) {
        try {
            final Set<Constructor> constructors = org.reflections.ReflectionUtils.getConstructors(serviceClass);
            final Constructor<?> constructor = constructors.stream()
                    .min(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow(() -> new RuntimeException("Could not find constructors for " + serviceClass));

            final boolean accessible = constructor.isAccessible();
            constructor.setAccessible(true);

            try {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                final Object[] parameters = Arrays.stream(parameterTypes)
                        .map(p -> {
                            try {
                                return Mockito.mock(p);
                            } catch (final Exception ex) {
                                return OBJECT_GENERATOR.nextObject(p);
                            }
                        })
                        .toArray();

                return (MemCachedService) constructor.newInstance(parameters);
            } finally {
                constructor.setAccessible(accessible);
            }
        } catch (final Exception ex) {
            throw new RuntimeException("Could not instantiate " + serviceClass, ex);
        }
    }

    private void checkField(Field field) {
        final ParameterizedType genericType = (ParameterizedType) field.getGenericType();

        // Проверяем что cacheable содержит типы для ключа/значения
        final Type[] typeArguments = genericType.getActualTypeArguments();
        assertThat(typeArguments, arrayWithSize(2));

        // Проверяем что тип значения сериализуем
        final Type valueType = typeArguments[0];
        final Class<?> clazz = checkType(valueType);
        if (clazz != null) {
            fail(String.format("Could not serialize %s in %s", clazz, field));
        }
    }

    private Class<?> checkType(final Type type) {
        // Если тип - простой класс
        if (type instanceof Class) {
            final Class<?> clazz = (Class<?>) type;
            final Class<?> badTypeClass = checkClass(clazz);

            if (badTypeClass != null) {
                return badTypeClass;
            }
        }
        // Если тип - параметризованный класс
        else if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final Type[] typeArguments = parameterizedType.getActualTypeArguments();

            // Проверяем что сам класс сериализуем (для интерфейсов коллекций используются сериалиуемые реализации)
            final Type rawType = parameterizedType.getRawType();
            final Class<?> badRawTypeClass = checkType(rawType);
            if (badRawTypeClass != null) {
                return badRawTypeClass;
            }

            // Проверяем что параметры так же сериализуемы
            for (final Type typeArgument : typeArguments) {
                final Class<?> badTypeArgumentClass = checkType(typeArgument);
                if (badTypeArgumentClass != null) {
                    return badTypeArgumentClass;
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported type " + type);
        }
        return null;
    }

    private Class<?> checkClass(final Class<?> clazz) {
        try {
            for (final Transcoder<Object> transcoder : TRANSCODERS) {
                final Serializable random = (Serializable) OBJECT_GENERATOR.nextObject(clazz);
                final CachedData cachedData = transcoder.encode(random);
                final Object decoded = transcoder.decode(cachedData);

                Assertions.assertThat(decoded)
                        .usingRecursiveComparison()
                        .isEqualTo(random);

                if (decoded != null) {
                    return null;
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Could not serialize class {}", clazz, ex);
        }
        return clazz;
    }

}
