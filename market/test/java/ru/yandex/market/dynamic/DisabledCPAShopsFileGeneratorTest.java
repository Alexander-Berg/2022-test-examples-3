package ru.yandex.market.dynamic;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see DisabledCPAShopsDataProvider
 */
class DisabledCPAShopsFileGeneratorTest extends FunctionalTest {
    @Autowired
    FileGenerator disabledCPAShopsFileGenerator;

    @Test
    @DbUnitDataSet(before = "DisabledCPAShopsFileGeneratorTest.before.csv")
    void shouldCreateDynamicFile() throws Exception {
        // when
        var file = disabledCPAShopsFileGenerator.generate(200100300L);

        // then
        try {
            assertThat(file).hasContent(StringTestUtil.getString(
                    getClass(),
                    "DisabledCPAShopsFileGeneratorTest.after.txt"
            ));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
