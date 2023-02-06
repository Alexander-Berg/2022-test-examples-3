package ru.yandex.market.core.param.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;

/**
 * Тесты для {@link DeliveryParamsListener}
 */

@DbUnitDataSet(before = "DeliveryParamsListenerTest.before.csv")
public class DeliveryParamsListenerTest extends FunctionalTest {

    @Autowired
    ParamService paramService;

    @Test
    @DisplayName("Отключаем IgnoreStocks")
    @DbUnitDataSet(after = "DeliveryParamsListenerTest.afterCCoff.csv")
    void setIgnoreStocksToFalse() {
        paramService.setParam(new BooleanParamValue(ParamType.IGNORE_STOCKS, 1L, false), 100500L);

    }

    @Test
    @DisplayName("Включаем IgnoreStocks")
    @DbUnitDataSet(after = "DeliveryParamsListenerTest.afterCCon.csv")
    void setIgnoreStocksToTrue() {
        paramService.setParam(new BooleanParamValue(ParamType.IGNORE_STOCKS, 104L, true), 100500L);

    }

    @Test
    @DisplayName("Выключаем DropshipAvailable")
    @DbUnitDataSet(after = "DeliveryParamsListenerTest.afterDAoff.csv")
    void setDropshipAvailableToFalse() {
        paramService.setParam(new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 1L, false), 100500L);

    }

}
