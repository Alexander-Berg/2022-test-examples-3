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
 * Функциональный тест на {@link DisabledShopLogoFileGenerator}.
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "DisabledShopLogoFileGeneratorTest.before.csv")
class DisabledLogoFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledShopLogoFileGenerator disabledShopLogoFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика shop-logo-disabled-shops.json")
    void generateFile() {
        File disabledShopLogosFile = disabledShopLogoFileGenerator.generate(0L);
        try {
            List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledShopLogosFile);
            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1, 2, 5, 8)
            );
        } finally {
            FileUtils.deleteQuietly(disabledShopLogosFile);
        }
    }
}
