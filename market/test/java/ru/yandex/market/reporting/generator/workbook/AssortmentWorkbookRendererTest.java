package ru.yandex.market.reporting.generator.workbook;

import org.junit.Test;
import ru.yandex.market.reporting.common.workbook.ReportData;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.service.Formatters;
import ru.yandex.market.reporting.generator.workbook.Rows.AssortmentRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
public class AssortmentWorkbookRendererTest {

    private AssortmentWorkbookRenderer workbookRenderer = new AssortmentWorkbookRenderer();

    private static ReportData<AssortmentRow, MarketReportParameters> reportData() throws IOException {
        MarketReportParameters parameters = new MarketReportParameters();
        ReportComponents.Assortment assortment = new ReportComponents.Assortment();
        assortment.setGroupByMonth(true);
        parameters.getComponents().setAssortment(assortment);

        try (InputStream hugeDataset = AssortmentWorkbookRendererTest.class.getResourceAsStream("/koleso-russia.ru.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(hugeDataset, Charset.forName("windows-1251")))) {
            List<AssortmentRow> assortmentRows =
                reader.lines().skip(1L).map(line -> line.split("\t"))
                    .filter(arr -> arr.length == 14)
                    .map(strs ->
                        Rows.AssortmentRow.builder()
                            .month(strs[0].equals("Весь период") ? null
                                : YearMonth.parse(strs[0], Formatters.MMMM_YYYY_FORMATTER_RU)
                                .atDay(1))
                            .region(strs[1])
                            .category(strs[2])
                            .hyperId(Long.parseLong(strs[3]))
                            .model(strs[4])
                            .vendor(strs[5])
                            .clientClicks(Long.parseLong(strs[6]))
                            .totalClicks(Long.parseLong(strs[7]))
                            .numShops(Integer.parseInt(strs[8]))
                            .clientInStock(strs[9].equals("Да"))
                            .clientShare(Double.parseDouble(strs[10].replaceAll("%", "")) / 100)
                            .clientPrice(new BigDecimal(strs[11]))
                            .leaderPrice(new BigDecimal(strs[12]))
                            .priceDiffInPercent(Double.parseDouble(strs[13].replaceAll("%", "")) / 100)
                            .build()
                    ).collect(toList());
            return new ReportData<>(parameters, assortmentRows);
        }
    }

    @Test
    public void buildAssortmentReport() throws Exception {
        MarketReportParameters parameters = new MarketReportParameters();
        ReportComponents.Assortment assortment = new ReportComponents.Assortment();
        assortment.setGroupByMonth(true);
        parameters.getComponents().setAssortment(assortment);

        ReportData<AssortmentRow, MarketReportParameters> reportData = ReportData.<AssortmentRow, MarketReportParameters>builder()
            .parameters(parameters)
            .rows(asList(
                Rows.AssortmentRow.builder()
                    .month(LocalDate.of(2016, 11, 1))
                    .region("Москва и Московская область")
                    .category("Планшеты (Компьютерная техника\\Компьютеры)")
                    .hyperId(12625693)
                    .model("Lenovo TAB 2 A10-70L 16Gb")
                    .clientClicks(214L)
                    .totalClicks(8558L)
                    .numShops(198)
                    .clientInStock(true)
                    .clientShare(0.025)
                    .build(),
                Rows.AssortmentRow.builder()
                    .month(null)
                    .region("Москва и Московская область")
                    .category("Планшеты (Компьютерная техника\\Компьютеры)")
                    .hyperId(12625693)
                    .model("Lenovo TAB 2 A10-70L 16Gb")
                    .clientClicks(214L)
                    .totalClicks(8558L)
                    .numShops(198)
                    .clientInStock(false)
                    .clientShare(0.01)
                    .build()))
            .build();

        Path path = Paths.get("assortment.xlsx");
        workbookRenderer.buildReport(reportData, path);
    }

    @Test
    public void buildHugeReport() throws Exception {
        AtomicLong maxMemoryUsageInBytes = new AtomicLong(0L);
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);
        fixedThreadPool.submit(() -> {
            while (Thread.currentThread().isAlive()) {
                MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
                // TODO Does anybody know how to interrupt the test immediately we exceeded the limit?
                maxMemoryUsageInBytes.set(Math.max(maxMemoryUsageInBytes.get(), memoryUsage.getUsed()));
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("assortment-test", "-temp.xslx");
                ReportData<AssortmentRow, MarketReportParameters> assortmentRows = reportData();
                workbookRenderer.buildReport(assortmentRows, tempFile);
            } finally {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            }
        } finally {
            fixedThreadPool.shutdownNow();
        }
        System.err.println("Max memory: " + maxMemoryUsageInBytes.get());
        assertThat(maxMemoryUsageInBytes.get(), lessThan(1_600_000_000L));
    }
}
