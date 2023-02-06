package ru.yandex.market.reporting.generator.presentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.generator.config.SlideShowRendererConfig;
import ru.yandex.market.reporting.generator.domain.BenchmarkingKind;
import ru.yandex.market.reporting.generator.domain.DatePeriod;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.presentation.BenchmarkReportData.CpcRegionCategoryGroup;
import ru.yandex.market.reporting.generator.presentation.MarketReportData.RegionGroup;
import ru.yandex.market.reporting.generator.service.Formatters;
import ru.yandex.market.reporting.generator.service.domain.Series;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;

/**
 * @author nettoyeur
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SlideShowRendererConfig.class)
public class BenchmarkReportSlideShowRendererTest {

    @Inject
    private MarketReportSlideShowRenderer<CpcRegionCategoryGroup> cpcSlideShowRenderer;

    public static BenchmarkReportData.BenchCpc2 emptyBench2() {
        return BenchmarkReportData.BenchCpc2.builder()
            .clicks(new Series<>())
            .expenses(new Series<String, Double>().fillGaps(Arrays.asList("Апр", "Май"), 0.0D, 0.0D))
            .build();
    }

    public static BenchmarkReportData.BenchCpc1 emptyBench1() {
        return BenchmarkReportData.BenchCpc1.builder()
            .clicks(new Series<>())
            .clicksMoustache(new Series<>())

            .expenses(new Series<String, Double>().fillGaps(Arrays.asList("Апр", "Май"), 0.0D, 0.0D))
            .expensesMoustache(new Series<String, Double>().fillGaps(Arrays.asList("Апр", "Май"), 0.0D, 0.0D))
            .clickPrice(new Series<>())
            .clickPriceMoustache(new Series<>())

            .categoryDynamicDiagram(new Series<String, Double>().fillGaps(Arrays.asList("Апр", "Май"), 0.0D, 0.0D))

            .build();
    }

    public static BenchmarkReportData.BenchCpc2 generateBench2() {

        Series<String, Double> clicksPercent = new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomPercentPair(monthMark(month))).collect(Collectors.toList()));

        Series<String, Double> expensesPercent = new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomPercentPair(monthMark(month))).collect(Collectors.toList()));

        return new BenchmarkReportData.BenchCpc2(clicksPercent, expensesPercent);
    }

    public static BenchmarkReportData.BenchCpc1 generateBench1() {
        Series<String, Double> categoryDynamic = generateCategoryDynamics();

        Series<String, Long> clicks = new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomLongPair(monthMark(month), 1L, 20L)).collect(Collectors.toList()));

        Series<String, Double> expenses = new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomDoublePair(monthMark(month), 8_000L, 50_000L)).collect(Collectors.toList()));

        Series<String, Double> clicksCost = new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomDoublePair(monthMark(month), 0L, 50L)).collect(Collectors.toList()));

        return new BenchmarkReportData.BenchCpc1(clicks, moustacheLong(clicks), expenses, moustacheDouble(expenses), clicksCost, moustacheDouble(clicksCost), categoryDynamic);
    }

    private static Series<String, Double> generateCategoryDynamics() {
        return new Series<>(IntStream.rangeClosed(1, 6)
            .mapToObj(YearMonth.of(2017, 1)::withMonth)
            .map(month -> generateRandomDoublePair(monthMark(month), 100.D, 1000.D)).collect(Collectors.toList()));
    }

    private static Series<String, Long> moustacheLong(Series<String, Long> clicks) {
        return new Series<>(clicks.pairStream(1)
            .map(pair -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                boolean hasMoustache = rnd.nextBoolean();
                if (hasMoustache) {
                    Long value = pair.getValue();
                    return Pair.of(pair.getKey(), (List<Long>) ImmutableList
                        .of(value - rnd.nextLong(0, value), value + rnd.nextLong(0, value)).asList());
                } else {
                    return Pair.of(pair.getKey(), (List<Long>) Lists.<Long>newArrayList(null, null));
                }
            })
            .collect(Collectors.toList()));
    }

    private static Series<String, Double> moustacheDouble(Series<String, Double> clicks) {
        return new Series<>(clicks.pairStream(1)
            .map(pair -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                Double value = pair.getValue();
                boolean hasMoustache = rnd.nextBoolean();
                if (hasMoustache) {
                    return Pair.of(pair.getKey(), (List<Double>) ImmutableList
                        .of(value - rnd.nextDouble(0, value), value + rnd.nextDouble(0, value)));
                } else {
                    return Pair.of(pair.getKey(), (List<Double>) Lists.<Double>newArrayList(null, null));
                }
            })
            .collect(Collectors.toList()));
    }

    private static String monthMark(YearMonth month) {
        return Formatters.vipMonthFormat(month);
    }

    public static Pair<String, List<Double>> generateRandomPercentPair(String mark) {
        double clientPercent = ThreadLocalRandom.current().nextDouble();
        return Pair.of(mark, ImmutableList.of(clientPercent, 1 - clientPercent));
    }

    public static Pair<String, List<Long>> generateRandomLongPair(String mark, long low, long up) {
        long client = ThreadLocalRandom.current().nextLong(low, up);
        long others = ThreadLocalRandom.current().nextLong(low, up);
        return Pair.of(mark, ImmutableList.of(client, others));
    }

    public static Pair<String, List<Double>> generateRandomDoublePair(String mark, double low, double up) {
        double client = ThreadLocalRandom.current().nextDouble(low, up);
        double others = ThreadLocalRandom.current().nextDouble(low, up);
        return Pair.of(mark, ImmutableList.of(client, others));
    }

    @Test
    public void englishPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "en");
        BenchmarkReportData reportData = reportData();
        Path path = Paths.get("benchmarking_report-en.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpcSlideShowRenderer);
    }

    @Test
    public void russianPresentationShouldNotHaveAnyPlaceholder() throws Exception {
        ThreadContext.put("language", "ru");
        BenchmarkReportData reportData = reportData();
        Path path = Paths.get("benchmarking_report-ru.pptx");
        AssertUtil.presentationShouldNotHaveAnyPlaceholder(reportData, path, cpcSlideShowRenderer);
    }

    @Test
    public void buildBenchmarkSlides() throws Exception {
        BenchmarkReportData reportData = reportData();

        Path path = Paths.get("benchmarking_report.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }

    private BenchmarkReportData reportData() {
        List<String> regions = singletonList("Москва и Московская область");
        List<String> categories = singletonList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();
        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .benchCpc1(generateBench1())
                    .benchCpc2(generateBench2())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("M-VIDEO");
        params.setDomain("mvideo.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2017, 2), YearMonth.of(2017, 4));

        params.getComponents().setBenchmarking(benchmarking(period));

        return BenchmarkReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();
    }

    private ReportComponents.Benchmarking benchmarking(DatePeriod period) {
        ReportComponents.Benchmarking benchmarking = new ReportComponents.Benchmarking();
        benchmarking.setBenchmarkingKind(BenchmarkingKind.SPECIFIC);
        benchmarking.setPeriod(period);
        benchmarking.setCompetitorsDomains(ImmutableList.of("ulmart.ru", "citilink.ru"));
        return benchmarking;
    }

    @Test
    public void emptyData() throws Exception {
        List<String> regions = singletonList("Москва и Московская область");
        List<String> categories = singletonList("Мобильные телефоны");

        List<RegionGroup<CpcRegionCategoryGroup>> groups = new ArrayList<>();

        groups.add(
            new RegionGroup<>(
                1,
                regions.get(0),
                ImmutableList.of(CpcRegionCategoryGroup.builder()
                    .region(regions.get(0))
                    .category(categories.get(0))
                    .categoryPath("Электроника\\Телефоны\\Мобильные телефоны")
                    .benchCpc1(emptyBench1())
                    .benchCpc2(emptyBench2())
                    .build())));

        MarketReportParameters params = new MarketReportParameters();
        params.setShop("M-VIDEO");
        params.setDomain("mvideo.ru");
        DatePeriod period = new DatePeriod(YearMonth.of(2017, 2), YearMonth.of(2017, 4));

        params.getComponents().setBenchmarking(benchmarking(period));

        BenchmarkReportData reportData = BenchmarkReportData.builder()
            .parameters(params)
            .regions(regions)
            .categories(categories)
            .regionGroups(groups)
            .build();

        Path path = Paths.get("benchmark_empty_data.pptx");
        cpcSlideShowRenderer.buildReport(reportData, path);
    }
}
