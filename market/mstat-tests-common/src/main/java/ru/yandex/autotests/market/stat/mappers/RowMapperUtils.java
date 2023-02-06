package ru.yandex.autotests.market.stat.mappers;

import com.google.common.base.CaseFormat;
import org.fest.util.Strings;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
import ru.yandex.autotests.market.stat.date.DatePatterns;
import ru.yandex.autotests.market.stat.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by entarrion on 07.09.16.
 */
public class RowMapperUtils {

    public static <T> T mapRow(Class<T> clazz, ResultSet rs, int rowNum) throws SQLException {
        return createMapperFromBean(clazz, Column.class, Column::name).mapRow(rs, rowNum);
    }

    public static <T, D extends Annotation> RowMapper<T> createMapperFromBean(Class<T> clazz, Class<D> annotationClass, Function<D, String> annotationValueFunction) {
        return createMapperFromBean(clazz, annotationClass, annotationValueFunction, new ArrayList<>(), false);
    }

    public static <T, D extends Annotation> RowMapper<T> createMapperFromBean(Class<T> clazz, Class<D> annotationClass, Function<D, String> annotationValueFunction, List<String> fieldsToSkip, boolean setNull) {
        return (rs, rowNum) -> {
            T result = ReflectionUtils.instantiate(clazz);
            for (Field field : getAllFields(clazz)) {
                D column = field.getAnnotation(annotationClass);
                if (column == null) {
                    continue;
                }
                String annotationValue = annotationValueFunction.apply(column);
                String columnName = annotationValue.isEmpty() ? field.getName() : annotationValue;
                Object value = fieldsToSkip.contains(columnName) ? null : getObjectValidValue(columnName, field, rs);
                value = setNull && ("".equals(value) || "0".equals(value)) ? null : value;
                ReflectionUtils.setField(field, result, value);
            }
            return result;
        };
    }

    private static Field[] getAllFields(Class clazz) {
        List<Field> result = new ArrayList<>();
        while (clazz.getSuperclass() != null) {
            result.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return result.toArray(new Field[result.size()]);
    }

    private static Object getObjectValidValue(String columnName, Field field, ResultSet rs) throws SQLException {
        if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
            return rs.getInt(columnName);
        } else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
            return rs.getLong(columnName);
        } else if (field.getType().equals(float.class) || field.getType().equals(Float.class)) {
            return rs.getFloat(columnName);
        } else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
            return rs.getDouble(columnName);
        } else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
            return rs.getBoolean(columnName);
        } else if (field.getType().equals(LocalDateTime.class)) {
            return DatePatterns.parseByFirstMatchingPattern(rs.getString(columnName));
        }
        try {
            return rs.getString(columnName);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static <D extends Annotation> String getFieldName(Field f, Class<D> annotationClass, Function<D, String> annotationValueFunction) {
        if (f.isAnnotationPresent(annotationClass)) {
            String value = annotationValueFunction.apply(f.getAnnotation(annotationClass));
            if (!Strings.isNullOrEmpty(value)) {
                return value;
            }
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, f.getName());
    }

    public static String getFieldName(Field f) {
        return getFieldName(f, org.beanio.annotation.Field.class, org.beanio.annotation.Field::name);
    }

}
