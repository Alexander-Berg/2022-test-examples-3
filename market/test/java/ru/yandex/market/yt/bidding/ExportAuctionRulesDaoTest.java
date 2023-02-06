package ru.yandex.market.yt.bidding;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.yt.bidding.AuctionGenerationInfoMatcher.hasGenerationId;
import static ru.yandex.market.yt.bidding.AuctionGenerationInfoMatcher.hasGenerationName;
import static ru.yandex.market.yt.bidding.AuctionGenerationInfoMatcher.hasPublishDateTime;
import static ru.yandex.market.yt.bidding.AuctionGenerationInfoMatcher.hasYtCluster;

/**
 * Тест для {@link ExportAuctionRulesDao}
 */
public class ExportAuctionRulesDaoTest extends FunctionalTest {

    @Autowired
    private ExportAuctionRulesDao exportAuctionRulesDao;

    @Test
    @DbUnitDataSet(
            before = "ExportAuctionRulesDaoTest.getInfos.before.csv"
    )
    void testGetGenerationInfos() {
        List<AuctionGenerationInfo> notImportedRules = exportAuctionRulesDao.getNotImportedRules(1);
        assertThat(notImportedRules, contains(
                allOf(
                        hasGenerationId(3),
                        hasGenerationName("20200622_0110"),
                        hasPublishDateTime(LocalDateTime.of(2020, 6, 22, 1, 10, 0)),
                        hasYtCluster("arnold.yt.yandex.net")
                )
        ));

    }
}
