package ru.yandex.market.dynamic;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see DisabledAuctionStrategyDynamicDataProvider
 */
class DisabledAuctionStrategyGeneratorTest extends FunctionalTest {
    @Autowired
    private FileGenerator disabledAuctionStrategyGenerator;

    @Test
    @DbUnitDataSet(before = "DisabledAuctionStrategyDynamicDataProvider.checkFile.before.csv")
    void testCheckFile() throws Exception {
        // when
        var file = disabledAuctionStrategyGenerator.generate(100500L);

        // then
        try {
            assertThat(file).hasContent(StringTestUtil.getString(
                    getClass(),
                    "DisabledAuctionStrategyDynamicDataProvider.checkFile.after.txt"
            ));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @Test
    void testFileIsEmpty() throws Exception {
        // when
        var file = disabledAuctionStrategyGenerator.generate(100500L);

        // then
        try {
            assertThat(file).hasContent("#100500");
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
