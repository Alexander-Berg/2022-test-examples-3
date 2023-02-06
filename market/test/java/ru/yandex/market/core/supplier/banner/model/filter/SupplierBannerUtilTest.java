package ru.yandex.market.core.supplier.banner.model.filter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SupplierBannerUtilTest {

    /**
     * Поля которые не нужно проверять, либо сложно проверить.
     */
    private static final Collection<String> NOT_CHECKED_FIELDS =
            Arrays.asList("id", "pageIds", "customConditions", "bannerType", "suppliersTypes",
                    "supplierAlivenessState", "useSuppliersWithDraftOffers");

    /**
     * Тест проверяет что если хотя бы одно поле заполнено, то условие "нет активных фильтров" вернет false.
     * Нужен для того чтобы не забыли протащить новое поле в {@link SupplierBannerUtil#areThereNoActiveFilters}
     * Если тест упал и поле действительно забыли добавить,
     * то добавить поле в {@link SupplierBannerUtil#areThereNoActiveFilters}.
     * Если поле добавили но тест падает, то нужно его прописать в {@link #NOT_CHECKED_FIELDS}
     *
     * @throws IllegalAccessException
     */
    @Test
    void testIfAnyFieldSet() throws IllegalAccessException {
        List<Field> declaredFields = Arrays.stream(SupplierBannerFilter.class.getDeclaredFields())
                .filter(f -> !NOT_CHECKED_FIELDS.contains(f.getName())).collect(Collectors.toList());
        for(Field f : declaredFields) {
            SupplierBannerFilter filter = new SupplierBannerFilter.Builder().setId("123").build();
            f.setAccessible(true);
            if (Collection.class.isAssignableFrom(f.getType())) {
                Type genericFieldType = f.getGenericType();
                if(genericFieldType instanceof ParameterizedType) {
                    ParameterizedType aType = (ParameterizedType) genericFieldType;
                    Type[] fieldArgTypes = aType.getActualTypeArguments();
                    Collection collection = (Collection)EnhancedRandom.random(f.getType());
                    collection.add(EnhancedRandom.random((Class)fieldArgTypes[0]));
                    f.set(filter, collection);
                }
            } else {
                f.set(filter, EnhancedRandom.random(f.getType()));
            }
            Assertions.assertFalse(SupplierBannerUtil.areThereNoActiveFilters(filter));
        }
    }

    @Test
    void testNoOneFieldSet() {
        Assertions.assertTrue(SupplierBannerUtil.areThereNoActiveFilters(
                new SupplierBannerFilter.Builder().setId("123").build()));
    }
}
