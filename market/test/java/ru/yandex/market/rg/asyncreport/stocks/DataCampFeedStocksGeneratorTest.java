package ru.yandex.market.rg.asyncreport.stocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.common.framework.core.MultipartRemoteFile;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.datacamp.DataCampClientStub;
import ru.yandex.market.core.datacamp.DataCampFeedPartnerService;
import ru.yandex.market.core.datacamp.DefaultDataCampService;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.db.SupplierSummaryDao;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.supplier.model.SupplierOffer;
import ru.yandex.market.core.supplier.service.PartnerFulfillmentLinkService;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.mbi.datacamp.combine.DataCampCombinedClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertCellValues;

/**
 * Функциональные тесты для {@link ActualFeedStocksReportGenerator},
 * который используется в {@link DatacampFeedStocksGenerator}.
 */
@ParametersAreNonnullByDefault
public class DataCampFeedStocksGeneratorTest extends FunctionalTest {

    private static final int PARTNER_ID = 199;
    private static final String UPLOAD_URL =
            "https://market-mbi-test.s3.mdst.yandex.net/upload-feed/199/upload-feed-111111";
    private static final String DATACAMP_WITH_OFFERS = "datacamp.feedstocks.test.json";
    private static final String DATACAMP_WITHOUT_OFFERS = "empty.test.json";
    @Autowired
    PartnerFulfillmentLinkService partnerFulfillmentLinkService;
    @Autowired
    @Qualifier("dataCampStocksSupplierXlsHelper")
    private SupplierXlsHelper dataCampStocksSupplierXlsHelper;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private FeedFileStorage feedFileStorage;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private DataCampFeedPartnerService dataCampFeedPartnerService;
    @Autowired
    private RetryTemplate retryTemplate;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private DataCampCombinedClient dataCampCommonCombinedClient;
    private ActualFeedStocksReportGenerator generator;
    private DataCampClient dataCampClientStub;

    @Autowired
    private SupplierSummaryDao supplierSummaryDao;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private UnitedCatalogEnvironmentService unitedCatalogEnvironmentService;

    void init(String fileName) throws IOException {
        dataCampClientStub = new DataCampClientStub(fileName);

        DefaultDataCampService defaultDataCampService =
                new DefaultDataCampService(
                        dataCampClientStub,
                        dataCampCommonCombinedClient,
                        unitedCatalogEnvironmentService,
                        campaignService,
                        dataCampFeedPartnerService,
                        businessService,
                        retryTemplate,
                        supplierSummaryDao,
                        eventPublisher,
                        protocolService,
                        () -> false);
        when(feedFileStorage.upload(any(), anyLong())).thenReturn(new StoreInfo(55, UPLOAD_URL));
        generator = new ActualFeedStocksReportGenerator(defaultDataCampService, dataCampStocksSupplierXlsHelper,
                fileUploadService, partnerFulfillmentLinkService);
    }

    @Test
    @DbUnitDataSet(before = "DataCampFeedStocksGeneratorTest.before.csv",
            after = "DataCampFeedStocksGeneratorTest.after.csv")
    @DisplayName("Корректная выгрузка отчета")
    void testCorrectReportGeneration() throws IOException {
        init(DATACAMP_WITH_OFFERS);
        ArgumentCaptor<MultipartRemoteFile> fileCaptor = ArgumentCaptor.forClass(MultipartRemoteFile.class);
        generator.generateReport(PARTNER_ID);
        verify(feedFileStorage).upload(fileCaptor.capture(), anyLong());
        Workbook workbook = WorkbookFactory.create(fileCaptor.getValue().getInputStream());
        List<List<Object>> expectedList = createExpectedList();
        Sheet sheet = workbook.getSheetAt(1);
        assertCellValues(expectedList, sheet, 2, 1);
    }

    @Test
    @DbUnitDataSet(before = "DataCampFeedStocksGeneratorTest.before.csv",
            after = "DataCampFeedStocksGeneratorTest.after.csv")
    @DisplayName("У поставщика нет офферов в хранилище")
    void testEmptyReportGeneration() throws IOException {
        init(DATACAMP_WITHOUT_OFFERS);
        ArgumentCaptor<Collection<SupplierOffer>> supplierOffersCaptor = ArgumentCaptor.forClass(Collection.class);
        generator.generateReport(PARTNER_ID);
        verify(dataCampStocksSupplierXlsHelper).fillTemplate(
                any(), supplierOffersCaptor.capture(), any(), any(),
                Mockito.anyBoolean());
        Collection<SupplierOffer> supplierOffers = supplierOffersCaptor.getValue();
        assertTrue(supplierOffers.isEmpty());
        verify(feedFileStorage).upload(any(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "DataCampFeedStocksGeneratorTest.nullFeedId.before.csv")
    @DisplayName("У поставщика нет feed_id на кроссдочном складе")
    void testNullFeedId() throws IOException {
        init(DATACAMP_WITH_OFFERS);
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> generator.generateReport(PARTNER_ID));
        assertEquals("Supplier doesn't have active feed for crossdock warehouse", ex.getMessage());

    }

    @Test
    @DbUnitDataSet(before = "DataCampFeedStocksGeneratorTest.noCrossdockWH.before.csv")
    @DisplayName("У поставщика не привязан склад типа кроссдок")
    void testNoCrossdockWh() throws IOException {
        init(DATACAMP_WITH_OFFERS);
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> generator.generateReport(PARTNER_ID));
        assertEquals("Supplier should have the one crossdock-type ff-service-link", ex.getMessage());

    }

    private List<List<Object>> createExpectedList() {
        return Arrays.asList(
                Arrays.asList("100", "SKU100", 10),
                Arrays.asList("101", "SKU101", 0),
                Arrays.asList("102", "SKU102", 50)
        );
    }
}
