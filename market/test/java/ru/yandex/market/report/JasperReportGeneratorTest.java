package ru.yandex.market.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.report.model.ReportType;

/**
 * Тесты для {@link JasperReportGenerator}.
 */
class JasperReportGeneratorTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("testData")
    void testReport(
            ReportType type, Map<String, Class<?>> types, String inputDataFile,
            String expectedTextFile, int pages
    ) throws IOException {
        final List<Map<String, ?>> data = readCsvData(inputDataFile, types);
        final PDDocument document = generatePdfDocument(type, data);
        Assertions.assertEquals(pages, document.getNumberOfPages());

        final String expectedContent = readExpectedContent(expectedTextFile);
        Assertions.assertEquals(expectedContent, new PDFTextStripper().getText(document));
    }


    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        ReportType.FBS_BOX_LABEL,
                        ImmutableMap.builder()
                                .put("supplierName", String.class)
                                .put("serviceName", String.class)
                                .put("orderId", Long.class)
                                .put("orderNum", String.class)
                                .put("fulfilmentId", String.class)
                                .put("place", String.class)
                                .put("deliveryServiceId", String.class)
                                .put("recipientName", String.class)
                                .put("address", String.class)
                                .put("shipmentDate", String.class)
                                .build(),
                        "parcelBoxLabels.csv",
                        "parcelBoxLabels.txt",
                        4
                ),
                Arguments.of(
                        ReportType.FIRST_MILE_SHIPMENT_LIST,
                        ImmutableMap.builder()
                                .put("SHIPMENT_INTERVAL", String.class)
                                .put("SHIPMENT_NUMBER", String.class)
                                .put("ORDER_ID", String.class)
                                .put("ORDER_STATUS", String.class)
                                .put("ORDER_SUB_STATUS", String.class)
                                .put("ORDER_ITEM_NAME", String.class)
                                .put("ORDER_ITEM_SKU", String.class)
                                .put("ORDER_ITEM_BARCODE", String.class)
                                .put("ORDER_ITEM_COUNT", Integer.class)
                                .build(),
                        "firstMileShipmentList.csv",
                        "firstMileShipmentList.txt",
                        1
                )
        );
    }

    private List<Map<String, ?>> readCsvData(String fileName, Map<String, Class<?>> types) throws IOException {
        try (InputStream stream = resourceAsStream(fileName);
             InputStreamReader reader = new InputStreamReader(stream)) {
            final CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            final List<CSVRecord> records = parser.getRecords();
            return records.stream()
                    .map(CSVRecord::toMap)
                    .map(r -> r.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> convertValue(e.getValue(), types.get(e.getKey()))
                    )))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == Long.class) {
            return NumberUtils.createLong(value);
        } else if (type == Integer.class) {
            return NumberUtils.createInteger(value);
        }
        throw new UnsupportedOperationException("Unsupported conversion for type " + type);
    }

    private PDDocument generatePdfDocument(ReportType type, Collection<Map<String, ?>> parameters) throws IOException {
        ByteArrayOutputStream report = JasperReportGenerator.generatePdfReport(type, parameters);
        // XXX(vbauer): Раскомментируйте эту строчку для генерации файла. NB вернуть комментарий
        // org.apache.commons.io.FileUtils.writeByteArrayToFile(new java.io.File(type + ".pdf"), report.toByteArray());
        return PDDocument.load(report.toByteArray());
    }

    private String readExpectedContent(String name) throws IOException {
        try (InputStream stream = resourceAsStream(name)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    private InputStream resourceAsStream(final String name) {
        final InputStream stream = getClass().getResourceAsStream(name);
        return Objects.requireNonNull(stream, () -> "Could not find resource" + name);
    }

}
