package ru.yandex.market.core.fulfillment.report;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.report.excel.StatisticsReportGenerator;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.supplier.model.PartnerExternalContract;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class StatisticsReportServiceTest extends FunctionalTest {
    private static final String BUCKET_NAME = "bucketName";
    private static final YearMonth REPORT_GENERATION_DATE = YearMonth.of(2017, 5);

    @Autowired
    private OrderReportYtDao orderReportYtDao;
    @Autowired
    private PrepayRequestDao prepayRequestDao;
    @Autowired
    private StatisticsReportGenerator statisticsReportGenerator;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private StatisticReportQueueService monthlyStatisticReportQueueService;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private EnvironmentService environmentService;

    private MdsS3Client mdsS3Client;
    private ResourceLocationFactory resourceLocationFactory;
    private StatisticsReportService statisticsReportService;

    private List<ResourceLocation> uploadedResourceLocations = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        PartnerContractService supplierContractService = mock(PartnerContractService.class);
        doReturn(PartnerExternalContract.builder().generalContractId("id1").subsidiesContractId("id2").build())
                .when(supplierContractService).getExternalContractIds(anyLong());
        statisticsReportGenerator = spy(new StatisticsReportGenerator(
                orderReportYtDao,
                supplierContractService,
                prepayRequestDao
        ));

        resourceLocationFactory = ResourceLocationFactory.create(BUCKET_NAME);
        mdsS3Client = mock(MdsS3Client.class);

        doAnswer(invocation -> {
            uploadedResourceLocations.add(invocation.getArgument(0));
            return null;
        }).when(mdsS3Client).upload(any(ResourceLocation.class), any(ContentProvider.class));

        final URL url;
        try {
            url = new URL("http://example.com/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        doReturn(url).when(mdsS3Client).getUrl(any(ResourceLocation.class));

        statisticsReportService = new StatisticsReportService(
                mdsS3Client,
                resourceLocationFactory,
                statisticsReportGenerator,
                supplierService,
                partnerTypeAwareService,
                monthlyStatisticReportQueueService,
                environmentService
        );
    }

    @Test
    @DisplayName("Тест на фильтрацию поставщиков беру и c&c")
    @DbUnitDataSet(before = "StatisticsReportServiceTest.before.csv")
    void testFilterSuppliers() {
        assertThat(statisticsReportService.getRequiredSupplierIds())
                .containsExactlyInAnyOrder(12345L, 12346L, 12347L, 12349L, 12348L, 12350L);
    }

//    @Test
    @DisplayName("Тест на кастомный список поставщиков при генерации отчета")
    @DbUnitDataSet(before = {"StatisticsReportServiceTest.before.csv",
            "StatisticsReportServiceTest_supplierIds.before.csv"})
    void testSuppliersForReportCustomList() {
        assertThat(statisticsReportService.getRequiredSupplierIds())
                .containsExactlyInAnyOrder(12345L, 12346L, 12347L, 12349L, 100500L);
    }
}
