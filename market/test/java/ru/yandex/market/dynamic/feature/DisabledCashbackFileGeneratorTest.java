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
 * Функциональный тест на {@link DisabledCashbackFileGenerator}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DisabledCashbackFileGeneratorTest.before.csv")
class DisabledCashbackFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledCashbackFileGenerator disabledCashbackFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика cashback-disabled-shops.json")
    void generateFile() {
        File disabledPromosFile = disabledCashbackFileGenerator.generate(0L);
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
