package ru.yandex.market.rg.asyncreport.turnover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.rg.asyncreport.turnover.yt.TurnoverReportItemBillingYtModel;
import ru.yandex.market.rg.asyncreport.turnover.yt.TurnoverReportSummaryBillingYtModel;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertEquals;

/**
 * Проверяем работу {@link TurnoverReportService}.
 */
@DbUnitDataSet(before = "TurnoverReportServiceTest.before.csv")
class TurnoverReportServiceOnBillingTest extends FunctionalTest {

    @Autowired
    private TurnoverReportService turnoverReportService;

    @Autowired
    private YtHttpFactory ytHttpFactory;

    private Yt yt;
    private YtTables ytTables;
    private YtTransactions transactions;
    private Cypress cypress;


    @Autowired
    private DataCampService dataCampService;

    @BeforeEach
    void init() {
        yt = Mockito.mock(Yt.class);
        transactions = Mockito.mock(YtTransactions.class);
        ytTables = Mockito.mock(YtTables.class);
        cypress = Mockito.mock(Cypress.class);

        when(ytHttpFactory.getYt(any())).thenReturn(yt);
        when(yt.transactions()).thenReturn(transactions);
        when(transactions.start(any())).thenReturn(GUID.create());
        when(yt.tables()).thenReturn(ytTables);
        when(yt.cypress()).thenReturn(cypress);
        when(cypress.list(any(YPath.class)))
                .thenReturn(List.of(new YTreeStringNodeImpl("2022-05-18", Map.of())));
    }

    @Test
    void emptyReport() throws IOException, InvalidFormatException {

        when(dataCampService.searchBusinessOffers(getRequest(110774L, 10774L, 0)))
                .thenReturn(SearchBusinessOffersResult.builder().setTotalCount(0).build());

        checkReport(List.of(), List.of(), "TurnoverReportServiceTestDraft.empty.xlsx");
    }

    private SearchBusinessOffersRequest getRequest(long businessId, long partnerId, int n) {
        Set<String> offerIds = IntStream.range(0, n).mapToObj(i -> "offer_" + i).collect(Collectors.toSet());
        return SearchBusinessOffersRequest.builder()
                .setPageRequest(SeekSliceRequest.firstN(1000))
                .setWithRetry(true)
                .setBusinessId(businessId)
                .setPartnerId(partnerId)
                .addOfferIds(offerIds)
                .build();
    }

    private String getFileContent(String filename) {
        return StringTestUtil.getString(this.getClass().getResourceAsStream(filename));
    }

    @Test
    void sameCategory() throws IOException, InvalidFormatException {
        SyncGetOffer.GetUnitedOffersResponse.Builder builder = SyncGetOffer.GetUnitedOffersResponse.newBuilder();
        JsonFormat.merge(getFileContent("proto/datacampOffers250.json"), builder);
        when(dataCampService.searchBusinessOffers(getRequest(110774L, 10774L, 250)))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(builder.build()));

        var models = new ArrayList<TurnoverReportItemBillingYtModel>();
        for (int i = 0; i < 250; ++i) {
            TurnoverReportItemBillingYtModel model = new TurnoverReportItemBillingYtModel();
            model.setPartnerId(10744L);
            model.setCategoryId(100L);
            model.setOfferId("offer_" + i);
//            model.setMsku(200L + i);
            model.setCategoryName("Category 100");
//            model.setOfferTitle("Offer title " + i);
            model.setLength(30 + i);
            model.setWidth(31 + i);
            model.setHeight(32 + i);
            model.setVolume(33 + i);
            model.setTurnover(i == 0 ? "Infinity" : String.valueOf(i));
            model.setAverageSoldVolume(110 + i);
            model.setAverageSoldItems(111 + i);
            model.setAverageSoldVolumeOnStock(112 + i);
            model.setItemsOnStock(113 + i);
            models.add(model);
        }

        var summary = new ArrayList<TurnoverReportSummaryBillingYtModel>();
        TurnoverReportSummaryBillingYtModel ytSummary = new TurnoverReportSummaryBillingYtModel();
        ytSummary.setPartnerId(10744L);
        ytSummary.setCategoryId(100L);
        ytSummary.setCategoryName("Category 100");
        ytSummary.setCategoryTurnover("99.");
        ytSummary.setTariff(0.45);
        ytSummary.setAverageVolumeOnStock(12.);
        ytSummary.setAverageSoldVolume(14.);
        ytSummary.setAmount(1300.4);
        ytSummary.setDaysInReportingPeriod(21);
        summary.add(ytSummary);

        checkReport(models, summary, "TurnoverReportServiceTestDraft.sameCategory.xlsx");
    }

    @Test
    void differentCategories() throws IOException, InvalidFormatException {
        SyncGetOffer.GetUnitedOffersResponse.Builder builder = SyncGetOffer.GetUnitedOffersResponse.newBuilder();
        JsonFormat.merge(getFileContent("proto/datacampOffers250.json"), builder);
        when(dataCampService.searchBusinessOffers(getRequest(110774L, 10774L, 150)))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(builder.build()));

        var models = new ArrayList<TurnoverReportItemBillingYtModel>();
        for (int i = 0; i < 150; ++i) {
            TurnoverReportItemBillingYtModel model = new TurnoverReportItemBillingYtModel();
            model.setPartnerId(10744L);
            model.setCategoryId(100L + (i / 110));
            model.setOfferId("offer_" + i);
//            model.setMsku(200L + i);
            model.setCategoryName("Category 10" + (i / 110));
//            model.setOfferTitle("Offer title " + i);
            model.setLength(30 + i);
            model.setWidth(31 + i);
            model.setHeight(32 + i);
            model.setVolume(33 + i);
            model.setTurnover(i == 0 ? "Infinity" : String.valueOf(i));
            model.setAverageSoldVolume(110 + i);
            model.setAverageSoldItems(111 + i);
            model.setAverageSoldVolumeOnStock(112 + i);
            model.setItemsOnStock(113 + i);
            models.add(model);
        }

        var summary = new ArrayList<TurnoverReportSummaryBillingYtModel>();
        for (int i = 0; i <= 1; ++i) {
            TurnoverReportSummaryBillingYtModel ytSummary = new TurnoverReportSummaryBillingYtModel();
            ytSummary.setPartnerId(10744L);
            ytSummary.setCategoryId(100L + i);
            ytSummary.setCategoryName("Category 10" + i);
            ytSummary.setCategoryTurnover(i == 0 ? "Infinity" : String.valueOf(99. + i));
            ytSummary.setTariff(0.45 + i / 2.);
            ytSummary.setAverageVolumeOnStock(12. + i);
            ytSummary.setAverageSoldVolume(14. + i);
            ytSummary.setAmount(1300.4 + i);
            ytSummary.setDaysInReportingPeriod(21 + i);
            summary.add(ytSummary);
        }

        checkReport(models, summary, "TurnoverReportServiceTestDraft.differentCategories.xlsx");
    }

    void checkReport(List<TurnoverReportItemBillingYtModel> models,
                     List<TurnoverReportSummaryBillingYtModel> summary,
                     String expectedPath) throws IOException, InvalidFormatException {
        mockYt(models, summary);

        XSSFWorkbook expected = new XSSFWorkbook(Objects.requireNonNull(
                getClass().getResourceAsStream(expectedPath)
        ));
        Path tempFilePath = Files.createTempFile("WorkbookForTurnoverReportTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        LocalDate dt = LocalDate.of(2022, 5, 18);
        try (OutputStream out = new FileOutputStream(reportFile)) {
            turnoverReportService.generateWorkbook(10774L, dt, out);
        }

//        // Сохранить файлик с результатом к себе в хомку. Мб полезно в случае поломок тестов
//        Files.copy(reportFile.toPath(), Path.of("/Users/nastik/" + expectedPath), StandardCopyOption
//        .REPLACE_EXISTING);

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    private void mockYt(List<TurnoverReportItemBillingYtModel> models,
                        List<TurnoverReportSummaryBillingYtModel> summary) {
        when(ytTables.read(any(), any()))
                .thenAnswer(invocation -> {
                    YPath path = invocation.getArgument(0);
                    if (path.toString().contains("agg_partner_report_by_ssku")) {
                        return CloseableIterator.wrap(models.iterator());
                    } else {
                        return CloseableIterator.wrap(summary.iterator());
                    }
                });
    }
}
