package ru.yandex.market.rg.asyncreport.offers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.util.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.partner.marketplace.AvailableMarketplaceProgramService;
import ru.yandex.market.core.supplier.model.SupplierOffer;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты для {@link WhiteOffersCatalogGenerator}.
 */
@ParametersAreNonnullByDefault
@DisplayName("Запрос списка белых оферов из кэша индексатора в виде Excel-файла")
public class WhiteOffersCatalogGeneratorTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    @Qualifier("supplierXlsHelper")
    private SupplierXlsHelper supplierXlsHelper;

    @Autowired
    private ReportsMdsStorage<ReportsType> reportsMdsStorage;

    @Autowired
    private MdsS3Client mdsS3Client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${market.reports.offers-mapping.file-extension:xlsx}")
    private String fileExtension;

    @Value("${market.reports.white-offers.offers-table}")
    private String offersTablePath;

    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @Autowired
    private AvailableMarketplaceProgramService availableMarketplaceProgramService;


    @DisplayName("Без данных")
    @Test
    public void testOffersWithEmptyResult() throws IOException {
        YtTemplate ytTemplate = initYtTemplate(invocation -> null);

        WhiteOfferCatalogParams params = new WhiteOfferCatalogParams();
        params.setPartnerId(SUPPLIER_ID);

        WhiteOffersCatalogGenerator whiteOffersCatalogGenerator =
                new WhiteOffersCatalogGenerator(reportsMdsStorage, ytTemplate,
                        offersTablePath, supplierXlsHelper, fileExtension, availableMarketplaceProgramService);
        var result = prepareAndRunGenerator(whiteOffersCatalogGenerator, params);

        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        Assertions.assertEquals("Report is empty", result.getReportGenerationInfo().getDescription());
        Assertions.assertNull(result.getReportGenerationInfo().getUrlToDownload());

        Mockito.verifyZeroInteractions(supplierXlsHelper);
    }

    @DisplayName("Задан некорректный supplier_id")
    @Test
    public void testOffersWithEWrongSupplier() throws IOException {
        YtTemplate ytTemplate = initYtTemplate(invocation -> {
            Consumer<JsonNode> consumer = invocation.getArgument(2);
            consumer.accept(initStubNode());
            return null;
        });

        WhiteOfferCatalogParams params = new WhiteOfferCatalogParams();
        params.setPartnerId(SUPPLIER_ID);

        WhiteOffersCatalogGenerator whiteOffersCatalogGenerator =
                new WhiteOffersCatalogGenerator(reportsMdsStorage, ytTemplate,
                        offersTablePath, supplierXlsHelper, fileExtension, availableMarketplaceProgramService);
        var result = prepareAndRunGenerator(whiteOffersCatalogGenerator, params);
        Assertions.assertEquals("http://path/to", result.getReportGenerationInfo().getUrlToDownload());

        ArgumentCaptor<Collection<SupplierOffer>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(supplierXlsHelper).fillTemplate(any(), captor.capture(), any(), any(), anyBoolean());

        Assert.isEmpty(captor.getValue());
    }

    private YtTemplate initYtTemplate(Answer answer) {
        Yt ytMock = mock(Yt.class);
        YtTables ytTablesMock = mock(YtTables.class);
        doReturn(ytTablesMock)
                .when(ytMock).tables();
        doAnswer(answer).when(ytTablesMock).read(any(), any(), any(Consumer.class));

        return new YtTemplate(new YtCluster[]{
                new YtCluster(".", ytMock)
        });
    }

    private JsonNode initStubNode() throws IOException {
        return objectMapper.readTree(this.getClass().getResource("whiteOfferStub.json"));
    }

    @DisplayName("Проверка чтения данных")
    @Test
    public void testOffersWithData() throws IOException {
        YtTemplate ytTemplate = initYtTemplate(invocation -> {
            Consumer<JsonNode> consumer = invocation.getArgument(2);
            consumer.accept(initStubNode());
            return null;
        });

        WhiteOfferCatalogParams params = new WhiteOfferCatalogParams();
        params.setPartnerId(SUPPLIER_ID);

        WhiteOffersCatalogGenerator whiteOffersCatalogGenerator =
                new WhiteOffersCatalogGenerator(reportsMdsStorage, ytTemplate,
                        offersTablePath, supplierXlsHelper, fileExtension, availableMarketplaceProgramService);
        var result = prepareAndRunGenerator(whiteOffersCatalogGenerator, params);
        Assertions.assertEquals("http://path/to", result.getReportGenerationInfo().getUrlToDownload());

        ArgumentCaptor<Collection<SupplierOffer>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(supplierXlsHelper).fillTemplate(any(), captor.capture(), any(), any(), anyBoolean());

        List<SupplierOffer> offers = (List<SupplierOffer>) captor.getValue();
        SupplierOffer offer = offers.get(0);

        Assertions.assertEquals("1", offer.getVendorCode());
        Assertions.assertEquals("11", offer.getShopSku());
        Assertions.assertEquals("vendor", offer.getVendor());
        Assertions.assertEquals("About", offer.getDescription());
        Assertions.assertEquals("0/0/0", offer.getDimensions());
        Assertions.assertEquals(new BigDecimal("6290"), offer.getPrice());
        Assertions.assertEquals(new BigDecimal("6290.0000001"), offer.getOldPrice());
        Assertions.assertEquals("https://www.bestwatch.ru/watch/Pierre_Lannier/043K900/?utm_source=YaMarket" +
                "&utm_medium=cpc&utm_content=043K900&utm_campaign=Pierre%20Lannier", offer.getUrl());
        Assertions.assertEquals("11009988", offer.getBarCode());
        Assertions.assertNull(offer.getVat());
        Assertions.assertNull(offer.getManufacturer());
        Assertions.assertNull(offer.getCountryOfOrigin());
        Assertions.assertNull(offer.getPeriodOfValidity());
        Assertions.assertNull(offer.getPeriodOfValidityComment());
        Assertions.assertNull(offer.getServiceLife());
        Assertions.assertNull(offer.getServiceLifeComment());
        Assertions.assertNull(offer.getWarranty());
        Assertions.assertNull(offer.getWarrantyComment());
        Assertions.assertEquals(0, offer.getTransportUnit());
        Assertions.assertEquals(1, offer.getMinDeliveryPieces());
        Assertions.assertEquals(0, offer.getQuantum());
        Assertions.assertEquals(0, offer.getLeadtime());
        Assertions.assertNull(offer.getDeliveryWeekdays());
        Assertions.assertEquals(0, offer.getBoxCount());
        Assertions.assertNull(offer.getCustomsCommodityCodes());
    }

    private ReportResult prepareAndRunGenerator(WhiteOffersCatalogGenerator whiteOffersCatalogGenerator,
                                        WhiteOfferCatalogParams params)
            throws MalformedURLException {
        doReturn(new java.net.URL("http://path/to")).
                when(mdsS3Client).getUrl(any());

        return whiteOffersCatalogGenerator.generate("1", params);
    }

    @Test
    @DisplayName("Вытащить параметры из базы и убедиться, что json извлекли корректно")
    @DbUnitDataSet(before = "testWhiteOffersReportParams.csv")
    public void paramsTest() throws IOException {
        ReportInfo<ReportsType> reportInfo = reportsDao.getPendingReportWithLock(
                Collections.singleton(ReportsType.WHITE_OFFERS_CATALOG));
        Assertions.assertNotNull(reportInfo);
        Assertions.assertEquals("1", reportInfo.getId());
        Assertions.assertEquals(774L, reportInfo.getReportRequest().getEntityId());
        Assertions.assertEquals(ReportsType.WHITE_OFFERS_CATALOG, reportInfo.getReportRequest().getReportType());
    }

    @Test
    @DisplayName("Проверка чтения данных импортированных по белому магазину")
    @DbUnitDataSet(
            before = "WhiteOffersCatalogGeneratorTest.testWhiteOffersReportParams.before.csv",
            after = "WhiteOffersCatalogGeneratorTest.testWhiteOffersReportParams.after.csv"
    )
    public void testOffersWithDataWhite() throws IOException {
        YtTemplate ytTemplate = initYtTemplate(invocation -> null);

        WhiteOfferCatalogParams params = new WhiteOfferCatalogParams();
        params.setPartnerId(SUPPLIER_ID);

        WhiteOffersCatalogGenerator whiteOffersCatalogGenerator =
                new WhiteOffersCatalogGenerator(reportsMdsStorage, ytTemplate,
                        offersTablePath, supplierXlsHelper, fileExtension, availableMarketplaceProgramService);
        var result = prepareAndRunGenerator(whiteOffersCatalogGenerator, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        Assertions.assertEquals("Report is empty", result.getReportGenerationInfo().getDescription());
        Assertions.assertNull(result.getReportGenerationInfo().getUrlToDownload());

        Mockito.verifyZeroInteractions(supplierXlsHelper);
    }
}
