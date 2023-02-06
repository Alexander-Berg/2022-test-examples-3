package ru.yandex.market.rg.asyncreport.united.orders.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.united.orders.UnitedOrdersParams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест на {@link UnitedOrdersXlsReportGenerator}
 */
public class UnitedOrdersXlsReportGeneratorTest {

    private ObjectMapper objectMapper;
    private UnitedReportsInformationService unitedReportInformationService;
    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        unitedReportInformationService = mock(UnitedReportsInformationService.class);
    }

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "UnitedOrdersXlsReportGeneratorTest.orders.before.json",
                        "UnitedOrdersXlsReportGeneratorTest.services.before.json",
                        "UnitedOrdersXlsReportGeneratorTest.expected.xlsx",
                        new UnitedOrdersParams(1L,
                                LocalDate.of(2021, 8, 5).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant(),
                                LocalDate.of(2021, 8, 5)
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant())
                ),
                Arguments.of(
                        "UnitedOrdersXlsReportGeneratorTest.singleOrder.before.json",
                        "UnitedOrdersXlsReportGeneratorTest.services.single.order.before.json",
                        "UnitedOrdersXlsReportGeneratorTest.expected.single.order.xlsx",
                        new UnitedOrdersParams(1L,5679434L)
                )
        );
    }

    @MethodSource("args")
    @ParameterizedTest
    public void testXls(String ordersPathToData,
                        String servicesPathToData,
                        String expectedFilePath,
                        UnitedOrdersParams params) throws IOException, InvalidFormatException {
        List<UnitedOrderItemRow> expectedOrders = objectMapper.readValue(
                StringTestUtil.getString(getClass(), ordersPathToData),
                new TypeReference<List<UnitedOrderItemRow>>() {
                });
        List<UnitedOrderServiceAndMarginRow> expectedServices = objectMapper.readValue(
                StringTestUtil.getString(getClass(), servicesPathToData),
                new TypeReference<List<UnitedOrderServiceAndMarginRow>>() {
                });

        when(unitedReportInformationService.getInformationByBusiness(anyLong()))
                .thenReturn(Stream.concat(
                                expectedOrders.stream().map(UnitedOrderItemRow::getHeader),
                                expectedServices.stream().map(UnitedOrderServiceAndMarginRow::getHeader))
                        .distinct()
                        .collect(Collectors.toList()));
        UnitedOrdersXlsReportGenerator unitedOrdersXlsReportGenerator =
                new UnitedOrdersXlsReportGenerator(null, null, null,
                        null, null, null, null,
                        null, null, null, null,
                        null, null, unitedReportInformationService,
                        null, environmentService
                ) {
                    @Override
                    protected UnitedOrdersRowsSupplier buildNewSupplier() {
                        UnitedOrdersRowsSupplier rowsSupplier = mock(UnitedOrdersRowsSupplier.class);
                        when(rowsSupplier.getOrderItems(any(UnitedOrdersParams.class)))
                                .thenReturn(expectedOrders.stream());
                        when(rowsSupplier.getOrderServiceAndMarginRow(any(UnitedOrdersParams.class)))
                                .thenReturn(expectedServices.stream());
                        return rowsSupplier;
                    }
                };

        Path tempFilePath = Files.createTempFile("UnitedOrdersXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedOrdersXlsReportGenerator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(getClass().getResourceAsStream(expectedFilePath));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }
}
