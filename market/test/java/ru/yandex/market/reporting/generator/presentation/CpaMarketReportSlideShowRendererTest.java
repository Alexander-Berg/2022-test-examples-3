package ru.yandex.market.reporting.generator.presentation;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.generator.config.SlideShowRendererConfig;
import ru.yandex.market.reporting.generator.domain.DatePeriod;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.presentation.MarketCpaReportData.CpaRegionCategoryGroup;
import ru.yandex.market.reporting.generator.presentation.MarketReportData.RegionGroup;
import ru.yandex.market.reporting.generator.service.Formatters;
import ru.yandex.market.reporting.generator.service.domain.Series;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SlideShowRendererConfig.class)
public class CpaMarketReportSlideShowRendererTest {

    @Inject
    private MarketReportSlideShowRenderer<MarketCpaReportData.CpaRegionCategoryGroup> cpaSlideShowRenderer;

    @Test
    public void makePresentation() throws Exception {
        MarketCpaReportData reportData = reportData();
        Path path = Paths.get("cpa_report.pptx");
        cpaSlideShowRenderer.buildReport(reportData, path);
    }

    @Test
    public void englishPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "en");
        MarketCpaReportData reportData = reportData();
        Path path = Paths.get("cpa_report-en.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpaSlideShowRenderer);
    }

    @Test
    public void russianPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "ru");
        MarketCpaReportData reportData = reportData();
        Path path = Paths.get("cpa_report-ru.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpaSlideShowRenderer);
    }

    private MarketCpaReportData reportData() {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpaRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpaRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpaSlide1(MarketCpaReportData.CpaSlide1.builder()
                        .ordersCount(1234L)
                        .ordersShare(15.0)
                        .cancelledOrdersCategoryShare(15.0)
                        .completedCancelledDiagram(new Series<>(ImmutableList.of(
                            Pair.of("Доставлены", asList(27L)),
                            Pair.of("Отменены при доставке", asList(73L)))))
                        .ordersShareDiagram(new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(5L)),
                            Pair.of("shop2", asList(5L)),
                            Pair.of("shop3", asList(5L)),
                            Pair.of("shop4", asList(5L)),
                            Pair.of("shop5", asList(5L)),
                            Pair.of("shop6", asList(5L)),
                            Pair.of("shop7", asList(5L)),
                            Pair.of("shop8", asList(5L)),
                            Pair.of("shop9", asList(5L)),
                            Pair.of("mts", asList(15L)),
                            Pair.of("Остальные", asList(20L)))))
                        .ordersShareDiagramShopPosition(9)
                        .ordersShareDynamicDiagram(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
                        .categoryDynamicDiagram(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
                        .shopRating(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(4.5)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(4.3)))))
                        .build())
                    .build()
                )));
        groups.add(
            new RegionGroup<>(
                2,
                regions.get(0),
                ImmutableList.of(CpaRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpaSlide1(MarketCpaReportData.CpaSlide1.builder()
                        .ordersCount(1234L)
                        .ordersShare(15.0)
                        .completedCancelledDiagram(new Series<>(ImmutableList.of(
                            Pair.of("mobile", asList(0L)),
                            Pair.of("desktop", asList(0L)))))
                        .ordersShareDiagram(new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(5L)),
                            Pair.of("shop2", asList(5L)),
                            Pair.of("shop3", asList(5L)),
                            Pair.of("shop4", asList(5L)),
                            Pair.of("shop5", asList(5L)),
                            Pair.of("shop6", asList(5L)),
                            Pair.of("shop7", asList(5L)),
                            Pair.of("shop8", asList(5L)),
                            Pair.of("shop9", asList(5L)),
                            Pair.of("mts", asList(15L)),
                            Pair.of("Остальные", asList(20L)))))
                        .ordersShareDiagramShopPosition(9)
                        .ordersShareDynamicDiagram(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
                        .categoryDynamicDiagram(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
                        .shopRating(new Series<>(ImmutableList.of(
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(4.5)),
                            Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(4.3)))))
                        .build())
                        .cpaSlide2(MarketCpaReportData.CpaSlide2.builder()
                                .competitionMapLeftDiagram(new Series[]{
                                        new Series<>(ImmutableList.of(
                                                Pair.of("shop1", asList(0.6, 5000.0, 2.0)),
                                                Pair.of("shop2", asList(0.7, 3000.8, 5.0)))),
                                        new Series<>(ImmutableList.of(
                                                Pair.of("mts.ru", asList(0.6, 1000.0, 2.0)))) })
                                .competitionMapRightDiagram(new Series[]{
                                        new Series<>(ImmutableList.of(
                                                Pair.of("shop1", asList(0.6, 5000.0, 2.0)),
                                                Pair.of("shop2", asList(0.7, 3000.8, 5.0)))),
                                        new Series<>(ImmutableList.of(
                                                Pair.of("mts.ru", asList(0.6, 1000.0, 2.0)))) })
                                .build())
                    .build()
                )));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));
        params.getComponents().setCpaSlide1(cpaSlide1(period));
        params.getComponents().setCpaSlide2(cpaSlide2(period));

        return MarketCpaReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();
    }

    private ReportComponents.CpaSlide2 cpaSlide2(DatePeriod period) {
        ReportComponents.CpaSlide2 cpaSlide2 = new ReportComponents.CpaSlide2();
        cpaSlide2.setCompetitionMapDiagramPeriod(period);
        return cpaSlide2;
    }

    private ReportComponents.CpaSlide1 cpaSlide1(final DatePeriod period) {
        ReportComponents.CpaSlide1 cpaSlide1 = new ReportComponents.CpaSlide1();
        cpaSlide1.setCategoryDynamicDiagramPeriod(period);
        cpaSlide1.setCompletedCancelledDiagramPeriod(period);
        cpaSlide1.setOrdersShareDiagramPeriod(period);
        cpaSlide1.setOrdersShareDynamicDiagramPeriod(period);
        return cpaSlide1;
    }

}
