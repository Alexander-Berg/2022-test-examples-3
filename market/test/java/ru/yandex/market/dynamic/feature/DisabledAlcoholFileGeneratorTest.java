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
 * Функциональный тест для {@link DisabledAlcoholFileGenerator}.
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "DisabledAlcoholFileGeneratorTest.before.csv")
class DisabledAlcoholFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledAlcoholFileGenerator disabledAlcoholFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика alcohol-disabled-shops.json")
    void generateFile() throws Exception {
        final File disabledAlcoholFile = disabledAlcoholFileGenerator.generate(0L);
        try {
            final List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledAlcoholFile);

            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1, 2, 5)
            );
        } finally {
            FileUtils.deleteQuietly(disabledAlcoholFile);
        }
    }

}
