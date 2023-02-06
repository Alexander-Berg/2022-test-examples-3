package ru.yandex.market.reporting.generator.workbook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.OffersCount;
import ru.yandex.market.reporting.generator.domain.OffersCountAverage;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.domain.ShareByPp;
import ru.yandex.market.reporting.generator.domain.forecaster.ClickType;
import ru.yandex.market.reporting.generator.domain.forecaster.FeeCbidForecastRecord;
import ru.yandex.market.reporting.generator.workbook.Rows.ForecasterRow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.market.reporting.generator.service.ForecasterYtReportBuilder.createRowsInOrderOf;
import static ru.yandex.market.reporting.generator.service.ForecasterYtReportBuilder.makeRows;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
public class ForecasterWorkbookRendererTest {

    public static final String WORKBOOK_PASSWORD = "123";

    public static List<String> vendors = ImmutableList.of("Apple", "Motorola", "Yota", "Microsoft", "АвтоВАЗ");

    private ForecasterWorkBookRenderer workbookRenderer = new ForecasterWorkBookRenderer(WORKBOOK_PASSWORD);

    private String getResource(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(name).toURI())));
    }

    private Triple<List<FeeCbidForecastRecord>, List<FeeCbidForecastRecord>, List<FeeCbidForecastRecord>> getForecasts() throws Exception {
        TypeToken<List<FeeCbidForecastRecord>> typeToken = new TypeToken<List<FeeCbidForecastRecord>>(){};
        Gson gson = new Gson();
        return ImmutableTriple.of(
            gson.fromJson(getResource("hybrid_rows.json"), typeToken.getType()),
            gson.fromJson(getResource("united_rows.json"), typeToken.getType()),
            gson.fromJson(getResource("sort_rows.json"), typeToken.getType())
        );
    }

    @Test
    public void buildForecasterReport() throws Exception {
        MarketReportParameters parameters = new MarketReportParameters();
        parameters.setShop("Megafon");
        parameters.setDomain("megafon.ru");
        parameters.setRegions(ImmutableList.of(213L));
        parameters.setCategories(ImmutableList.of(91491L));

        ReportComponents.Forecaster forecaster = new ReportComponents.Forecaster();
        forecaster.setDate(LocalDate.of(2017, 7, 4));
        forecaster.setPeriodLength(7);
        parameters.getComponents().setForecaster(forecaster);

        LocalDate periodTo = parameters.getComponents().getForecaster().getDate();
        LocalDate periodFrom = periodTo.minusDays(parameters.getComponents().getForecaster().getPeriodLength() - 1);

        Map<ClickType, OffersCount> offersCountImmutableMap = ImmutableMap.of(ClickType.CPA, OffersCount.builder().modelsNum(23).offersNum(44).build(),
            ClickType.CPC, OffersCount.builder().modelsNum(33).offersNum(88).build());

        Map<ClickType, OffersCountAverage> offersCountAverageImmutableMap = ImmutableMap.of(ClickType.CPA, OffersCountAverage.offersCountAverage().modelsNum(23).offersNum(44).build(),
            ClickType.CPC, OffersCountAverage.offersCountAverage().modelsNum(33).offersNum(88).build());

        ShareByPp ppShare = ShareByPp
            .builder()
            .cardSort(2.5)
            .hybridAuction(3.5)
            .defOffer(4.5)
            .other(5.5)
            .searchAuction(6.5)
            .searchSort(7.5)
            .build();

        Map<ClickType, ShareByPp> ppShareImmutableMap = ImmutableMap.of(ClickType.CPA, ppShare,
                ClickType.CPC, ShareByPp
                .builder()
                .cardSort(2.2)
                .hybridAuction(3.2)
                .defOffer(4.2)
                .other(5.2)
                .searchAuction(6.2)
                .searchSort(7.2)
                .build());

        Triple<List<FeeCbidForecastRecord>, List<FeeCbidForecastRecord>, List<FeeCbidForecastRecord>> forecastsWithCurrentAndTotal = getForecasts();
        List<ForecasterRow> hybridRows = makeRows(forecastsWithCurrentAndTotal.getLeft());
        List<ForecasterRow> rowsSortByPrice = createRowsInOrderOf(hybridRows, forecastsWithCurrentAndTotal.getRight());
        List<ForecasterRow> rowsUnited = createRowsInOrderOf(hybridRows, forecastsWithCurrentAndTotal.getMiddle());

        List<ForecastAllPpRows> vendorRows = vendors.stream().map(el ->
            ForecastAllPpRows
                .builder()
                .rowsHybrid(makeRows(recordsWithVendorName(forecastsWithCurrentAndTotal.getLeft(), el)))
                .rowsSortByPrice(createRowsInOrderOf(hybridRows, recordsWithVendorName(forecastsWithCurrentAndTotal.getRight(), el)))
                .rowsUnited(createRowsInOrderOf(hybridRows, recordsWithVendorName(forecastsWithCurrentAndTotal.getMiddle(), el)))
                .shopOrdersPpShareMap(ppShareImmutableMap)
                .offersCountMap(offersCountImmutableMap)
                .offersCountAverageMap(offersCountAverageImmutableMap)
                .ordersPpShare(ppShare)
                .build()
        ).collect(Collectors.toList());

        ForecasterReportData reportData = ForecasterReportData.forecastReportDataBuilder()
            .parameters(parameters)
            .clientLocalDomains(asList("msk.domain.ru"))
                .categoriesNames(singletonList("Мобильные телефоны"))
            .regionsNames(singletonList("Москва"))
            .periodFrom(periodFrom)
            .periodTo(periodTo)
            .rows(hybridRows)
            .rowsSortByPrice(rowsSortByPrice)
            .rowsUnited(rowsUnited)
            .shopOrdersPpShareMap(ppShareImmutableMap)
            .offersCountMap(offersCountImmutableMap)
            .offersCountAverageMap(offersCountAverageImmutableMap)
            .ordersPpShare(ppShare)
            .clientLocalDomains(asList(
                "moscow.shop.megafon.ru"))
            .vendorRowGroups(vendorRows)
            .build();

        Path path = Paths.get("forecaster.xlsx");
        workbookRenderer.buildReport(reportData, path);
    }

    private List<FeeCbidForecastRecord> recordsWithVendorName(List<FeeCbidForecastRecord> records, String vendorName) {
        records.stream().forEach(record -> record.setVendorName(vendorName));
        return records;
    }

}
