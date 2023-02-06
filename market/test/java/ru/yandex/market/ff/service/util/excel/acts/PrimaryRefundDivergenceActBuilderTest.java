package ru.yandex.market.ff.service.util.excel.acts;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.ff.client.dto.ActDataRowDTO;
import ru.yandex.market.ff.client.dto.PrimaryDivergenceActDto;
import ru.yandex.market.ff.client.enums.UnredeemedPrimaryDivergenceType;
import ru.yandex.market.ff.exception.http.BadRequestException;
import ru.yandex.market.ff.exception.http.RequestProcessingException;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.service.MarketIdClientService;
import ru.yandex.market.ff.service.implementation.LmsClientCachingServiceImpl;
import ru.yandex.market.ff.service.implementation.MarketIdClientServiceImpl;
import ru.yandex.market.ff.service.util.excel.acts.misc.DataLoader;
import ru.yandex.market.ff.service.util.excel.acts.misc.Utils;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrimaryRefundDivergenceActBuilderTest {

    private static final String TEMPLATE_FILE_NAME = "service/xlsx-report/refund_primary_act.xlsx";

    private static final PrimaryDivergenceActDto PRIMARY_DIVERGENCE_ACT_DTO =
            new PrimaryDivergenceActDto("106", "1123_T",
                    LocalDate.parse("2020-06-18"),
                    LocalDateTime.parse("18.06.2020 10:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    LocalDateTime.parse("19.06.2020 11:18", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    "140126, Московская область, Раменский городской округ, " +
                            "Логистический технопарк Софьино, строение 3/1",
                    Arrays.asList(
                            new ActDataRowDTO("h8==ed", "D0003639315",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 1),
                            new ActDataRowDTO("2563", "D0002352111",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 3),
                            new ActDataRowDTO(null, "D0002325346",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 1)
                    ));

    private final LmsClientCachingService lmsClient = mock(LmsClientCachingServiceImpl.class);
    private final MarketIdServiceGrpc.MarketIdServiceBlockingStub marketIdServiceBlockingStub =
            mock(MarketIdServiceGrpc.MarketIdServiceBlockingStub.class);
    private final MarketIdClientService marketIdClientService =
            new MarketIdClientServiceImpl(marketIdServiceBlockingStub);

    private final PrimaryRefundDivergenceActBuilder service;

    public PrimaryRefundDivergenceActBuilderTest() {
        DataLoader dataLoader = spy(new DataLoader(marketIdClientService, lmsClient));
        service = spy(new PrimaryRefundDivergenceActBuilder());
        ReflectionTestUtils.setField(service, "dataLoader", dataLoader);
    }

    @Test
    @Order(1)
    public void testGetPrimaryDivergenceAct() throws Exception {
        Workbook templateExcel = new XSSFWorkbook(
                Objects.requireNonNull(getSystemResourceAsStream(TEMPLATE_FILE_NAME)));
        when(marketIdServiceBlockingStub.getByPartner(any()))
                .thenReturn(GetByPartnerResponse.newBuilder()
                        .setMarketId(MarketAccount
                                .newBuilder().setLegalInfo(
                                        LegalInfo.newBuilder()
                                                .setLegalName("«Сеть автоматизированных пунктов выдачи»").build())
                                .build())
                        .build());
        service.setPrimaryDivergenceActDto(PRIMARY_DIVERGENCE_ACT_DTO);
        Workbook excel = service.getDivergenceAct();
//        excel.write(new FileOutputStream("out.xlsx"));
        assertFalse(PRIMARY_DIVERGENCE_ACT_DTO.isSendToEmails());
        assertEquals(templateExcel.getNumberOfSheets(), excel.getNumberOfSheets());
        assertEquals(templateExcel.getSheetAt(0).getLastRowNum(), excel.getSheetAt(0).getLastRowNum());
        assertTrue(Utils.excelContentIsEqual(templateExcel, excel, 0));
        assertEquals("«Сеть автоматизированных пунктов выдачи»",
                excel.getSheetAt(0).getRow(20).getCell(1).getStringCellValue());
    }

    @Test
    @Order(2)
    public void testActWithNoLegalNameInMarketIdFound() {
        when(lmsClient.getPartnerName(106L)).thenReturn("Боксберри");
        when(marketIdServiceBlockingStub.getByPartner(any())).thenThrow(new RuntimeException());
        service.setPrimaryDivergenceActDto(PRIMARY_DIVERGENCE_ACT_DTO);

        Workbook excel = service.getDivergenceAct();

        assertEquals(excel.getSheetAt(0).getRow(20).getCell(1).getStringCellValue(),
                "Боксберри");
    }

    @Test
    @Order(3)
    public void testGetActWithNoDataException() {
        assertThrows(BadRequestException.class, service::getDivergenceAct);
    }

    @Test
    @Order(4)
    public void testGetActWithExcelReadingException() {
        ReflectionTestUtils.setField(service, "templateFile", "");
        assertThrows(RequestProcessingException.class, service::getDivergenceAct);
    }

    @Test
    @Order(5)
    public void testGetActWithContentParsingException() {
        doThrow(new RuntimeException()).when(service).createHeaderRows(any(Sheet.class));
        assertThrows(RuntimeException.class, service::getDivergenceAct);
    }

    @Test
    @Order(6)
    public void testGetActFileName() {
        service.setPrimaryDivergenceActDto(PRIMARY_DIVERGENCE_ACT_DTO);
        String generatedActName = service.getGeneratedActName();
        assertEquals(generatedActName,
                "Акт расхождений (Первичная приемка) к Акту клиентский возврат No 1123_T ОТ 2020-06-18.xlsx");
    }

    @Test
    @Order(7)
    public void testGetActWithLegalNameException() {
        when(marketIdClientService.getLegalNameByPartner(anyLong(), anyString())).thenThrow(new RuntimeException());
        when(lmsClient.getPartnerName(106L)).thenThrow(new RuntimeException());
        service.setPrimaryDivergenceActDto(PRIMARY_DIVERGENCE_ACT_DTO);
        assertThrows(RuntimeException.class, service::getDivergenceAct);
    }

    @Test
    @Order(9)
    public void testGetActWithNoDivergenceDataException() {
        PRIMARY_DIVERGENCE_ACT_DTO.setActDataRows(Collections.emptyList());
        assertThrows(RuntimeException.class, service::getDivergenceAct);
    }
}
