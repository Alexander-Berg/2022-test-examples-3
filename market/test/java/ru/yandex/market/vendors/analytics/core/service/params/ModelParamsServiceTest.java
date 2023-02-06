package ru.yandex.market.vendors.analytics.core.service.params;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.model.DetalizationLevel;
import ru.yandex.market.vendors.analytics.core.model.params.ModelsParamsResult;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 */
@ClickhouseDbUnitDataSet(before = "ModelParamsServiceTest.before.csv")
class ModelParamsServiceTest extends FunctionalTest {

    @Autowired
    private ModelParamsService modelParamsService;

    @Test
    void loadParams() {
        ModelsParamsResult modelsParamsResult = modelParamsService.loadModelParams(
                91491,
                List.of(10001L, 10002L),
                DetalizationLevel.MODEL,
                2
        );
        assertEquals(2, modelsParamsResult.getParameterInfos().size());
        assertEquals(Set.of("Смартфон"), modelsParamsResult.get(10001, 101));
        assertEquals(Set.of("Eight inches"), modelsParamsResult.get(10001, 102));
        assertEquals(Set.of("Кнопочный"), modelsParamsResult.get(10002, 101));
        assertTrue(modelsParamsResult.get(10001, 106).isEmpty());
        assertTrue(modelsParamsResult.get(10002, 102).isEmpty());
        assertTrue(modelsParamsResult.get(10002, 106).isEmpty());
    }
}