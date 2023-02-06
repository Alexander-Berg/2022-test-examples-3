package ru.yandex.market.mbi.tariffs.mvc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.swagger.annotations.ApiParam;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.api.DraftsApi;
import ru.yandex.market.mbi.tariffs.api.TariffsApi;
import ru.yandex.market.mbi.tariffs.mvc.validate.ValidateUtils;

import static org.junit.Assert.assertTrue;

public class ValidateUtilsTest extends FunctionalTest {
    @ParameterizedTest(name = "[{index}] {0}.{1}")
    @MethodSource("testSortByFieldData")
    void testSortByFields(
            Class<?> apiClass,
            String methodName,
            int parameterNumber,
            Map<String, String> availableMap
    ) {
        Method method = findMethod(methodName, apiClass);
        checkAllowableValues(method.getParameters()[parameterNumber], methodName, availableMap);
    }

    private static Stream<Arguments> testSortByFieldData() {
        return Stream.of(
                Arguments.of(DraftsApi.class, "getDrafts", 0, ValidateUtils.CONVERTER_DRAFT_SORT_BY),
                Arguments.of(DraftsApi.class, "findDrafts", 1, ValidateUtils.CONVERTER_DRAFT_SORT_BY),
                Arguments.of(TariffsApi.class, "getTariffs", 0, ValidateUtils.CONVERTER_TARIFFS_SORT_BY),
                Arguments.of(TariffsApi.class, "findTariffs", 1, ValidateUtils.CONVERTER_TARIFFS_SORT_BY)
        );
    }

    private Method findMethod(String methodName, Class<?> apiClass) {
        return Arrays.stream(apiClass.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No method [" + methodName + "] in " + apiClass.getSimpleName()));
    }

    private void checkAllowableValues(
            Parameter sortByParameter,
            String methodName,
            Map<String, String> validateMap
    ) {
        ApiParam apiParam = sortByParameter.getAnnotation(ApiParam.class);
        String allowableValues = apiParam.allowableValues();
        String[] allowableValueArray = allowableValues.split(", ");

        Map<String, String> map = new HashMap<>(validateMap);
        List<String> unknownValues = new ArrayList<>();
        for (String value : allowableValueArray) {
            if (!map.containsKey(value)) {
                unknownValues.add(value);
                continue;
            }
            map.remove(value);
        }

        String valuesNotPresentedInMap = "";
        if (!unknownValues.isEmpty()) {
            valuesNotPresentedInMap = "Unknown values in openApi specification: " + unknownValues + ".";
        }

        String valuesNotPresentedInOpenapi = "";
        if (!map.isEmpty()) {
            valuesNotPresentedInOpenapi = "Following values is not presented in specification: " + map.keySet();
        }

        assertTrue(
                "Method " + methodName + ": " + valuesNotPresentedInMap + valuesNotPresentedInOpenapi,
                valuesNotPresentedInMap.isEmpty() && valuesNotPresentedInOpenapi.isEmpty()
        );

    }
}
