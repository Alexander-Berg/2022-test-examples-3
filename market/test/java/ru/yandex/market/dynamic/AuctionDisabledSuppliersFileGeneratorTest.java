package ru.yandex.market.dynamic;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see AuctionDisabledSuppliersDataProvider
 */
class AuctionDisabledSuppliersFileGeneratorTest extends FunctionalTest {
    @Autowired
    private FileGenerator auctionDisabledSuppliersFileGenerator;

    @Test
    @DbUnitDataSet(before = "AuctionDisabledSuppliersFileGeneratorTest.shouldCreateDynamicFile.before.csv")
    void shouldCreateDynamicFile() throws Exception {
        // when
        var file = auctionDisabledSuppliersFileGenerator.generate(200100305L);

        // then
        try {
            assertThat(file).hasContent(StringTestUtil.getString(
                    getClass(),
                    "AuctionDisabledSuppliersFileGeneratorTest.shouldCreateDynamicFile.after.txt"
            ));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
