package ru.yandex.market.core.param.listener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(before = "ProhibitPIOrderProcessingParamListenerTest.before.csv")
public class ProhibitPIOrderProcessingParamListenerTest extends FunctionalTest {

    @Autowired
    private ParamService paramService;

    @Test
    @DbUnitDataSet(after = "ProhibitPIOrderProcessingParamListenerTest.allowed.csv")
    void testSetProhibitPiOrderProcessing_allowed() {
        paramService.setParam(new BooleanParamValue(ParamType.PROHIBIT_PI_ORDER_PROCESSING, 1, true), 100500L);
    }

    @Test
    @DbUnitDataSet(before = "ProhibitPIOrderProcessingParamListenerTest.notAllowed.before.csv",
            after = "ProhibitPIOrderProcessingParamListenerTest.notAllowed.before.csv")
    void testSetProhibitPiOrderProcessing_NotAllowed() {
        assertThrows(IllegalArgumentException.class, () ->
                paramService.setParam(new BooleanParamValue(ParamType.PROHIBIT_PI_ORDER_PROCESSING, 1, true), 100500L));
    }
}
