package ru.yandex.direct.model;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.primitives.Primitives;
import org.apache.commons.beanutils.PropertyUtils;
import org.assertj.core.api.SoftAssertions;
import org.jooq.types.ULong;
import org.junit.Test;
import org.reflections.Reflections;

import ru.yandex.misc.dataSize.DataSize;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;

/**
 * ВАЖНО!
 * <p>
 * Параметризованный тест, используется для запуска в других модулях для тестирования моделей.
 * Для запуска тестов из IDEA необходимо снавигироваться в необходимый suite в нужном модyле
 * (например в core ru.yandex.direct.core.ForeignTestSuite)
 * и запустить тесты для моделей там.
 * <p>
 * ВАЖНО: Тестов очень много, а ya test_tool создаёт по лог файлу на каждый запуск, поэтому использовать
 * Parametrized крайне расточительно, используем SoftAssertions
 */
public abstract class AllModelPropsCorrespondToBeanProps {

    public static final String PACKAGE_NAME = "ru.yandex.direct";
    public static final Set<Class<?>> BROKEN_CLASSES = new HashSet<>(singleton(TestBrokenPropertiesClass.class));

    class Case {
        private ModelProperty modelProperty;
        private PropertyDescriptor propertyDescriptor;
        private Model model;
        private Object propertyValue;
        private boolean isPrimitiveProperty;

        Case(ModelProperty modelProperty, Class<Model> modelClass) {
            try {
                this.modelProperty = modelProperty;
                this.model = modelClass.getConstructor().newInstance();
                this.propertyDescriptor = PropertyUtils.getPropertyDescriptor(this.model, modelProperty.name());

                if (propertyDescriptor == null) {
                    throw new IllegalStateException("propertyDescriptor is null for class " + modelClass.getName()
                            + " and property " + modelProperty.name());
                }

                if (propertyDescriptor.getPropertyType().isPrimitive()) {
                    this.isPrimitiveProperty = true;
                    this.propertyValue = createValueForProperty(Primitives.wrap(propertyDescriptor.getPropertyType()));
                } else {
                    this.isPrimitiveProperty = false;
                    this.propertyValue = createValueForProperty(propertyDescriptor.getPropertyType());
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    protected abstract Map<Class<?>, Supplier<?>> getCustomClassFactories();

    @SuppressWarnings("unchecked")
    public Collection<Case> testCases() {
        Reflections reflections = new Reflections(PACKAGE_NAME);
        return reflections.getSubTypesOf(Model.class)
                .stream()
                .filter(cls -> !BROKEN_CLASSES.contains(cls))
                .filter(cls -> !Modifier.isAbstract(cls.getModifiers()))
                .flatMap(model -> Stream.of(model.getDeclaredFields())
                        .filter(f -> Modifier.isStatic(f.getModifiers()))
                        .filter(f -> ModelProperty.class.isAssignableFrom(f.getType())))
                .map(f -> {
                    ModelProperty modelProperty;
                    try {
                        modelProperty = (ModelProperty) f.get(null);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    return new Case(modelProperty, (Class<Model>) f.getDeclaringClass());
                })
                .collect(toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setViaModelPropSetsValueToBeanProp() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        for (Case testCase : testCases()) {
            // Проверяем, что свойство не является свойством только для чтения
            if (testCase.propertyDescriptor.getWriteMethod() == null) {
                continue;
            }

            testCase.modelProperty.set(testCase.model, testCase.propertyValue);

            Object actualValue = testCase.propertyDescriptor.getReadMethod().invoke(testCase.model);
            if (testCase.isPrimitiveProperty) {
                soft.assertThat(actualValue)
                        .as(String.format("не удалось выставить значение ModelProperty через Bean Property %s.%s",
                                testCase.model.getClass().getCanonicalName(), testCase.modelProperty.name()))
                        .isEqualTo(testCase.propertyValue);
            } else {
                soft.assertThat(actualValue)
                        .as(String.format("не удалось выставить значение ModelProperty через Bean Property %s.%s",
                                testCase.model.getClass().getCanonicalName(), testCase.modelProperty.name()))
                        .isSameAs(testCase.propertyValue);
            }
        }

        soft.assertAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setViaBeanPropSetsValueToModelProp() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        for (Case testCase : testCases()) {
            // для ModelProperty публичный сеттер не обязателен
            if (testCase.propertyDescriptor.getWriteMethod() == null) {
                continue;
            }

            testCase.propertyDescriptor.getWriteMethod().invoke(testCase.model, testCase.propertyValue);

            Object actualValue = testCase.modelProperty.get(testCase.model);
            soft.assertThat(actualValue)
                    .as(String.format("не удалось выставить значение ModelProperty через Bean Property %s.%s",
                            testCase.model.getClass().getCanonicalName(), testCase.modelProperty.name()))
                    .isSameAs(testCase.propertyValue);
        }

        soft.assertAll();
    }

    private Object createValueForProperty(Class<?> propertyType) {
        if (String.class.isAssignableFrom(propertyType)) {
            return "some value";
        } else if (Boolean.class.isAssignableFrom(propertyType)) {
            return true;
        } else if (Optional.class.isAssignableFrom(propertyType)) {
            return Optional.empty();
        } else if (Integer.class.isAssignableFrom(propertyType)) {
            return 244839029;
        } else if (Short.class.isAssignableFrom(propertyType)) {
            return (short) 453;
        } else if (Long.class.isAssignableFrom(propertyType)) {
            return 9382918238L;
        } else if (Float.class.isAssignableFrom(propertyType)) {
            return 293.287f;
        } else if (Double.class.isAssignableFrom(propertyType)) {
            return 193238928.2938138d;
        } else if (BigInteger.class.isAssignableFrom(propertyType)) {
            return BigInteger.valueOf(989780345);
        } else if (BigDecimal.class.isAssignableFrom(propertyType)) {
            return BigDecimal.valueOf(19391838.839);
        } else if (BigInteger.class.isAssignableFrom(propertyType)) {
            return BigInteger.valueOf(19391838);
        } else if (ULong.class.isAssignableFrom(propertyType)) {
            return ULong.valueOf(9382918238L);
        } else if (LocalDate.class.isAssignableFrom(propertyType)) {
            return LocalDate.now();
        } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
            return LocalDateTime.now();
        } else if (ZonedDateTime.class.isAssignableFrom(propertyType)) {
            return ZonedDateTime.now();
        } else if (ZoneId.class.isAssignableFrom(propertyType)) {
            return ZoneId.systemDefault();
        } else if (OffsetDateTime.class.isAssignableFrom(propertyType)) {
            return OffsetDateTime.now();
        } else if (propertyType.getEnumConstants() != null) {
            return propertyType.getEnumConstants()[0];
        } else if (List.class.isAssignableFrom(propertyType)) {
            return new ArrayList<>();
        } else if (EnumSet.class.isAssignableFrom(propertyType)) {
            return EnumSet.noneOf(DummyEnum.class);
        } else if (Set.class.isAssignableFrom(propertyType)) {
            return new HashSet<>();
        } else if (Map.class.isAssignableFrom(propertyType)) {
            return new HashMap<>();
        } else if (Collection.class.isAssignableFrom(propertyType)) {
            return new ArrayList<>();
        } else if (DataSize.class.isAssignableFrom(propertyType)) {
            return DataSize.fromBytes(123L);
        } else if (Duration.class.isAssignableFrom(propertyType)) {
            return Duration.ofSeconds(12);
        } else if (byte[].class.isAssignableFrom(propertyType)) {
            return "byte array".getBytes();
        } else if (getCustomClassFactories().containsKey(propertyType)) {
            return getCustomClassFactories().get(propertyType).get();
        } else {
            return mock(propertyType);
        }
    }

    /**
     * Пустой {@code enum} для создания пустого {@link EnumSet}'а.
     */
    private enum DummyEnum {
    }
}
