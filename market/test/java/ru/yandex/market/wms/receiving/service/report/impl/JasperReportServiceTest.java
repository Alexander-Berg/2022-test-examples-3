package ru.yandex.market.wms.receiving.service.report.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.dto.report.BaseDiscrepanciesReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerBoxReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerItemReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerPalletReportData;
import ru.yandex.market.wms.receiving.model.dto.report.ReportFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class JasperReportServiceTest extends ReceivingIntegrationTest {

    @Autowired
    private JasperReportService jasperReportService;
    @Autowired
    private Clock clock;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Test
    public void createPdfReportCheckFile() throws IOException {
        //given
        var reportData = discrepanciesReportDto();

        //when
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.PDF);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        File file = new File("report.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(result);
        fileOutputStream.close();

        PDDocument document = PDDocument.load(result);

        //then
        assertEquals(document.getNumberOfPages(), 6, "doc has 5 pages");
        checkPageImages(document, List.of(2916470435L, 4273535782L, 3753966781L, 1686527291L, 1632683818L,
                3118382359L));
        file.delete();
    }

    @Test
    public void createPdfReportPerPalletCheckFile() throws IOException {
        //given
        var reportData = discrepanciesPerPalletReportDto();

        //when
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.PDF);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        File file = new File("report1.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(result);
        fileOutputStream.close();

        PDDocument document = PDDocument.load(result);

        //then
        assertEquals(document.getNumberOfPages(), 6, "doc has 5 pages");
        checkPageImages(document, List.of(1511039092L, 2395016240L, 3868513208L, 1666731423L, 206422307L,
                75859875L));
        file.delete();
    }


    @Test
    public void createPdfReportPerItemCheckFile() throws IOException {
        //given
        var reportData = discrepanciesPerItemReportDto();

        //when
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.PDF);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        File file = new File("report1.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(result);
        fileOutputStream.close();

        PDDocument document = PDDocument.load(result);

        //then
        assertEquals(2, document.getNumberOfPages(), "doc has 1 page");
        checkPageImages(document, List.of(2497970136L, 3040486676L));
        file.delete();
    }

    @Test
    public void createHtmlReport() throws IOException {
        //given
        var reportData = discrepanciesReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.HTML);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    @Test
    public void createHtmlPerPalletReport() throws IOException {
        //given
        var reportData = discrepanciesPerPalletReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.HTML);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    @Test
    public void createHtmlPerItemReport() throws IOException {
        //given
        var reportData = discrepanciesPerItemReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.HTML);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    @Test
    public void createXlsReport() throws IOException {
        var reportData = discrepanciesReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.XLS);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    @Test
    public void createXlsPerPalletReport() throws IOException {
        var reportData = discrepanciesPerPalletReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.XLS);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    @Test
    public void createXlsPerItemReport() throws IOException {
        var reportData = discrepanciesPerItemReportDto();
        ByteArrayOutputStream byteArrayOutputStream = jasperReportService.generateReport(reportData, ReportFormat.XLS);
        OutputStream outputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        //then
        assertTrue(result.length > 1000, "File is generated");
    }

    private BaseDiscrepanciesReportData discrepanciesReportDto() {
        List<BaseDiscrepanciesReportData.Discrepancy> discrepancies = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            discrepancies.add(discrepancy(i));
        }
        List<BaseDiscrepanciesReportData.Summary> summaries = new ArrayList<>();
        summaries.add(summary("Итого: не принято (некорректный склад возврата)", 1));
        summaries.add(summary("Итого: не принято (статус заказа «50 Доставлен»)", 2));
        summaries.add(summary("Итого: не принято (превышен срок возврата заказа)", 3));
        summaries.add(summary("Итого: недостача (заявлен в реестре, но отсутствует)", 4));
        summaries.add(summary("Итого: дропшип (не обрабатывается на ФФЦ Яндекс.Маркета)", 5));

        DiscrepanciesPerBoxReportData reportData = new DiscrepanciesPerBoxReportData();
        reportData.setOrderDate(LocalDate.now(clock));
        reportData.setReceivingDate(LocalDateTime.now(clock).minusDays(1));
        reportData.setCarrierName("Алиса Продакшн");
        reportData.setAgentName("СДЭК-Мск");
        reportData.setActNumber("143");
        reportData.setSupplyNumber("012376");
        reportData.setRegistryNumber("reg000555000");
        reportData.setShipFromAddress("Послано с адреса");
        reportData.setWarehouseAddress("140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1 ))" +
                "140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1");
        reportData.setDiscrepancyList(discrepancies);
        reportData.setDiscrepancySummaries(summaries);
        reportData.setQualityAttributes(qualityAttributes(20));
        reportData.setPrintingDate(LocalDateTime.now(clock));
        return reportData;
    }

    private BaseDiscrepanciesReportData discrepanciesPerPalletReportDto() {
        List<BaseDiscrepanciesReportData.Discrepancy> discrepancies = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            discrepancies.add(discrepancy(i));
        }
        List<BaseDiscrepanciesReportData.Summary> summaries = new ArrayList<>();
        summaries.add(summary("Итого:", 100));
        DiscrepanciesPerPalletReportData reportData = new DiscrepanciesPerPalletReportData();
        reportData.setOrderDate(LocalDate.now(clock));
        reportData.setReceivingDate(LocalDateTime.now(clock).minusDays(1));
        reportData.setCarrierName("Алиса Продакшн");
        reportData.setAgentName("СДЭК-Мск");
        reportData.setActNumber("143");
        reportData.setSupplyNumber("012376");
        reportData.setRegistryNumber("reg000555000");
        reportData.setShipFromAddress("Послано с адреса");
        reportData.setUserLogin("@user_login");
        reportData.setUserName("Вася Петров");
        reportData.setWarehouseAddress("140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1 ))" +
                "140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1");
        reportData.setDiscrepancyList(discrepancies);
        reportData.setDiscrepancySummaries(summaries);
        reportData.setQualityAttributes(qualityAttributes(20));
        reportData.setQualityAttributeSummary(List.of(new BaseDiscrepanciesReportData.Summary("Итого:", 20)));
        reportData.setPrintingDate(LocalDateTime.now(clock));
        return reportData;
    }

    private BaseDiscrepanciesReportData discrepanciesPerItemReportDto() {
        DiscrepanciesPerItemReportData reportData = new DiscrepanciesPerItemReportData();
        reportData.setOrderDate(LocalDate.now(clock));
        reportData.setReceivingDate(LocalDateTime.now(clock).minusDays(1));
        reportData.setCarrierName("Алиса Продакшн");
        reportData.setAgentName("СДЭК-Мск");
        reportData.setActNumber("143");
        reportData.setSupplyNumber("012376");
        reportData.setSupplyNumber("176523672");
        reportData.setRegistryNumber("reg000555000");
        reportData.setShipFromAddress("Послано с адреса");
        reportData.setWarehouseAddress("140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1 ))" +
                "140126, Московская область, Раменский городской округ, " +
                "Логистический технопарк Софьино, строение 3/1");
        reportData.setItemsWithAttributes(itemsWithAttributes(20));
        reportData.setPrintingDate(LocalDateTime.now(clock));
        return reportData;
    }

    private List<BaseDiscrepanciesReportData.QualityAttribute> qualityAttributes(int n) {
        String[] attrs = new String[] {"Осторожно, окрашено", "Не кантовать", "Помято"};
        List<BaseDiscrepanciesReportData.QualityAttribute> attributes = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            BaseDiscrepanciesReportData.QualityAttribute attribute = BaseDiscrepanciesReportData.QualityAttribute
                    .builder()
                    .number(i)
                    .dispatchNumber(String.format("%07d", i))
                    .attributes(attrs[i % 3])
                    .build();
            attributes.add(attribute);
        }
        return attributes;
    }

    private List<BaseDiscrepanciesReportData.ItemWithAttributes> itemsWithAttributes(int n) {
        String[] attrs = new String[] {"Недостача", "Неопознанный", "Поврежден"};
        List<BaseDiscrepanciesReportData.ItemWithAttributes> attributes = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            BaseDiscrepanciesReportData.ItemWithAttributes attribute = BaseDiscrepanciesReportData.ItemWithAttributes
                    .builder()
                    .number(i)
                    .dispatchNumber("0123243")
                    .registryNumber("12387632")
                    .sku("GARNIER Fructis шампунь Огуречная свежесть Укрепляющий с витаминами и экстрактом " +
                            "Огурца для склонных к жирности волос, 250 мл")
                    .count(i % 5)
                    .attributes(attrs[i % 3] + ", " + attrs[i % 2])
                    .build();
            attributes.add(attribute);
        }
        return attributes;
    }

    private BaseDiscrepanciesReportData.Discrepancy discrepancy(int i) {
        return BaseDiscrepanciesReportData.Discrepancy.builder()
                .number(i)
                .externalNumber("ext12345" + i)
                .dispatchNumber("sending8" + i)
                .discrepancyType("Недостача" + i)
                .value(5 + i)
                .build();
    }

    private BaseDiscrepanciesReportData.Summary summary(String name, int value) {
        return new BaseDiscrepanciesReportData.Summary(name, value);
    }

    private static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    private void checkPageImages(PDDocument document, List<Long> pagesCheckSum) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        var actualChecksums = new ArrayList<Long>();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage bim = pdfRenderer.renderImage(i);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "png", baos);
            long crc32Checksum = getCRC32Checksum(baos.toByteArray());
            actualChecksums.add(crc32Checksum);
        }
        assertThat(actualChecksums).containsAll(pagesCheckSum);
        document.close();
    }
}
