package ru.yandex.market.wms.shared.libs.printer.service;

import java.util.Arrays;

import org.apache.commons.text.TextStringBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.implementation.PrinterDao;
import ru.yandex.market.wms.common.spring.enums.LabelType;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.label.printer.domain.dto.SerialNumberPrinterData;
import ru.yandex.market.wms.shared.libs.label.printer.domain.pojo.PrintResult;
import ru.yandex.market.wms.shared.libs.label.printer.domain.pojo.ZplTemplateBuilder;
import ru.yandex.market.wms.shared.libs.label.printer.exceptions.InvalidPrinterException;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintService;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintServiceImpl;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.SerialNumberPrinter;
import ru.yandex.market.wms.shared.libs.printer.config.PrinterTestConfig;
import ru.yandex.market.wms.trace.Module;
import ru.yandex.market.wms.trace.log.RequestTraceLogBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Import({
        PrinterTestConfig.class,
})
public class SerialNumberPrinterTest extends BaseTest {

    private static final String SERIAL_INVENTORY_TEMPLATE = "" +
        "\u0010CT~~CD,~CC^~CT~\n" +
        "^XA~TA000~JSN^LT0^MNW^PON^PMN^LH32,0^JMA^PR4,4~SD17^JUS^LRN^CI0^XZ\n" +
        "^XA\n" +
        "^MMT\n" +
        "^PW464\n" +
        "^LL0320\n" +
        "^LS0\n" +
        "^BY2,2.5,108^FT12,214^B3N,N,,Y,N\n" +
        "^FD$@CASEID@$^FS\n" +
        "^FT315,89^A0N,56,55^FH\\^FD$@LOT@$^FS\n" +
        "^PQ1,0,1,Y^XZ";

    private static final String SERIAL_INVENTORY_TEMPLATE_EXPECTED = "" +
        "\u0010CT~~CD,~CC^~CT~\n" +
        "^XA~TA000~JSN^LT0^MNW^PON^PMN^LH32,0^JMA^PR4,4~SD17^JUS^LRN^CI0^XZ\n" +
        "^XA\n" +
        "^MMT\n" +
        "^PW464\n" +
        "^LL0320\n" +
        "^LS0\n" +
        "^BY2,2.5,108^FT12,214^B3N,N,,Y,N\n" +
        "^FDserialNumber^FS\n" +
        "^FT315,89^A0N,56,55^FH\\^FD123^FS\n" +
        "^PQ1,0,1,Y^XZ";

    private static final String HEADER_SERIAL_INVENTORY_TEMPLATE_EXPECTED = "" +
            "\u0010CT~~CD,~CC^~CT~\n" +
            "^XA~TA000~JSN^LT0^MNW^PON^PMN^LH32,0^JMA^PR4,4~SD17^JUS^LRN^CI0^XZ\n";

    private static final String LABEL_SERIAL_INVENTORY_TEMPLATE_EXPECTED = "" +
            "^XA\n" +
            "^MMT\n" +
            "^PW464\n" +
            "^LL0320\n" +
            "^LS0\n" +
            "^BY2,2.5,108^FT12,214^B3N,N,,Y,N\n" +
            "^FDserialNumber^FS\n" +
            "^FT315,89^A0N,56,55^FH\\^FD123^FS\n" +
            "^PQ1,0,1,Y^XZ";

    private static final String SERIAL_INVENTORY_TEMPLATE_2 = "" +
            "\u0010CT~~CD,~CC^~CT~" +
            "^XA~TA000~JSN^LT0^MNW^PON^PMN^LH0,0^JMA^PR4,4~SD15^JUS^LRN^CI0^XZ\n" +
            "^XA\n" +
            "^MMT\n" +
            "^PW344\n" +
            "^LL0200\n" +
            "^LS0\n" +
            "^BY2,3,68^FT10,137^B3N,N,,Y,N\n" +
            "^FD$@CASEID@$^FS\n" +
            "^FT228,65^A0N,62,62^FH\\^FD$@LOT@$^FS\n" +
            "^PQ1,0,1,Y^XZ";

    private static final String SERIAL_INVENTORY_TEMPLATE_EXPECTED_2 = "" +
            "\u0010CT~~CD,~CC^~CT~" +
            "^XA~TA000~JSN^LT0^MNW^PON^PMN^LH0,0^JMA^PR4,4~SD15^JUS^LRN^CI0^XZ\n" +
            "^XA\n" +
            "^MMT\n" +
            "^PW344\n" +
            "^LL0200\n" +
            "^LS0\n" +
            "^BY2,3,68^FT10,137^B3N,N,,Y,N\n" +
            "^FDserialNumber^FS\n" +
            "^FT228,65^A0N,62,62^FH\\^FD123^FS\n" +
            "^PQ1,0,1,Y^XZ";

    private static final String PRINTER_NAME = "printerName";
    private static final String PRINTER_NAME2 = "nonDefaultPrinterName";

    private PrintService printService;
    private ZplTemplateBuilder zplTemplateBuilder;
    private SerialNumberPrinter serialNumberPrinter;
    private PrinterDao printerDao;
    private DbConfigService dbConfigService;
    private SecurityDataProvider securityDataProvider;
    private Module applicationModuleName;

    @BeforeEach
    public void setup() {
        super.setup();
        printService = spy(new PrintServiceImpl("host", "123", Module.RECEIVING, new RequestTraceLogBase("test")));
        zplTemplateBuilder = spy(new ZplTemplateBuilder());
        printerDao = mock(PrinterDao.class);
        dbConfigService = mock(DbConfigService.class);
        securityDataProvider = mock(SecurityDataProvider.class);
        applicationModuleName = Module.OLTP;

        serialNumberPrinter = new SerialNumberPrinter(printerDao, printService, zplTemplateBuilder, dbConfigService,
                securityDataProvider, applicationModuleName);

        Mockito.doAnswer(invocation -> new TextStringBuilder(SERIAL_INVENTORY_TEMPLATE)).when(zplTemplateBuilder)
                .getTemplate(eq("SERIAL_RF.zpl"));
        Mockito.doAnswer(invocation -> new TextStringBuilder(SERIAL_INVENTORY_TEMPLATE_2)).when(zplTemplateBuilder)
                .getTemplate(eq("SERIAL_RF2.zpl"));
        Mockito.doReturn(0).when(dbConfigService)
                .getConfigAsInteger(eq("YM_GUID_PRINT_TIME_DELAY"), any());
    }

    @AfterEach
    public void reset() {
        Mockito.reset(printService);
    }

    @Test
    public void printSerialNumberIsOkByHttpCode() {
        Mockito.doReturn(new PrintResult(HttpStatus.OK.toString(), null, null))
            .when(printService)
            .print(eq(SERIAL_INVENTORY_TEMPLATE_EXPECTED), eq(PRINTER_NAME));

        SerialNumberPrinterData printerData = new SerialNumberPrinterData("serialNumber", "lot123123");
        serialNumberPrinter.print(printerData, PRINTER_NAME);

        verify(printService).print(SERIAL_INVENTORY_TEMPLATE_EXPECTED, PRINTER_NAME);
    }

    @Test
    public void printSerialNumberIsOkByIppStatus() {
        Mockito.doReturn(new PrintResult("0x0000", null, null))
            .when(printService)
            .print(eq(SERIAL_INVENTORY_TEMPLATE_EXPECTED), eq(PRINTER_NAME));

        SerialNumberPrinterData printerData = new SerialNumberPrinterData("serialNumber", "lot123123");
        serialNumberPrinter.print(printerData, PRINTER_NAME);

        verify(printService).print(SERIAL_INVENTORY_TEMPLATE_EXPECTED, PRINTER_NAME);
    }

    @Test
    public void printSerialNumberWithEmptyPrinterName() {
        SerialNumberPrinterData printerData = new SerialNumberPrinterData("serialNumber", "lot123123");
        assertThrows(InvalidPrinterException.class,
            () -> serialNumberPrinter.print(printerData, ""), "Printer name are null or empty");

        verify(printService).print(SERIAL_INVENTORY_TEMPLATE_EXPECTED, "");
    }

    @Test
    public void printSerialNumberWithUnexpectedException() {
        Mockito.doReturn(new PrintResult(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "unexpected error",
            new RuntimeException("unexpected"))
        )
            .when(printService)
            .print(eq(SERIAL_INVENTORY_TEMPLATE_EXPECTED), eq(PRINTER_NAME));

        SerialNumberPrinterData printerData = new SerialNumberPrinterData("serialNumber", "lot123123");
        InvalidPrinterException exception = assertThrows(InvalidPrinterException.class,
            () -> serialNumberPrinter.print(printerData, PRINTER_NAME), "unexpected error");

        verify(printService).print(SERIAL_INVENTORY_TEMPLATE_EXPECTED, PRINTER_NAME);
        assertEquals("unexpected", exception.getCause().getMessage());
    }

    @Test
    public void printMultipleSerialNumbers() {
        String template1 = SERIAL_INVENTORY_TEMPLATE_EXPECTED.replaceAll("serialNumber", "serialNumber1");
        String template2 = SERIAL_INVENTORY_TEMPLATE_EXPECTED.replaceAll("serialNumber", "serialNumber2");
        Mockito.doReturn(new PrintResult(HttpStatus.OK.toString(), null, null))
                .when(printService)
                .print(eq(template1), eq(PRINTER_NAME));
        Mockito.doReturn(new PrintResult(HttpStatus.OK.toString(), null, null))
                .when(printService)
                .print(eq(template2), eq(PRINTER_NAME));

        SerialNumberPrinterData printerData1 = new SerialNumberPrinterData("serialNumber1", "lot123123");
        SerialNumberPrinterData printerData2 = new SerialNumberPrinterData("serialNumber2", "lot123123");
        serialNumberPrinter.print(Arrays.asList(printerData1, printerData2), PRINTER_NAME);

        verify(printService).print(template1, PRINTER_NAME);
        verify(printService).print(template2, PRINTER_NAME);

    }

    @Test
    public void batchPrintMultipleSerialNumbers() {
        String template = HEADER_SERIAL_INVENTORY_TEMPLATE_EXPECTED +
                LABEL_SERIAL_INVENTORY_TEMPLATE_EXPECTED.replaceAll("serialNumber", "serialNumber1") +
                LABEL_SERIAL_INVENTORY_TEMPLATE_EXPECTED.replaceAll("serialNumber", "serialNumber2") +
                LABEL_SERIAL_INVENTORY_TEMPLATE_EXPECTED.replaceAll("serialNumber", "serialNumber3");
        Mockito.doReturn(new PrintResult(HttpStatus.OK.toString(), null, null))
                .when(printService)
                .print(eq(template), eq(PRINTER_NAME));
        Mockito.doReturn(true)
                .when(dbConfigService)
                .getConfigAsBoolean("YM_USE_GUID_PRINT_BATCH", false);

        SerialNumberPrinterData printerData1 = new SerialNumberPrinterData("serialNumber1", "lot123123");
        SerialNumberPrinterData printerData2 = new SerialNumberPrinterData("serialNumber2", "lot123123");
        SerialNumberPrinterData printerData3 = new SerialNumberPrinterData("serialNumber3", "lot123123");
        serialNumberPrinter.print(Arrays.asList(printerData1, printerData2, printerData3), PRINTER_NAME);

        verify(printService).print(template, PRINTER_NAME);
    }

    @Test
    public void printWithNotDefaultTemplate() {
        Mockito.doReturn(new PrintResult(HttpStatus.OK.toString(), null, null))
                .when(printService)
                .print(eq(SERIAL_INVENTORY_TEMPLATE_EXPECTED_2), eq(PRINTER_NAME2));

        Mockito.when(printerDao.findLabelTemplateName(eq(PRINTER_NAME2), eq(LabelType.SERIAL_NUMBER)))
                .thenReturn(java.util.Optional.of("SERIAL_RF2"));

        SerialNumberPrinterData printerData = new SerialNumberPrinterData("serialNumber", "lot123123");
        serialNumberPrinter.print(printerData, PRINTER_NAME2);

        verify(printService).print(SERIAL_INVENTORY_TEMPLATE_EXPECTED_2, PRINTER_NAME2);
    }
}
