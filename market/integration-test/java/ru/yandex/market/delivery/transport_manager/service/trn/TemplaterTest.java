package ru.yandex.market.delivery.transport_manager.service.trn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.JxlsTemplate;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnCar;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnCourier;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnInformation;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnLegalInfo;

public class TemplaterTest {
    private final Templater templater = new Templater();

    @Test
    public void returnArrayIsNotNull() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("service/trn/dummy_trn.xlsx");
        var dto = TrnInformation.builder()
                .isExpeditor("X")
                .build();
        Map<String, JxlsTemplate> map = new HashMap<>();
        map.put("courier", dto);
        var res = templater.getXlsTemplate(stream, map);
        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.length > 0);

        File file = File.createTempFile("trn-test", "xlsx");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(res);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            var cellIsExpeditor = sheet.getRow(0).getCell(0).getStringCellValue();
            var cellCar = sheet.getRow(0).getCell(1).getStringCellValue();
            Assertions.assertEquals(dto.getIsExpeditor(), cellIsExpeditor);
            Assertions.assertEquals("", cellCar);
        }
        file.deleteOnExit();
    }

    @Test
    public void testTrn() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("service/trn/test.xlsx");
        TrnInformation dto = TrnInformation.builder()
                .legalInfoSender(TrnLegalInfo.builder()
                        .inn("inn")
                        .legalName("Sender")
                        .legalAddress("Address")
                        .phoneNumber("88005553535")
                        .build())
                .car(TrnCar.builder()
                        .ownershipType(null)
                        .model("Model")
                        .number("O123OO")
                        .trailerNumber("AA123")
                        .build())
                .courier(TrnCourier.builder()
                        .name("Курьер")
                        .surName("Курьеров")
                        .patronymic("Курьерович")
                        .inn("INN")
                        .build())
                .build();
        Map<String, JxlsTemplate> map = new HashMap<>();
        map.put("trn", dto);
        var res = templater.getXlsTemplate(stream, map);
        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.length > 0);

        File file = File.createTempFile("trn-test", "xlsx");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(res);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet firstSheet = workbook.getSheetAt(0);

            var sender = firstSheet.getRow(6).getCell(0).getStringCellValue();
            var cellCourierTrailerNumber = firstSheet.getRow(39).getCell(9).getStringCellValue();
            var ownershipType = firstSheet.getRow(41).getCell(0).getStringCellValue();
            Assertions.assertFalse(sender.isEmpty());
            Assertions.assertFalse(cellCourierTrailerNumber.isEmpty());
            Assertions.assertTrue(ownershipType.isEmpty());
        }
    }

}
