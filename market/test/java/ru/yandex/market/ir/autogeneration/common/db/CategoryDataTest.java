package ru.yandex.market.ir.autogeneration.common.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingFormula;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

public class CategoryDataTest {

    /**
     * Проверяем, что коллекции возвращаемые из кэша immutable, если коллекции mutable - это может приводить к
     * сложным багам.
     */
    @Test
    public void categoryDataImmutabilityTest() {
        CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
                .setHid(1000)
                .setLeaf(true)
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID)
                        .setXslName(CategoryData.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.BARCODE_ID)
                        .setXslName(CategoryData.BAR_CODE)
                        .setValueType(MboParameters.ValueType.STRING)
                        .setMultivalue(true))
                .build());
        Method[] declaredMethods = CategoryData.class.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?> returnType = method.getReturnType();

            if (returnType.equals(void.class)
                    || returnType.equals(boolean.class)
                    || returnType.equals(Long.class)
                    || returnType.equals(long.class)
                    || returnType.equals(MboParameters.Parameter.class)
                    || returnType.equals(MboParameters.Unit.class)
                    || returnType.equals(String.class)
                    || returnType.equals(Integer.class)
                    || returnType.equals(CategoryData.class)
                    || returnType.equals(OptionRestrictionIndex.class)
                    || returnType.equals(SkuRatingFormula.class)
                    || returnType.equals(int.class)
            ) {
                //методы с таким return типом пропускаем
                continue;
            } else if (method.getParameterCount() != 0) {
                System.out.println("skip method " + method.getName() + " with arguments");
                continue;
            } else if (Collection.class.isAssignableFrom(returnType)) {
                try {
                    Collection invoke = (Collection) method.invoke(categoryData);
                    Assertions.assertThatThrownBy(() -> invoke.remove(1L))
                            .isExactlyInstanceOf(UnsupportedOperationException.class);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Something went wrong with method " + method, e);
                }
            } else if (Map.class.isAssignableFrom(returnType)) {
                try {
                    Map invoke = (Map) method.invoke(categoryData);
                    Assertions.assertThatThrownBy(() -> invoke.remove(1L))
                            .isExactlyInstanceOf(UnsupportedOperationException.class);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Something went wrong with method " + method, e);
                }
            } else {
                Assertions.fail("Method " + method.getName() + " has new return type " +
                        returnType + ". Add immutability check for this type or to ignored ones (if immutablity is " +
                        "not needed)");
            }
        }
    }
}
