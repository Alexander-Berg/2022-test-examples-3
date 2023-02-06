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
 * Тесты для {@link DisabledCutPriceFileGenerator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "DisabledCutPriceFileGeneratorTest.before.csv")
class DisabledCutPriceFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledCutPriceFileGenerator disabledCutPriceFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика cutprice-disabled-shops.json")
    void generateFile() throws Exception {
        File disabledPromosFile = disabledCutPriceFileGenerator.generate(0L);
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
