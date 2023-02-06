package ru.yandex.market.asyncreport;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.ReportsServiceSettings;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.supplier.promo.PromoOfferHistoryRemovalService;

class DeleteReportsExecutorTest extends FunctionalTest {

    @Autowired
    private DeleteReportsExecutor deleteReportsExecutor;

    @Autowired
    private ReportsMdsStorage<ReportsType> reportsTypeReportsMdsStorage;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ReportsDao<ReportsType> reportsTypeReportsDao;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PromoOfferHistoryRemovalService promoOfferHistoryRemovalService;

    @Value("${mbi.mds.s3.feedlog.bucket:${mbi.mds.s3.bucket}}")
    private String mdsBucketName;

    @BeforeEach
    void setUp() {
        deleteReportsExecutor = new DeleteReportsExecutor(
                reportsTypeReportsMdsStorage,
                new ReportsService<>(
                        new ReportsServiceSettings.Builder<ReportsType>().setReportsQueueLimit(10).build(),
                        reportsTypeReportsDao,
                        transactionTemplate,
                        () -> "1",
                        Clock.fixed(
                                DateTimes.toInstantAtDefaultTz(2019, 10, 24, 10, 0, 0),
                                ZoneId.systemDefault()
                        ),
                        new DisabledAsyncReportService(jdbcTemplate),
                        environmentService
                ),
                transactionTemplate,
                "xlsx",
                promoOfferHistoryRemovalService
        );
    }

    @Test
    @DbUnitDataSet(before = "DeleteReportsExecutorTest.before.csv", after = "DeleteReportsExecutorTest.after.csv")
    void test() {
        deleteReportsExecutor.doJob(null);

        ResourceLocation[] expectedLocations = {
                ResourceLocation.create(mdsBucketName, "reports/3/fulfillment_orders/33.xlsx"),
                ResourceLocation.create(mdsBucketName, "reports/3/fulfillment_orders/1.xlsx")
        };

        Mockito.verify(mdsS3Client).delete(expectedLocations);
    }

    @Test
    @DbUnitDataSet(before = "DeleteReportsExecutorTest.emptyReports.before.csv",
            after = "DeleteReportsExecutorTest.emptyReports.after.csv")
    void testDeleteEmptyReports() {
        deleteReportsExecutor.doJob(null);
    }
}
