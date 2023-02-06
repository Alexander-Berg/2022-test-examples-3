package ru.yandex.market.dynamic;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class IgnoreStockCPAShopsDataProviderTest extends FunctionalTest {
    @Autowired
    FileGenerator ignoreStockCPAShopsFileGenerator;


    @Test
    @DbUnitDataSet(before = "IgnoreStockCPAShopsDataProviderTest.before.csv")
    void shouldCreateDynamicFile() {
        // when
        var file = ignoreStockCPAShopsFileGenerator.generate(200100300L);

        // then
        try {
            assertThat(file).hasContent(StringTestUtil.getString(
                    getClass(),
                    "IgnoreStockCPAShopsDataProviderTest.after.txt"
            ));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
