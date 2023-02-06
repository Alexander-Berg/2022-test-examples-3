package ru.yandex.market.dynamic.feature;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.dynamic.FileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see DisabledSubsidyDataProvider
 */
class SubsidiesFileGeneratorTest extends FunctionalTest {
    @Autowired
    private FileGenerator disabledSubsidyFileGenerator;

    @Test
    @DbUnitDataSet(before = "subsidies.before.csv")
    void generateInDb() {
        // when
        var file = disabledSubsidyFileGenerator.generate(100500L);

        // then
        try {
            assertThat(file).hasContent(StringTestUtil.getString(
                    getClass(),
                    "subsidies.after.txt"
            ));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
