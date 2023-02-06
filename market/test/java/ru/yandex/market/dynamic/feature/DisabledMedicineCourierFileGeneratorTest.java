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

/**
 * Функциональный тест для {@link DisabledMedicineCourierFileGenerator}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "DisabledMedicineCourierFileGeneratorTest.before.csv")
class DisabledMedicineCourierFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledMedicineCourierFileGenerator disabledMedicineCourierFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика alcohol-disabled-shops.json")
    void generateFile() throws Exception {
        final File disabledMedicineCourierFile = disabledMedicineCourierFileGenerator.generate(0L);
        try {
            final List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledMedicineCourierFile);

            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1)
            );
        } finally {
            FileUtils.deleteQuietly(disabledMedicineCourierFile);
        }
    }
}
