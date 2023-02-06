package ru.yandex.market.rg.asyncreport.united.services.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.model.PartnerTypeAwareInfo;
import ru.yandex.market.rg.asyncreport.services.MarketplaceServicesGenerator;
import ru.yandex.market.rg.asyncreport.services.MarketplaceServicesParams;
import ru.yandex.market.rg.asyncreport.services.MarketplaceServicesXlsGenerator;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplacePlacementRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link MarketplaceServicesGenerator}.
 */
class MarketplaceServicesGeneratorTest extends FunctionalTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
    }

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "excelPartnerTest.json",
                        "MarketplaceServicesGeneratorTest.expected.xlsx")
        );
    }

    @MethodSource("args")
    @ParameterizedTest
    public void testXls(String ordersPathToData,
                        String expectedFilePath) throws IOException, InvalidFormatException {
        List<UnitedMarketplacePlacementRow> expected = objectMapper.readValue(
                StringTestUtil.getString(getClass(), ordersPathToData),
                new TypeReference<List<UnitedMarketplacePlacementRow>>() {
                });

        UnitedMarketplaceRowsSupplier unitedMarketplaceRowsSupplier =
                mock(UnitedMarketplaceRowsSupplier.class);
        when(unitedMarketplaceRowsSupplier.getPlacementRows(any(MarketplaceServicesParams.class)))
                .thenReturn(expected);

        PartnerTypeAwareService partnerTypeAwareService =
                mock(PartnerTypeAwareService.class);
        PartnerTypeAwareInfo info = mock(PartnerTypeAwareInfo.class);
        when(partnerTypeAwareService.getPartnerTypeAwareInfo(anyLong()))
                .thenReturn(info);

        MarketplaceServicesXlsGenerator marketplaceServicesXlsGenerator =
                new MarketplaceServicesXlsGenerator(
                        unitedMarketplaceRowsSupplier,
                        partnerTypeAwareService,
                        null,
                        null,
                        null);

        Path tempFilePath = Files.createTempFile("ReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            marketplaceServicesXlsGenerator.generate(
                    new MarketplaceServicesParams(1L, Instant.now(), Instant.now()),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(getClass().getResourceAsStream(expectedFilePath));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new HashSet<>()
        );
    }
}
