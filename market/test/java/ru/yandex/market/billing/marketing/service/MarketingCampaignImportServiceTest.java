package ru.yandex.market.billing.marketing.service;

import java.io.InputStream;
import java.time.LocalDate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.marketing.dao.MarketingCampaignDao;
import ru.yandex.market.billing.marketing.dao.MarketingCampaignYtDao;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.billing.marketing.PartnerMarketingTestUtil.mockYt;

@ParametersAreNonnullByDefault
class MarketingCampaignImportServiceTest extends FunctionalTest {

    @Autowired
    MarketingCampaignDao marketingCampaignDao;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate;

    private String marketingCampaignTableYtPath = "//unit-tests/path/to/marketing_campaigns_billing";

    MarketingCampaignImportService marketingCampaignImportService;

    private void setUp(@Nullable String hahnDataSourceFile, @Nullable String arnoldDataSourceFile) {
        var hahnYt = mockYt(getDataSource(hahnDataSourceFile));
        var arnoldYt = mockYt(getDataSource(arnoldDataSourceFile));
        var ytTemplate = new YtTemplate(new YtCluster[]{
                new YtCluster("hahn", hahnYt),
                new YtCluster("arnold", arnoldYt)
        });

        MarketingCampaignYtDao marketingCampaignYtDao =
                new MarketingCampaignYtDao(marketingCampaignTableYtPath, ytTemplate);

        marketingCampaignImportService = new MarketingCampaignImportService(
                marketingCampaignYtDao, marketingCampaignDao,
                transactionTemplate, environmentService
        );
    }

    private InputStream getDataSource(String hahnDataSourceFile) {
        if (hahnDataSourceFile == null) {
            return null;
        }

        var stream = getClass().getResourceAsStream(hahnDataSourceFile);
        if (stream == null) {
            throw new IllegalArgumentException("Can't find file " + hahnDataSourceFile + " in resources");
        }
        return stream;
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.2021-06-20.after.csv"
    )
    void importSingleMarketingCampaign() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2021-06-20"));
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.2021-06-19.after.csv"
    )
    void importMultipleMarketingCampaigns() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2021-06-19"));
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.before.csv"
    )
    void importZeroMarketingCampaigns() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2021-06-18"));
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.2021-06-17.after.csv"
    )
    void reimportMarketingCampaigns() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2021-06-17"));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketingCampaignImportServiceTest.before.csv",
                    "MarketingCampaignImportServiceTest.importIgnoreList.csv"
            },
            after = "MarketingCampaignImportServiceTest.2021-06-16.after.csv"
    )
    void importMarketingCampaignsWithIgnored() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2021-06-16"));
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.before.csv"
    )
    void importMarketingCampaignsFailed() {
        setUp("MarketingCampaignImportServiceTest.yt.json", null);

        var importDate = LocalDate.parse("2021-06-21");
        var exception = assertThrows(
                IllegalStateException.class,
                () -> marketingCampaignImportService.process(importDate)
        );
        assertThat(exception).hasMessage(
                "Table for date '2021-06-21' does not exist"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest.before.csv",
            after = "MarketingCampaignImportServiceTest.2021-06-20.after.csv"
    )
    void importSingleMarketingCampaign_whenHahnIsBroken() {
        setUp(null, "MarketingCampaignImportServiceTest.yt.json");
        marketingCampaignImportService.process(LocalDate.parse("2021-06-20"));
    }

    @Test
    @DisplayName("Обновление маркетинговых кампаний")
    @DbUnitDataSet(
            before = "MarketingCampaignImportServiceTest_updateCampaign.before.csv",
            after = "MarketingCampaignImportServiceTest_updateCampaign.after.csv"
    )
    void importUpdatedMarketingCampaign() {
        setUp("MarketingCampaignImportServiceTest_updateCampaign.yt.json", null);
        marketingCampaignImportService.process(LocalDate.parse("2022-01-17"));
    }

}
