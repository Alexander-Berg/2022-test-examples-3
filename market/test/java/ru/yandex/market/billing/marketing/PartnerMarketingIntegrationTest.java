package ru.yandex.market.billing.marketing;

import java.time.Instant;
import java.time.ZoneOffset;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.environment.EnvironmentAwareDatesProcessingService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;

import static ru.yandex.market.billing.marketing.PartnerMarketingTestUtil.mockYt;

@ParametersAreNonnullByDefault
public class PartnerMarketingIntegrationTest extends FunctionalTest {

    @Autowired
    MarketingCampaignDao marketingCampaignDao;

    @Autowired
    MarketingCampaignDao pgMarketingCampaignDao;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    TransactionTemplate billingPgTransactionTemplate;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    EnvironmentAwareDatesProcessingService datesProcessingService;

    @Autowired
    TestableClock clock;

    MarketingCampaignImportExecutor marketingCampaignImportExecutor;

    @Autowired
    PartnerMarketingFixedBillingExecutor partnerMarketingFixedBillingExecutor;

    @Autowired
    PromoOrderItemImportExecutor promoOrderItemImportExecutor;

    @Autowired
    PartnerMarketingCompensationBillingExecutor partnerMarketingCompensationBillingExecutor;

    @Mock
    JobExecutionContext jobExecutionContext;

    private void setUp(String dataSourceFile) {
        var hahnYt = mockYt(getClass().getResourceAsStream(dataSourceFile));
        var arnoldYt = mockYt(getClass().getResourceAsStream(dataSourceFile));
        var ytTemplate = new YtTemplate(new YtCluster[]{
                new YtCluster("hahn", hahnYt),
                new YtCluster("arnold", arnoldYt)
        });

        MarketingCampaignYtDao marketingCampaignYtDao =
                new MarketingCampaignYtDao("//marketing_campaigns_billing", ytTemplate);

        MarketingCampaignImportService marketingCampaignImportService = new MarketingCampaignImportService(
                marketingCampaignYtDao, marketingCampaignDao, pgMarketingCampaignDao,
                transactionTemplate, billingPgTransactionTemplate, environmentService
        );

        marketingCampaignImportExecutor =
                new MarketingCampaignImportExecutor(marketingCampaignImportService, datesProcessingService);

        clock.setFixed(Instant.parse("2022-04-11T05:06:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingIntegrationTest.before.csv",
            after = "PartnerMarketingIntegrationTest.after.csv"
    )
    void test() {
        setUp("PartnerMarketingIntegrationTest.yt.json");
        marketingCampaignImportExecutor.doJob(jobExecutionContext);
        partnerMarketingFixedBillingExecutor.doJob(jobExecutionContext);
        promoOrderItemImportExecutor.doJob(jobExecutionContext);
        partnerMarketingCompensationBillingExecutor.doJob(jobExecutionContext);

        // сбор в тлог не проверяем, потому что он работает в m-b-tms
        // можно будет добавить, когда все остальное переедет туда
    }
}
