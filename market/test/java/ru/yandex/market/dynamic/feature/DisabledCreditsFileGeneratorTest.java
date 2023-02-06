package ru.yandex.market.dynamic.feature;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

/**
 * Функциональный тест для {@link DisabledCreditsFileGenerator}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "DisabledCreditsFileGeneratorTest.before.csv")
class DisabledCreditsFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledCreditsFileGenerator disabledCreditsFileGenerator;

    @Autowired
    private FeatureService featureService;

    @Test
    @DisplayName("Проверка формирования файла динамика credits-disabled-shops.json")
    void generateFile() throws Exception {
        //проверяем, что в первый раз включенная фича в статусе NEW не попадет в файл
        featureService.changeStatus(1L, ShopFeature.of(5L, FeatureType.CREDITS, ParamCheckStatus.NEW));

        File disabledCreditsFile = disabledCreditsFileGenerator.generate(0L);

        try {
            List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledCreditsFile);
            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1, 2)
            );
        } finally {
            FileUtils.deleteQuietly(disabledCreditsFile);
        }
    }
}
