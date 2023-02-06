package ru.yandex.market.ff.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.dto.ArrivedRequestsListRowDTO;
import ru.yandex.market.ff.service.implementation.ArrivedRequestsListGenerationServiceImpl;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class ArrivedRequestsListGenerationServiceTest {

    private ArrivedRequestsListGenerationService arrivedRequestsListGenerationService;

    private final SoftAssertions assertions = new SoftAssertions();

    @BeforeEach
    public void before() {
        arrivedRequestsListGenerationService = new ArrivedRequestsListGenerationServiceImpl();
    }

    @AfterEach
    public void triggerSoftAssertions() {
        assertions.assertAll();
    }

    @Test
    public void generateListFor1pTest() throws IOException {

        List<ArrivedRequestsListRowDTO> arrivedRequestsListRowDTOS = List.of(ArrivedRequestsListRowDTO.builder()
                        .externalRequestId("Зп-370178280")
                        .serviceRequestId("0025378618")
                        .requestId(11586652)
                        .supplierId(1005526)
                        .realSupplierId("000099")
                        .realSupplierName("ООО \"НТС \"Градиент\"")
                        .comment("ЭДО/EDI,  Принять по электронному УПД, Зп-370178280, ООО \"СВИТМИЛК\"")
                        .arrivedDateTime(LocalDateTime.of(2022, 3, 1, 1, 0))
                        .build(),
                ArrivedRequestsListRowDTO.builder()
                        .externalRequestId("Зп-370180564")
                        .serviceRequestId("0025956547")
                        .requestId(11627631)
                        .supplierId(465852)
                        .realSupplierId("000099")
                        .realSupplierName("ООО СиКеэр Евразия")
                        .comment("ЭДО/EDI,  Принять по электронному УПД, Зп-370180564, ООО СиКеэр Евразия")
                        .arrivedDateTime(LocalDateTime.of(2022, 3, 1, 1, 0))
                        .build()
        );

        assertPdf(arrivedRequestsListGenerationService.generateListFor1p(
                arrivedRequestsListRowDTOS,
                LocalDate.of(2022, 3, 12),
                "Софьино"
        ), "arrived_requests_list_1p.txt");

    }

    @Test
    public void generateListFor3pTest() throws IOException {

        List<ArrivedRequestsListRowDTO> arrivedRequestsListRowDTOS = List.of(ArrivedRequestsListRowDTO.builder()
                        .externalRequestId("Зп-370178280")
                        .serviceRequestId("0025378618")
                        .requestId(11586652)
                        .supplierId(1005526)
                        .realSupplierId("000099")
                        .realSupplierName("ООО \"НТС \"Градиент\"")
                        .comment("ЭДО/EDI,  Принять по электронному УПД, Зп-370178280, ООО \"СВИТМИЛК\"")
                        .arrivedDateTime(LocalDateTime.of(2022, 3, 1, 1, 0))
                        .build(),
                ArrivedRequestsListRowDTO.builder()
                        .externalRequestId("Зп-370180564")
                        .serviceRequestId("0025956547")
                        .requestId(11627631)
                        .supplierId(465852)
                        .realSupplierId("000099")
                        .realSupplierName("ООО СиКеэр Евразия")
                        .comment("ЭДО/EDI,  Принять по электронному УПД, Зп-370180564, ООО СиКеэр Евразия")
                        .arrivedDateTime(LocalDateTime.of(2022, 3, 1, 1, 0))
                        .build()
        );

        assertPdf(arrivedRequestsListGenerationService.generateListFor3p(
                arrivedRequestsListRowDTOS,
                LocalDate.of(2022, 3, 12),
                "Софьино"
        ), "arrived_requests_list_3p.txt");

    }

    private void assertPdf(final InputStream is, String fileName) throws IOException {
        final PDFParser parser = new PDFParser(new RandomAccessBuffer(is));
        parser.parse();

        final PDDocument document = parser.getPDDocument();

        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);

        final InputStream expectedIS = getSystemResourceAsStream("service/pdf-report/" + fileName);
        final String expectedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);

        System.out.println(text);
        assertions.assertThat(text).isEqualToIgnoringWhitespace(expectedText);
    }

}
