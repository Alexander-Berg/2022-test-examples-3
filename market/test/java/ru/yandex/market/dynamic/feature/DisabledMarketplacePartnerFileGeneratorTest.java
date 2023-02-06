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
 * Тесты для {@link DisabledMarketplacePartnerFileGenerator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "DisabledMarketplacePartnerFileGeneratorTest.before.csv")
public class DisabledMarketplacePartnerFileGeneratorTest extends FunctionalTest {

    @Autowired
    private DisabledMarketplacePartnerFileGenerator disabledMarketplacePartnerFileGenerator;

    @Test
    @DisplayName("Проверка формирования файла динамика marketplace-partner-disabled-shops.json")
    void generateFile() throws Exception {
        final File disabledPromosFile = disabledMarketplacePartnerFileGenerator.generate(0L);
        try {
            final List<Long> disabledShopIds = DynamicJsonReader.readFile(disabledPromosFile);
            MatcherAssert.assertThat(
                    disabledShopIds,
                    Matchers.containsInAnyOrder(1, 2, 5)
            );
        } finally {
            FileUtils.deleteQuietly(disabledPromosFile);
        }
    }
}
