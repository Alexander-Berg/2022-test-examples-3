package ru.yandex.market.core.param;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;

public class ParamServiceTest extends FunctionalTest {

    private static final long SHOP_ID = 774;

    @Autowired
    private ParamService paramService;

    private static boolean getBooleanParamValueOrDefault(
            Map<ParamType, List<ParamValue>> paramValues, ParamType paramType) {
        return paramValues.getOrDefault(paramType, Collections.emptyList())
                .stream()
                .findFirst()
                .map(ParamValue::getValueAsBoolean)
                .orElse((Boolean) paramType.getDefaultValue());
    }

    @Test
    @DbUnitDataSet(before = "ParamServiceTest.before.csv")
    void directSuppliesAvailableTest() {
        Map<ParamType, List<ParamValue>> paramValues1 =
                paramService.getParams(
                        SHOP_ID,
                        ImmutableSet.of(ParamType.DIRECT_SUPPLIES_AVAILABLE)
                ).getBackingMap();
        Map<ParamType, List<ParamValue>> paramValues2 =
                paramService.getParams(
                        775,
                        ImmutableSet.of(ParamType.DIRECT_SUPPLIES_AVAILABLE)
                ).getBackingMap();
        boolean actual1 = getBooleanParamValueOrDefault(paramValues1, ParamType.DIRECT_SUPPLIES_AVAILABLE);
        boolean actual2 = getBooleanParamValueOrDefault(paramValues2, ParamType.DIRECT_SUPPLIES_AVAILABLE);

        Assertions.assertFalse(actual1);
        Assertions.assertTrue(actual2);
    }
}
