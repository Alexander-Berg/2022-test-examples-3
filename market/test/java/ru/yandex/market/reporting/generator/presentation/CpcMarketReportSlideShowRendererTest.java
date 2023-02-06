package ru.yandex.market.reporting.generator.presentation;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.generator.config.SlideShowRendererConfig;
import ru.yandex.market.reporting.generator.domain.CompetitorsMapKind;
import ru.yandex.market.reporting.generator.domain.DatePeriod;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.presentation.MarketCpcReportData.CpcRegionCategoryGroup;
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
public class CpcMarketReportSlideShowRendererTest {

    @Inject
    private MarketReportSlideShowRenderer<CpcRegionCategoryGroup> cpcSlideShowRenderer;

    @Test
    public void buildCpcReport2Slides() throws Exception {
        MarketCpcReportData reportData = reportData();

        Path path = Paths.get("cpc_report.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    private MarketCpcReportData reportData() {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_1())
                    .cpcSlide2(getCpcSlide2_2())
                    .cpcSlide2complementary(getCpcSlide2_1())
                    .build())));
/*
        groups.add(
            new RegionGroup<>(
                2,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_2())
                    .cpcSlide2(getCpcSlide2_2())
                    .cpcSlide2complementary(getCpcSlide2_1())
                    .build())));
*/

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide1(cpcSlide1(period));
        params.getComponents().setCpcSlide2(cpcSlide2(period));

        return MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();
    }

    private ReportComponents.CpcSlide2 cpcSlide2(DatePeriod period) {
        ReportComponents.CpcSlide2 cpcSlide2 = new ReportComponents.CpcSlide2();
        cpcSlide2.setCompetitionMapDiagramPeriod(period);
        cpcSlide2.setMapKind(CompetitorsMapKind.CATEGORY);
        return cpcSlide2;
    }

    private ReportComponents.CpcSlide2 cpcSlide2Assortment(DatePeriod period) {
        ReportComponents.CpcSlide2 cpcSlide2 = new ReportComponents.CpcSlide2();
        cpcSlide2.setCompetitionMapDiagramPeriod(period);
        cpcSlide2.setMapKind(CompetitorsMapKind.ASSORTMENT);
        return cpcSlide2;
    }

    private ReportComponents.CpcSlide1 cpcSlide1(DatePeriod period) {
        ReportComponents.CpcSlide1 cpcSlide1 = new ReportComponents.CpcSlide1();
        cpcSlide1.setCategoryDynamicDiagramPeriod(period);
        cpcSlide1.setClicksShareDiagramPeriod(period);
        cpcSlide1.setClicksShareDynamicDiagramPeriod(period);
        return cpcSlide1;
    }

    @Test
    public void englishPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "en");
        MarketCpcReportData reportData = reportData();
        Path path = Paths.get("cpc_report-en.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpcSlideShowRenderer);
    }

    @Test
    public void russianPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "ru");
        MarketCpcReportData reportData = reportData();
        Path path = Paths.get("cpc_report-ru.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpcSlideShowRenderer);
    }

    @Test
    public void buildCpcReport1stSlide() throws Exception {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_1())
                    .build())));
        groups.add(
            new RegionGroup<>(
                2,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_2())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide1(cpcSlide1(period));

        MarketCpcReportData reportData = MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("cpc_report_1st_slide.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    @Test
    public void buildCpcReport2ndSlide() throws Exception {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide2(getCpcSlide2_1())
                    .cpcSlide2complementary(getCpcSlide2_2())
                    .build())));
        groups.add(
            new RegionGroup<>(
                2,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide2(getCpcSlide2_2())
                    .cpcSlide2complementary(getCpcSlide2_1())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide2(cpcSlide2(period));

        MarketCpcReportData reportData = MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("cpc_report_2nd_slide.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    @Test
    public void buildCpcCompetitorsMapByAssortmentSlide() throws Exception {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide2(getCpcSlide2_1())
                    .cpcSlide2complementary(getCpcSlide2_2())
                    .build())));
        groups.add(
            new RegionGroup<>(
                2,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide2(getCpcSlide2_2())
                    .cpcSlide2complementary(getCpcSlide2_1())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide2(cpcSlide2Assortment(period));

        MarketCpcReportData reportData = MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("cpc_report_2nd_slide_assortment.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    @Test
    public void buildCpcReportTopCatsSlide() throws Exception {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_1())
                    .topCategoriesL3(getTopCats())
                    .topCategoriesL2(getTopCats())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide1(cpcSlide1(period));

        MarketCpcReportData reportData = MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("cpc_report_top_cats3_slide.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    @Test
    public void buildCpcReportClientCategoryDynamics() throws Exception {
        List<String> regions = asList("Москва и Московская область");
        List<String> categories = asList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .cpcSlide1(getCpcSlide1_1())
                    .clientCategoryDynamics(getClientCategoryDynamics())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 6), YearMonth.of(2016, 7));

        params.getComponents().setCpcSlide1(cpcSlide1(period));

        MarketCpcReportData reportData = MarketCpcReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("cpc_report_client_cat_dyn_slide.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    public MarketCpcReportData.CpcSlide2 getCpcSlide2_1() {
        return MarketCpcReportData.CpcSlide2.builder()
            .competitionMapLeftDiagram(new Series[]{
                    new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(0.6, 5000.0, 200.0)),
                            Pair.of("shop2", asList(0.7, 3000.8, 50.0)))),
                    new Series<>(ImmutableList.of(
                            Pair.of("mts.ru", asList(0.6, 1000.0, 200.0)))) })
            .competitionMapRightDiagram(new Series[]{
                    new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(0.3, 5000.0, 200.0)),
                            Pair.of("shop2", asList(0.4, 3000.8, 500.0)))),
                    new Series<>(ImmutableList.of(
                            Pair.of("mts.ru", asList(0.3, 1000.0, 20.0)))) })
            .build();
    }

    public MarketCpcReportData.CpcSlide2 getCpcSlide2_2() {
        return MarketCpcReportData.CpcSlide2.builder()
            .competitionMapLeftDiagram(new Series[]{
                    new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(0.6, 4000.0, 200.0)),
                            Pair.of("shop2", asList(0.77, 2000.8, 500.0)))),
                    new Series<>(ImmutableList.of(
                            Pair.of("mts.ru", asList(0.6, 2000.0, 200.0)))) })
            .competitionMapRightDiagram(new Series[]{
                    new Series<>(ImmutableList.of(
                            Pair.of("shop1", asList(0.5, 6000.0, 200.0)),
                            Pair.of("shop2", asList(0.6, 4000.8, 500.0)))),
                    new Series<>(ImmutableList.of(
                            Pair.of("mts.ru", asList(0.9, 9000.0, 200.0)))) })
            .build();
    }

    public MarketCpcReportData.CpcSlide1 getCpcSlide1_1() {
        return MarketCpcReportData.CpcSlide1.builder()
            .clicksShare(clickShareDiagram())
            .clicksShareByAssortment(clicksShareByAssortment())
            .clicksShareDynamicDiagram(new Series<>(ImmutableList.of(
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
            .categoryDynamicDiagram(new Series<>(ImmutableList.of(
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0, 0.2)),
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8, 0.5)))))
            .shopRating(new Series<>(ImmutableList.of(
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(4.5)),
                    Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(4.3)))))
            .build();
    }

    public MarketCpcReportData.CpcSlide1 getCpcSlide1_2() {
        return MarketCpcReportData.CpcSlide1.builder()
                .clicksShare(clickShareDiagram())
                .clicksShareByAssortment(clicksShareByAssortment())
                .clicksShareDynamicDiagram(new Series<>(ImmutableList.of(
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0)),
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8)))))
                .categoryDynamicDiagram(new Series<>(ImmutableList.of(
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(0.6, 1.0, 0.2)),
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(0.7, 0.8, 0.5)))))
                .shopRating(new Series<>(ImmutableList.of(
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-06")), asList(4.5)),
                        Pair.of(Formatters.formatYearMonth(YearMonth.parse("2016-07")), asList(4.3)))))
                .build();
    }

    private MarketCpcReportData.CpcSlide1.DiagramData clicksShareByAssortment() {
        return MarketCpcReportData.CpcSlide1.DiagramData.builder().count(34L).share(18.0D)
            .diagram(new Series<>(ImmutableList.of(
                Pair.of("shop1", asList(5L)),
                Pair.of("shop2", asList(5L)),
                Pair.of("shop3", asList(5L)),
                Pair.of("shop4", asList(5L)),
                Pair.of("shop5", asList(5L)),
                Pair.of("shop6", asList(5L)),
                Pair.of("shop7", asList(5L)),
                Pair.of("shop8", asList(5L)),
                Pair.of("shop9", asList(5L)),
                Pair.of("shop10", asList(15L)),
                Pair.of("Остальные", asList(20L)))))
                .shopPosition(3)
                .build();
    }

    private MarketCpcReportData.CpcSlide1.DiagramData clickShareDiagram() {
        return MarketCpcReportData.CpcSlide1.DiagramData.builder().diagram(new Series<>(ImmutableList.of(
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
                .shopPosition(9)
                .count(1234L)
                .share(15.0D)
                .build();
    }

    private MarketCpcReportData.TopCategoriesSlide getTopCats() {
        return MarketCpcReportData.TopCategoriesSlide.builder()
                .top20Categories(new Series<>(ImmutableList.of(
                        Pair.of("category01", asList(55L)),
                        Pair.of("category02", asList(45L)),
                        Pair.of("category03", asList(35L)),
                        Pair.of("category04", asList(25L)),
                        Pair.of("category05", asList(15L)),
                        Pair.of("category06", asList(5L)),
                        Pair.of("category07", asList(4L)),
                        Pair.of("category08", asList(3L)),
                        Pair.of("category09", asList(2L)),
                        Pair.of("category10", asList(1L))
                )))
                .build();
    }

    private MarketCpcReportData.ClientCategoryDynamics getClientCategoryDynamics() {
        return MarketCpcReportData.ClientCategoryDynamics.builder()
                .categoryDynamicDiagram(new Series<>(ImmutableList.of(
                        Pair.of("2017-01", asList(10.0D, 20.0D, 0.2D)),
                        Pair.of("2017-02", asList(13.0D, 14.0D, 0.2D)),
                        Pair.of("2017-03", asList(11.0D, 18.0D, 0.26D)),
                        Pair.of("2017-04", asList(13.0D, 23.0D, 0.1D))
                )))
                .build();
    }
}
