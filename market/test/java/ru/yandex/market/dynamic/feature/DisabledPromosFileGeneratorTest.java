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
 * Функциональыне тесты на {@link DisabledPromosFileGenerator}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DisabledPromosFileGeneratorTest.before.csv")
class DisabledPromosFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledPromosFileGenerator disabledPromosFileGenerator;

    @Test
    @DisplayName("Проверка магазинов, попадающих в Dynamic")
    void generateDynamicFile() {
        File disabledPromosFile = disabledPromosFileGenerator.generate(0L);
        try {
            List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledPromosFile);
            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1, 2, 5)
            );
        } finally {
            FileUtils.deleteQuietly(disabledPromosFile);
        }
    }
}
