package ru.yandex.market.delivery.transport_manager.util.testdata;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.UtilityClass;
import org.apache.commons.beanutils.FluentPropertyBeanIntrospector;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class SetFieldValuesUtil {

    public static final OffsetDateTime DEFAULT_OFFSET_DATE_TIME = LocalDateTime
        .parse("2022-03-01T12:00")
        .atOffset(ZoneOffset.UTC);
    public static final Instant DEFAULT_INSTANT = DEFAULT_OFFSET_DATE_TIME.toInstant();

    public void setFieldValues(Object instance, boolean jsonOnly) {
        Class<?> dtoClass = instance.getClass();

        PropertyUtils.addBeanIntrospector(new FluentPropertyBeanIntrospector());

        for (PropertyDescriptor pd : PropertyUtils.getPropertyDescriptors(dtoClass)) {
            final Method readMethod = pd.getReadMethod();
            final Method writeMethod = pd.getWriteMethod();
            final Field field = toField(dtoClass, pd);

            if (readMethod == null || readMethod.getAnnotation(JsonIgnore.class) != null) {
                continue;
            }

            Class<?> propertyType = pd.getPropertyType();
            Consumer<Object> valueSetter = v -> {
                try {
                    if (writeMethod != null) {
                        writeMethod.invoke(instance, v);
                    } else if (field != null) {
                        field.set(instance, v);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            Class<?> elementType = getFirstGenericType(pd);
            setSomeValue(propertyType, valueSetter, elementType, jsonOnly);
        }
    }

    @Nullable
    private Field toField(Class<?> dtoClass, PropertyDescriptor pd) {
        try {
            final Field field;
            field = dtoClass.getDeclaredField(pd.getName());
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            // do nothing
            return null;
        }
    }

    @Nullable
    private static Class<?> getFirstGenericType(PropertyDescriptor pd) {
        Class<?> elementType = pd.getClass().getComponentType();
        if (elementType == null) {
            Type getterType = pd.getReadMethod().getGenericReturnType();
            if (getterType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) getterType).getActualTypeArguments();
                if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class<?>) {
                    elementType = (Class<?>) actualTypeArguments[0];
                }
            }
        }
        return elementType;
    }

    public static void setSomeValue(
        Class<?> propertyType,
        Consumer<Object> valueSetter,
        @Nullable Class<?> firstGenericType,
        boolean jsonOnly
    ) {
        if (propertyType == Integer.class || propertyType == int.class) {
            valueSetter.accept(1);
        } else if (propertyType == Long.class || propertyType == long.class) {
            valueSetter.accept(1L);
        } else if (propertyType == Double.class || propertyType == double.class) {
            valueSetter.accept(1.1D);
        } else if (propertyType == Float.class || propertyType == float.class) {
            valueSetter.accept(1.1F);
        } else if (propertyType == Boolean.class || propertyType == boolean.class) {
            valueSetter.accept(true);
        } else if (propertyType == String.class) {
            valueSetter.accept("Abc");
        } else if (propertyType.isArray()) {
            if (firstGenericType == byte.class) {
                valueSetter.accept(new byte[0]);
            }
        } else if (List.class.isAssignableFrom(propertyType)
            || propertyType == Collection.class) {
            List<?> container = initializeContainer(firstGenericType, jsonOnly, List::of, Collections.emptyList());
            valueSetter.accept(container);
        } else if (Set.class.isAssignableFrom(propertyType)) {
            Set<?> container = initializeContainer(firstGenericType, jsonOnly, Set::of, Collections.emptySet());
            valueSetter.accept(container);
        } else if (Map.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(Collections.emptyMap());
        } else if (LocalDate.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(DEFAULT_OFFSET_DATE_TIME.toLocalDate());
        } else if (LocalTime.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(DEFAULT_OFFSET_DATE_TIME.toLocalTime());
        } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(DEFAULT_OFFSET_DATE_TIME.toLocalDateTime());
        } else if (OffsetDateTime.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(DEFAULT_OFFSET_DATE_TIME);
        } else if (DateTimeInterval.class.isAssignableFrom(propertyType)) { // special lgw format
            valueSetter.accept(DateTimeInterval.fromFormattedValue("2020-07-12T12:00+03:00/2020-07-12T20:00+03:00"));
        } else if (Duration.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(Duration.ofSeconds(1));
        } else if (Instant.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(DEFAULT_INSTANT);
        } else if (Date.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(Date.from(DEFAULT_INSTANT));
        } else if (BigDecimal.class.isAssignableFrom(propertyType)) {
            valueSetter.accept(BigDecimal.valueOf(1L));
        } else if (propertyType.isEnum()) {
            valueSetter.accept(propertyType.getEnumConstants()[0]);
        } else if (isRegularDto(propertyType)) {
            valueSetter.accept(ObjectTestFieldValuesUtils.createAndFillInstance(propertyType, jsonOnly));
        } else {
            throw new IllegalStateException("Unsupported field type " + propertyType);
        }
    }

    @NotNull
    private static <X, C extends Collection<X>> C initializeContainer(
        @Nullable Class<?> firstGenericType,
        boolean jsonOnly,
        Function<X, C> creator,
        C empty
    ) {
        C container;
        if (firstGenericType != null) {
            AtomicReference<Object> x = new AtomicReference<>();
            setSomeValue(firstGenericType, x::set, null, jsonOnly);
            X value = (X) x.get();
            container = creator.apply(value);
        } else {
            container = empty;
        }
        return container;
    }

    private static boolean isRegularDto(Class<?> propertyType) {
        return !propertyType.isPrimitive()
            && !Collection.class.isAssignableFrom(propertyType)
            && !Map.class.isAssignableFrom(propertyType)
            && !propertyType.isEnum()
            && !propertyType.isArray()
            && !propertyType.isAnnotation()
            && !propertyType.isInterface();
    }
}
